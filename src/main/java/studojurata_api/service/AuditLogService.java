package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import studojurata_api.model.AuditLog;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.repository.AuditLogRepository;

import java.util.List;

/**
 * Ponto único de registro de auditoria (item 2.9/10.4 da Segunda Análise
 * Crítica). Usado hoje por NotaService, SimuladoAlunoService e AulaService —
 * as três entidades priorizadas na recomendação ("Nota, SimuladoAluno e Aula
 * primeiro"). Pode ser chamado de qualquer outro service no futuro sem
 * mudança de contrato.
 */
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

    public List<AuditLog> historicoDaEntidade(String entidade, Long entidadeId) {
        return repository.findByEntidadeAndEntidadeIdOrderByIdDesc(entidade, entidadeId);
    }

    private String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}
