package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.CursoDisciplina;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.Usuario;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusPlano;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.CursoDisciplinaRepository;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.repository.PlanoAulaRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.security.PlanejamentoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Só aceita disciplinas da grade curricular do curso da turma. */
@Service
@RequiredArgsConstructor
public class TurmaDisciplinaService {

    private final TurmaDisciplinaRepository repository;
    private final TurmaRepository turmaRepository;
    private final CursoDisciplinaRepository cursoDisciplinaRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final PlanoEnsinoRepository planoEnsinoRepository;
    private final PlanoAulaRepository planoAulaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final PlanejamentoAccessGuard planejamentoAccessGuard;

    /**
     * Listagem escopada: ADMINISTRADOR vê todos os vínculos; PROFESSOR vê apenas
     * os seus; ALUNO vê os vínculos das turmas em que está matriculado.
     */
    public List<TurmaDisciplina> listar() {
        Usuario usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return repository.findAll();
        }

        if (usuario.getTipoUsuario() == TipoUsuario.PROFESSOR) {
            return usuario.getProfessor() == null ? List.of()
                    : repository.findByProfessorId(usuario.getProfessor().getId());
        }

        return vinculosDasTurmasDoAluno(usuario);
    }

    private List<TurmaDisciplina> vinculosDasTurmasDoAluno(Usuario usuario) {
        if (usuario.getAluno() == null || usuario.getAluno().getId() == null) {
            return List.of();
        }

        Set<Long> turmaIds = alunoTurmaRepository.findByAluno_Id(usuario.getAluno().getId()).stream()
                .map(AlunoTurma::getTurma)
                .filter(Objects::nonNull)
                .map(Turma::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return turmaIds.isEmpty() ? List.of() : repository.findByTurma_IdIn(turmaIds);
    }

    public TurmaDisciplina buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo turma-disciplina " + id + " não encontrado."));
    }

    public TurmaDisciplina salvar(TurmaDisciplina obj) {
        planejamentoAccessGuard.garantirEscritaNaTurma(
                obj != null && obj.getTurma() != null ? obj.getTurma().getId() : null,
                "Você só pode vincular disciplinas às suas turmas.");
        validar(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    /**
     * Soft-delete, recusado enquanto houver plano de ensino ou de aula ATIVO
     * no vínculo: o professor perderia o rastro do que está ministrando.
     */
    public void deletar(Long id) {
        TurmaDisciplina turmaDisciplina = buscar(id);
        planejamentoAccessGuard.garantirEscrita(turmaDisciplina.getId(),
                "Você só pode desvincular disciplinas das suas turmas.");

        boolean temPlanoEnsinoAtivo = planoEnsinoRepository.findByTurmaDisciplina_Id(id).stream()
                .anyMatch(plano -> plano.getStatus() == StatusPlano.ATIVO);
        boolean temPlanoAulaAtivo = planoAulaRepository.findByTurmaDisciplina_Id(id).stream()
                .anyMatch(plano -> plano.getStatus() == StatusPlano.ATIVO);

        if (temPlanoEnsinoAtivo || temPlanoAulaAtivo) {
            throw new RegraNegocioException(
                    "Esta disciplina tem plano de ensino ou plano de aula ativo nesta turma e não pode ser "
                            + "desvinculada — conclua (ou exclua) o(s) plano(s) primeiro.");
        }

        turmaDisciplina.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(turmaDisciplina);
    }

    private void validar(TurmaDisciplina obj) {
        if (obj.getTurma() == null || obj.getTurma().getId() == null) {
            throw new RequisicaoInvalidaException("Turma é obrigatória para o vínculo.");
        }
        if (obj.getDisciplina() == null || obj.getDisciplina().getId() == null) {
            throw new RequisicaoInvalidaException("Disciplina é obrigatória para o vínculo.");
        }

        Turma turma = turmaRepository.findById(obj.getTurma().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma " + obj.getTurma().getId() + " não encontrada."));
        obj.setTurma(turma);

        Long cursoId = turma.getCurso() != null ? turma.getCurso().getId() : null;
        Long disciplinaId = obj.getDisciplina().getId();

        // Disciplina inativada não pode ser ofertada em turma nova: o vínculo
        // ficaria inutilizável (sem plano de ensino possível). Vínculos
        // históricos continuam válidos, porque só o cadastro de novos passa aqui.
        Disciplina disciplinaDoVinculo = disciplinaRepository.findById(disciplinaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina " + disciplinaId + " não encontrada."));
        if (disciplinaDoVinculo.getStatus() == StatusAtivoInativo.INATIVO) {
            throw new RegraNegocioException(
                    "A disciplina \"" + disciplinaDoVinculo.getTitulo() + "\" está inativa e não pode ser vinculada a uma turma.");
        }
        obj.setDisciplina(disciplinaDoVinculo);

        boolean estaNaGrade = cursoId != null && cursoDisciplinaRepository.findByCurso_Id(cursoId).stream()
                .filter(item -> item.getStatus() == StatusAtivoInativo.ATIVO)
                .map(CursoDisciplina::getDisciplina)
                .anyMatch(disciplina -> disciplina != null && disciplinaId.equals(disciplina.getId()));

        if (!estaNaGrade) {
            throw new RequisicaoInvalidaException(
                    "Esta disciplina não faz parte da grade curricular do curso desta turma.");
        }
    }
}
