package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Aluno;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Nota;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.repository.NotaRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.TurmaRepository;

import java.util.List;

/**
 * A nota é sempre derivada dos simulados concluídos (regra de cálculo em
 * Nota). Toda alteração é registrada em AuditLog.
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

    public List<Nota> listar() { return repository.findAll(); }

    public List<Nota> historicoPorAluno(Long alunoId) {
        return repository.findByAluno_IdOrderByCreatedAtDesc(alunoId);
    }

    /** Chamado a cada simulado finalizado e, manualmente, para reprocessamento. */
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

        return salva;
    }

    /**
     * Sem matrícula encontrada, conta o simulado: um dado de matrícula
     * inconsistente não deve zerar a nota do aluno.
     */
    private boolean elegivelPelaMatricula(SimuladoAluno simuladoAluno, AlunoTurma matricula) {
        if (matricula == null || matricula.getDataInicio() == null) return true;
        var dataAplicacao = simuladoAluno.getSimulado().getDataInicio();
        if (dataAplicacao == null) return true;
        return !dataAplicacao.toLocalDate().isBefore(matricula.getDataInicio());
    }
}
