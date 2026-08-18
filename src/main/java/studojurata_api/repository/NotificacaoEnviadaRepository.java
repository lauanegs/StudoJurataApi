package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.NotificacaoEnviada;

import java.util.List;

public interface NotificacaoEnviadaRepository extends JpaRepository<NotificacaoEnviada, Long> {
    List<NotificacaoEnviada> findAllByOrderByCreatedAtDesc();
}
