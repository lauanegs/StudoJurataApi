package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Professor;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.ProfessorRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;

import java.util.List;

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

    public List<Professor> listar() { return repository.findAll(); }

    public Professor buscar(Long id) {
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
