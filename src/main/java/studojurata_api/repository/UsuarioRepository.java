package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}