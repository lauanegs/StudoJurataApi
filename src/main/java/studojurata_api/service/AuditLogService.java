package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import studojurata_api.model.AuditLog;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.repository.AuditLogRepository;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repository;

    public void registrar(String entidade, Long entidadeId, AcaoAuditoria acao, String detalhes) {
        AuditLog log = new AuditLog();
        log.setEntidade(entidade);
        log.setEntidadeId(entidadeId);
        log.setAcao(acao);
        log.setUsuario(usuarioAtual());
        log.setDetalhes(detalhes);
        repository.save(log);
    }

    private String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}
