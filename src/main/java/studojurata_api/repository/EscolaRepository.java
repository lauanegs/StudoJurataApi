package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Escola;

public interface EscolaRepository extends JpaRepository<Escola, Long> {
}
