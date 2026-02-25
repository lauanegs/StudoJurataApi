package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.AulaConteudo;

public interface AulaConteudoRepository extends JpaRepository<AulaConteudo, Long> {
}