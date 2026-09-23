package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Aluno;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.model.enums.StatusTurma;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.ResponsavelAlunoRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.PlanejamentoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlunoTurmaService {

    private final AlunoTurmaRepository repository;
    private final AlunoRepository alunoRepository;
    private final ResponsavelAlunoRepository responsavelAlunoRepository;
    private final TurmaRepository turmaRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoProfessor escopoProfessor;
    private final PlanejamentoAccessGuard planejamentoAccessGuard;

    /**
     * Listagem escopada: ADMINISTRADOR vê todas as matrículas; PROFESSOR vê
     * apenas as das turmas em que leciona. O controller só chega aqui depois de
     * {@code AlunoAccessGuard.garantirAcessoDeGestao()}, então ALUNO não passa.
     */
    public List<AlunoTurma> listar() {
        if (usuarioAutenticado.ehAdministrador()) {
            return repository.findAll();
        }

        Set<Long> turmaIds = escopoProfessor.turmaIdsDoProfessor(usuarioAutenticado.professorId());
        return turmaIds.isEmpty() ? List.of() : repository.findByTurma_IdIn(turmaIds);
    }

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

        // Antes das regras de negócio: o professor só matricula nas próprias turmas.
        planejamentoAccessGuard.garantirEscritaNaTurma(obj.getTurma().getId(),
                "Você só pode matricular alunos nas suas turmas.");
        exigirResponsavelParaMenor(obj.getAluno().getId());

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

        // A matrícula atual e a turma de destino precisam estar no escopo: sem
        // isso, o id da URL deixaria um professor mexer na matrícula de outro.
        Long turmaAtualId = existente.getTurma() != null ? existente.getTurma().getId() : null;
        Long turmaDestinoId = obj.getTurma() != null ? obj.getTurma().getId() : turmaAtualId;

        planejamentoAccessGuard.garantirEscritaNaTurma(turmaAtualId,
                "Você só pode alterar matrículas das suas turmas.");
        if (!Objects.equals(turmaAtualId, turmaDestinoId)) {
            planejamentoAccessGuard.garantirEscritaNaTurma(turmaDestinoId,
                    "Você só pode matricular alunos nas suas turmas.");
        }

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

    /**
     * Aluno menor de idade precisa de ao menos um responsável vinculado: é a
     * escola que responde por ele. A tela de matrícula já exige esse cadastro
     * quando o aluno é novo; aqui a mesma regra vale para quem chama a API
     * direto. Aluno sem data de nascimento (cadastro antigo) não é bloqueado.
     */
    private void exigirResponsavelParaMenor(Long alunoId) {
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno " + alunoId + " não encontrado."));

        LocalDate nascimento = aluno.getPessoa() != null ? aluno.getPessoa().getDataNascimento() : null;
        boolean menorDeIdade = nascimento != null && nascimento.isAfter(LocalDate.now().minusYears(18));
        if (!menorDeIdade) {
            return;
        }

        if (responsavelAlunoRepository.findByAlunoId(alunoId).isEmpty()) {
            throw new RegraNegocioException(
                    "Aluno menor de idade precisa de um responsável vinculado antes da matrícula.");
        }
    }
}
