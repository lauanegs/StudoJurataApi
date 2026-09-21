package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.TurmaDisciplina;

import java.util.Collection;
import java.util.List;

public interface TurmaDisciplinaRepository extends JpaRepository<TurmaDisciplina, Long> {
    List<TurmaDisciplina> findByProfessorId(Long professorId);

    /**
     * Vínculos das turmas informadas — usado para montar o escopo por turma sem
     * uma consulta por turma (evita N+1).
     */
    List<TurmaDisciplina> findByTurma_IdIn(Collection<Long> turmaIds);
}
