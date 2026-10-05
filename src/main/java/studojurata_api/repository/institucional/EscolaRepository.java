package studojurata_api.repository.institucional;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.institucional.Escola;

public interface EscolaRepository extends JpaRepository<Escola, Long> {
}
