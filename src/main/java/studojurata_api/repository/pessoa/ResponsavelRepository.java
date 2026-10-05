package studojurata_api.repository.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.pessoa.Responsavel;

public interface ResponsavelRepository extends JpaRepository<Responsavel, Long> {
}