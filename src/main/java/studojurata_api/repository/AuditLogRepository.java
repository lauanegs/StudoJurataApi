package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
