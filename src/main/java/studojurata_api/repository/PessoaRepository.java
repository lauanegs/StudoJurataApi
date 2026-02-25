package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Pessoa;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {
}