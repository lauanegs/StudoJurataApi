package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Aula;

public interface AulaRepository extends JpaRepository<Aula, Long> {
}