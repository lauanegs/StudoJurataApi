package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Professor;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.TurmaDisciplinaSubstituto;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.ProfessorRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.repository.TurmaDisciplinaSubstitutoRepository;

import java.util.List;
import java.util.stream.Stream;

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
    private final TurmaDisciplinaSubstitutoRepository turmaDisciplinaSubstitutoRepository;

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

    /**
     * Lista as turmas/disciplinas em que este professor pode registrar aula:
     * como titular ou como substituto — os dois compartilham o mesmo
     * Plano de Ensino/Plano de Aula da TurmaDisciplina.
     */
    public List<TurmaDisciplina> turmasLecionadas(Long professorId) {
        List<TurmaDisciplina> comoTitular = turmaDisciplinaRepository.findByProfessorId(professorId);

        List<TurmaDisciplina> comoSubstituto = turmaDisciplinaSubstitutoRepository
                .findByProfessor_Id(professorId).stream()
                .filter(vinculo -> vinculo.getStatus() != StatusAtivoInativo.INATIVO)
                .map(TurmaDisciplinaSubstituto::getTurmaDisciplina)
                .toList();

        return Stream.concat(comoTitular.stream(), comoSubstituto.stream())
                .distinct()
                .toList();
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
