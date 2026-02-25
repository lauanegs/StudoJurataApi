package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Turma;

public interface TurmaRepository extends JpaRepository<Turma, Long> {
}