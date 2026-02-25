package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.PlanoEnsino;

public interface PlanoEnsinoRepository extends JpaRepository<PlanoEnsino, Long> {
}