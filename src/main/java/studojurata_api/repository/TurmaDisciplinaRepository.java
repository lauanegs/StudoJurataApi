package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.TurmaDisciplina;

import java.util.List;

public interface TurmaDisciplinaRepository extends JpaRepository<TurmaDisciplina, Long> {
    /** Usado em ProfessorService.deletar (caso extremo "Professor deixa a escola"). */
    List<TurmaDisciplina> findByProfessorId(Long professorId);
}
