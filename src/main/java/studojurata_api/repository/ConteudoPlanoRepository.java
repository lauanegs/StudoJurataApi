package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.ConteudoPlano;

public interface ConteudoPlanoRepository extends JpaRepository<ConteudoPlano, Long> {
}