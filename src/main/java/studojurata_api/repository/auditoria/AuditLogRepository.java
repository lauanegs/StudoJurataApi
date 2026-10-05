package studojurata_api.repository.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.auditoria.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
