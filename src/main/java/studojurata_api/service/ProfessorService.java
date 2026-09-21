package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Professor;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.Usuario;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.ProfessorRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.security.ProfessorAccessGuard;
import studojurata_api.security.UsuarioAutenticado;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * deletar() inativa o professor e o desvincula das turmas em que lecionava,
 * para que o Administrador reatribua em vez de a turma continuar associada a
 * quem saiu da escola.
 */
@Service
@RequiredArgsConstructor
public class ProfessorService {

    private final ProfessorRepository repository;
    private final TurmaDisciplinaRepository turmaDisciplinaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final ProfessorAccessGuard professorAccessGuard;

    /**
     * Listagem escopada pelo usuário logado:
     * <ul>
     *   <li>ADMINISTRADOR: todos — {@code Professor} não tem vínculo de escola no
     *       modelo, então a administração mantém a visão completa;</li>
     *   <li>PROFESSOR: apenas o próprio cadastro (as telas que precisam dele
     *       como referência continuam funcionando);</li>
     *   <li>ALUNO: professores que lecionam nas turmas em que está matriculado;</li>
     *   <li>sem autenticação: 403.</li>
     * </ul>
     */
    public List<Professor> listar() {
        Usuario usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return repository.findAll();
        }
        if (usuario.getTipoUsuario() == TipoUsuario.PROFESSOR) {
            return apenasOMesmo(usuario);
        }
        return professoresDasTurmasDoAluno(usuario);
    }

    /** Professor enxerga só o próprio cadastro; sem vínculo, ninguém. */
    private List<Professor> apenasOMesmo(Usuario usuario) {
        if (usuario.getProfessor() == null || usuario.getProfessor().getId() == null) {
            return List.of();
        }
        return repository.findById(usuario.getProfessor().getId())
                .map(List::of)
                .orElseGet(List::of);
    }

    /**
     * Professores das turmas do aluno. Três consultas fixas (matrículas,
     * vínculos das turmas, professores), sem 1+N; escopo vazio não consulta as
     * etapas seguintes.
     */
    private List<Professor> professoresDasTurmasDoAluno(Usuario usuario) {
        if (usuario.getAluno() == null || usuario.getAluno().getId() == null) {
            return List.of();
        }

        Set<Long> turmaIds = alunoTurmaRepository.findByAluno_Id(usuario.getAluno().getId()).stream()
                .map(AlunoTurma::getTurma)
                .filter(Objects::nonNull)
                .map(Turma::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (turmaIds.isEmpty()) {
            return List.of();
        }

        Set<Long> professorIds = turmaDisciplinaRepository.findByTurma_IdIn(turmaIds).stream()
                .map(TurmaDisciplina::getProfessor)
                .filter(Objects::nonNull)
                .map(Professor::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (professorIds.isEmpty()) {
            return List.of();
        }

        return repository.findAllById(professorIds);
    }

    public Professor buscar(Long id) {
        professorAccessGuard.garantir(id);
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor " + id + " não encontrado."));
    }

    public Professor salvar(Professor obj) {
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public Professor atualizar(Long id, Professor obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    /** Turmas/disciplinas em que este professor é titular. */
    public List<TurmaDisciplina> turmasLecionadas(Long professorId) {
        professorAccessGuard.garantir(professorId);
        return turmaDisciplinaRepository.findByProfessorId(professorId);
    }

    public void deletar(Long id) {
        Professor professor = buscar(id);
        professor.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(professor);

        List<TurmaDisciplina> turmas = turmaDisciplinaRepository.findByProfessorId(id);
        for (TurmaDisciplina td : turmas) {
            td.setProfessor(null);
            turmaDisciplinaRepository.save(td);
        }
    }

    public Professor ativar(Long id) {
        Professor professor = buscar(id);
        professor.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(professor);
    }
}
