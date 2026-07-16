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
 * Correção 5.1 + caso extremo "Professor deixa a escola" (Segunda Análise
 * Crítica): deletar() agora faz soft-delete (Professor.status = INATIVO,
 * já existia o campo mas nada o usava) e, na mesma operação, desvincula o
 * professor das TurmaDisciplina em que lecionava — deixando-as "órfãs"
 * (professor = null) para que o Administrador reatribua um novo professor,
 * em vez de a turma continuar silenciosamente associada a um professor que
 * já não está mais na escola.
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

    /** Lista as turmas/disciplinas atualmente lecionadas por este professor (para o Admin reatribuir). */
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
}
