package studojurata_api.repository.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.pessoa.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    List<Usuario> findByEscola_Id(Long escolaId);
}
