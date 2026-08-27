package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Aluno;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Nota;
import studojurata_api.model.ResponsavelAluno;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.TipoNotificacao;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.repository.NotaRepository;
import studojurata_api.repository.ResponsavelAlunoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.TurmaRepository;

import java.util.List;

/**
 * Correção 1.2 + 2.13 da Segunda Análise Crítica: Nota deixou de ser um
 * valor solto editável por PUT e passou a ser sempre derivada dos simulados
 * concluídos do aluno naquela disciplina — nunca persistida diretamente a
 * partir do que o cliente da API mandar em "total".
 *
 * Correção "matrícula cíclica" (revisão pedagógica): o escopo do cálculo
 * deixou de ser periodoLetivo (calendário fixo) e passou a ser a turma —
 * cada disciplina distribui 100 pontos entre os simulados com notaMaxima
 * maior que zero (notaMaxima = 0 é só simulado de reforço/repetição
 * espaçada, não entra na soma), e só simulados aplicados a partir da data em
 * que o aluno se matriculou naquela turma contam — quem entrou depois do
 * ciclo começar não é penalizado por simulados anteriores à sua matrícula.
 *
 * Toda alteração de Nota é registrada em AuditLog (item 2.9/10.4).
 */
@Service
@RequiredArgsConstructor
public class NotaService {

    private final NotaRepository repository;
    private final AlunoRepository alunoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final TurmaRepository turmaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final SimuladoAlunoRepository simuladoAlunoRepository;
    private final AuditLogService auditLogService;
    private final ResponsavelAlunoRepository responsavelAlunoRepository;
    private final NotificacaoService notificacaoService;

    public List<Nota> listar() { return repository.findAll(); }

    public Nota buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nota " + id + " não encontrada."));
    }

    /** Histórico completo do aluno, do registro mais recente ao mais antigo — acessível ao próprio aluno. */
    public List<Nota> historicoPorAluno(Long alunoId) {
        return repository.findByAluno_IdOrderByCreatedAtDesc(alunoId);
    }

    public List<Nota> historicoPorAlunoEDisciplina(Long alunoId, Long disciplinaId) {
        return repository.findByAluno_IdAndDisciplina_IdOrderByCreatedAtDesc(alunoId, disciplinaId);
    }

    /**
     * Recalcula (cria ou atualiza) a Nota de um aluno numa disciplina/turma,
     * como a SOMA dos SimuladoAluno.nota já CONCLUIDOs daquela
     * disciplina/turma, com notaMaxima > 0 e aplicados a partir da data de
     * matrícula do aluno na turma. Chamado automaticamente sempre que um
     * simulado é finalizado (ver SimuladoAlunoService.finalizar), e pode
     * também ser chamado manualmente (ex.: reprocessamento administrativo).
     */
    @Transactional
    public Nota recalcular(Long alunoId, Long disciplinaId, Long turmaId) {
        AlunoTurma matricula = alunoTurmaRepository
                .findFirstByAluno_IdAndTurma_IdOrderByDataInicioDesc(alunoId, turmaId)
                .orElse(null);

        List<SimuladoAluno> concluidos = simuladoAlunoRepository
                .findByAluno_IdAndStatusAndSimulado_Disciplina_IdAndSimulado_Turma_Id(
                        alunoId, StatusSimuladoAluno.CONCLUIDO, disciplinaId, turmaId)
                .stream()
                .filter(sa -> sa.getSimulado().getNotaMaxima() != null && sa.getSimulado().getNotaMaxima() > 0)
                .filter(sa -> elegivelPelaMatricula(sa, matricula))
                .toList();

        double total = concluidos.stream()
                .filter(sa -> sa.getNota() != null)
                .mapToDouble(SimuladoAluno::getNota)
                .sum();

        Nota nota = repository.findByAluno_IdAndDisciplina_IdAndTurma_Id(alunoId, disciplinaId, turmaId)
                .orElseGet(() -> {
                    Nota nova = new Nota();
                    Aluno aluno = alunoRepository.findById(alunoId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
                    Disciplina disciplina = disciplinaRepository.findById(disciplinaId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina não encontrada."));
                    Turma turma = turmaRepository.findById(turmaId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
                    nova.setAluno(aluno);
                    nova.setDisciplina(disciplina);
                    nova.setTurma(turma);
                    return nova;
                });

        Double totalAnterior = nota.getTotal();
        nota.setTotal(concluidos.isEmpty() ? null : total);
        nota.setQuantidadeSimuladosConsiderados(concluidos.size());
        Nota salva = repository.save(nota);

        auditLogService.registrar("Nota", salva.getId(),
                totalAnterior == null ? AcaoAuditoria.CRIACAO : AcaoAuditoria.ATUALIZACAO,
                "total: " + totalAnterior + " -> " + salva.getTotal()
                        + " (turma " + turmaId + ", " + concluidos.size() + " simulado(s) concluído(s))");

        // Item 9.8: notifica (registro em banco, opt-in) os responsáveis do aluno que marcaram receberNotificacoes.
        List<ResponsavelAluno> destinatarios = responsavelAlunoRepository.findByAlunoIdAndReceberNotificacoesTrue(alunoId);
        for (ResponsavelAluno destinatario : destinatarios) {
            notificacaoService.registrar(destinatario, TipoNotificacao.NOVA_NOTA,
                    "Nova nota registrada em " + salva.getDisciplina().getTitulo() + ".");
        }

        return salva;
    }

    /**
     * Sem matrícula encontrada, mantém o comportamento permissivo anterior
     * (conta o simulado) em vez de zerar a nota do aluno por um dado de
     * matrícula ausente/inconsistente. Com matrícula, só conta simulados
     * aplicados a partir da data em que o aluno entrou na turma.
     */
    private boolean elegivelPelaMatricula(SimuladoAluno simuladoAluno, AlunoTurma matricula) {
        if (matricula == null || matricula.getDataInicio() == null) return true;
        var dataAplicacao = simuladoAluno.getSimulado().getDataInicio();
        if (dataAplicacao == null) return true;
        return !dataAplicacao.toLocalDate().isBefore(matricula.getDataInicio());
    }

    /** Exclusão administrativa pontual (registro puramente derivado, não há "histórico pedagógico" a preservar em si). */
    @Transactional
    public void deletar(Long id) {
        auditLogService.registrar("Nota", id, AcaoAuditoria.EXCLUSAO, "Registro de nota removido manualmente.");
        repository.deleteById(id);
    }
}
