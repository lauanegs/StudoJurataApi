package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.model.enums.StatusTurma;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.TurmaRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlunoTurmaService {

    private final AlunoTurmaRepository repository;
    private final TurmaRepository turmaRepository;

    public List<AlunoTurma> listar() { return repository.findAll(); }

    public AlunoTurma buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula " + id + " não encontrada."));
    }

    public List<AlunoTurma> historicoPorTurma(Long turmaId) {
        return repository.findByTurmaIdOrderByDataInicioDesc(turmaId);
    }

    public List<AlunoTurma> ativosPorTurma(Long turmaId) {
        return repository.findByTurmaIdAndStatus(turmaId, StatusMatricula.ATIVA);
    }

    public long contarAtivosPorTurma(Long turmaId) {
        return repository.countByTurmaIdAndStatus(turmaId, StatusMatricula.ATIVA);
    }

    /**
     * Cria uma nova matrícula, aplicando as regras de negócio:
     * - não permite duas matrículas ATIVAS para o mesmo par (aluno, turma);
     * - não permite matricular além da capacidadeMaxima da turma;
     * - status default é ATIVA quando não informado.
     */
    @Transactional
    public AlunoTurma matricular(AlunoTurma obj) {
        if (obj.getAluno() == null || obj.getAluno().getId() == null) {
            throw new RequisicaoInvalidaException("Aluno é obrigatório para a matrícula.");
        }
        if (obj.getTurma() == null || obj.getTurma().getId() == null) {
            throw new RequisicaoInvalidaException("Turma é obrigatória para a matrícula.");
        }
        if (obj.getStatus() == null) {
            obj.setStatus(StatusMatricula.ATIVA);
        }
        if (obj.getDataInicio() == null) {
            obj.setDataInicio(LocalDate.now());
        }

        if (obj.getStatus() == StatusMatricula.ATIVA) {
            validarTurmaAtiva(obj.getTurma().getId());
            validarMatriculaAtivaUnica(obj.getAluno().getId(), obj.getTurma().getId(), null);
            validarCapacidade(obj.getTurma().getId());
        }

        return repository.save(obj);
    }

    @Transactional
    public AlunoTurma atualizar(Long id, AlunoTurma obj) {
        AlunoTurma existente = buscar(id);
        obj.setId(id);

        if (obj.getStatus() == StatusMatricula.ATIVA) {
            Long alunoId = obj.getAluno() != null ? obj.getAluno().getId() : existente.getAluno().getId();
            Long turmaId = obj.getTurma() != null ? obj.getTurma().getId() : existente.getTurma().getId();
            validarTurmaAtiva(turmaId);
            validarMatriculaAtivaUnica(alunoId, turmaId, id);
            // só valida capacidade se a matrícula não já estava ativa nessa mesma turma
            boolean jaEstavaAtivaNaMesmaTurma = existente.getStatus() == StatusMatricula.ATIVA
                    && existente.getTurma().getId().equals(turmaId);
            if (!jaEstavaAtivaNaMesmaTurma) {
                validarCapacidade(turmaId);
            }
        }

        return repository.save(obj);
    }

    @Transactional
    public AlunoTurma concluir(Long id, LocalDate dataFim) {
        AlunoTurma matricula = buscar(id);
        matricula.setStatus(StatusMatricula.CONCLUIDA);
        matricula.setDataFim(dataFim != null ? dataFim : LocalDate.now());
        return repository.save(matricula);
    }

    private void validarMatriculaAtivaUnica(Long alunoId, Long turmaId, Long ignorarMatriculaId) {
        boolean jaAtiva = repository.findFirstByAluno_IdAndTurma_IdAndStatus(alunoId, turmaId, StatusMatricula.ATIVA)
                .filter(m -> ignorarMatriculaId == null || !m.getId().equals(ignorarMatriculaId))
                .isPresent();
        if (jaAtiva) {
            throw new RegraNegocioException("Este aluno já possui uma matrícula ativa nesta turma.");
        }
    }

    /** Não é permitido matricular (ou reativar matrícula) em turma que não esteja ATIVA. */
    private void validarTurmaAtiva(Long turmaId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma " + turmaId + " não encontrada."));
        if (turma.getStatus() != StatusTurma.ATIVA) {
            throw new RegraNegocioException("A turma \"" + turma.getTitulo() + "\" está inativa e não pode receber novas matrículas.");
        }
    }

    private void validarCapacidade(Long turmaId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma " + turmaId + " não encontrada."));
        if (turma.getCapacidadeMaxima() != null) {
            long ativos = repository.countByTurmaIdAndStatus(turmaId, StatusMatricula.ATIVA);
            if (ativos >= turma.getCapacidadeMaxima()) {
                throw new RegraNegocioException(
                        "Capacidade máxima da turma atingida (" + turma.getCapacidadeMaxima() + " alunos).");
            }
        }
    }
}
