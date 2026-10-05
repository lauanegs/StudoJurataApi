package studojurata_api.repository.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.pessoa.Pessoa;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);
}