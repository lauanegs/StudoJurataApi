package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Professor;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
}