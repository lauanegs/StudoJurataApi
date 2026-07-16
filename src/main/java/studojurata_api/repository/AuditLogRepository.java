package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.AuditLog;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntidadeAndEntidadeIdOrderByIdDesc(String entidade, Long entidadeId);
    List<AuditLog> findByEntidadeOrderByIdDesc(String entidade);
}
