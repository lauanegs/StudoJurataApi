package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Endereco;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
}
