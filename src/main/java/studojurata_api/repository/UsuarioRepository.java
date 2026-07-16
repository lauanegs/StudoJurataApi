package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);

    /** Correção 2.2 da Terceira Análise Crítica (isolamento multi-tenant): filtra por escola. */
    List<Usuario> findByEscola_Id(Long escolaId);
}
