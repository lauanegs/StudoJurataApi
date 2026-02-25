package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Disciplina;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {
}