package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Aluno;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Nota;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.repository.NotaRepository;
import studojurata_api.repository.SimuladoAlunoRepository;

import java.util.List;

/**
 * Correção 1.2 + 2.13 da Segunda Análise Crítica: Nota deixou de ser um
 * valor solto editável por PUT e passou a ser sempre derivada dos simulados
 * concluídos do aluno naquela disciplina/período letivo — nunca persistida
 * diretamente a partir do que o cliente da API mandar em "total".
 *
 * Toda alteração de Nota é registrada em AuditLog (item 2.9/10.4).
 */
@Service
@RequiredArgsConstructor
public class NotaService {

    private final NotaRepository repository;
    private final AlunoRepository alunoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final SimuladoAlunoRepository simuladoAlunoRepository;
    private final AuditLogService auditLogService;

    public List<Nota> listar() { return repository.findAll(); }

    public Nota buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nota " + id + " não encontrada."));
    }

    /** Histórico completo do aluno, do período letivo mais recente ao mais antigo — acessível ao próprio aluno. */
    public List<Nota> historicoPorAluno(Long alunoId) {
        return repository.findByAluno_IdOrderByPeriodoLetivoDesc(alunoId);
    }

    public List<Nota> historicoPorAlunoEDisciplina(Long alunoId, Long disciplinaId) {
        return repository.findByAluno_IdAndDisciplina_IdOrderByPeriodoLetivoDesc(alunoId, disciplinaId);
    }

    /**
     * Recalcula (cria ou atualiza) a Nota de um aluno numa disciplina/período
     * letivo, como a média dos SimuladoAluno.nota já CONCLUIDOs daquela
     * disciplina/período. Chamado automaticamente sempre que um simulado é
     * finalizado (ver SimuladoAlunoService.finalizar), e pode também ser
     * chamado manualmente (ex.: reprocessamento administrativo).
     */
    @Transactional
    public Nota recalcular(Long alunoId, Long disciplinaId, String periodoLetivo) {
        List<SimuladoAluno> concluidos = simuladoAlunoRepository
                .findByAluno_IdAndStatusAndSimulado_Disciplina_IdAndSimulado_PlanoEnsino_PeriodoLetivo(
                        alunoId, StatusSimuladoAluno.CONCLUIDO, disciplinaId, periodoLetivo);

        double media = concluidos.stream()
                .filter(sa -> sa.getNota() != null)
                .mapToDouble(SimuladoAluno::getNota)
                .average()
                .orElse(0d);

        Nota nota = repository.findByAluno_IdAndDisciplina_IdAndPeriodoLetivo(alunoId, disciplinaId, periodoLetivo)
                .orElseGet(() -> {
                    Nota nova = new Nota();
                    Aluno aluno = alunoRepository.findById(alunoId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
                    Disciplina disciplina = disciplinaRepository.findById(disciplinaId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina não encontrada."));
                    nova.setAluno(aluno);
                    nova.setDisciplina(disciplina);
                    nova.setPeriodoLetivo(periodoLetivo);
                    return nova;
                });

        Double totalAnterior = nota.getTotal();
        nota.setTotal(concluidos.isEmpty() ? null : media);
        nota.setQuantidadeSimuladosConsiderados(concluidos.size());
        Nota salva = repository.save(nota);

        auditLogService.registrar("Nota", salva.getId(),
                totalAnterior == null ? AcaoAuditoria.CRIACAO : AcaoAuditoria.ATUALIZACAO,
                "total: " + totalAnterior + " -> " + salva.getTotal()
                        + " (período " + periodoLetivo + ", " + concluidos.size() + " simulado(s) concluído(s))");

        return salva;
    }

    /** Exclusão administrativa pontual (registro puramente derivado, não há "histórico pedagógico" a preservar em si). */
    @Transactional
    public void deletar(Long id) {
        auditLogService.registrar("Nota", id, AcaoAuditoria.EXCLUSAO, "Registro de nota removido manualmente.");
        repository.deleteById(id);
    }
}
