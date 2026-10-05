package studojurata_api.repository.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.pessoa.Professor;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
}