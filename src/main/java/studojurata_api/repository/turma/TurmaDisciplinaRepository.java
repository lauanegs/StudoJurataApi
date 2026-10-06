package studojurata_api.repository.turma;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.turma.TurmaDisciplina;

import java.util.Collection;
import java.util.List;

public interface TurmaDisciplinaRepository extends JpaRepository<TurmaDisciplina, Long> {
    List<TurmaDisciplina> findByProfessorId(Long professorId);

    List<TurmaDisciplina> findByTurma_IdIn(Collection<Long> turmaIds);
}
