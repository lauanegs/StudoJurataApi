package studojurata_api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Escola do usuário autenticado, para isolamento multi-tenant das listagens */
@Component
public class EscolaContext {
    public Long escolaAtualId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return null;
        }
        var usuario = principal.getUsuario();
        return usuario.getEscola() != null ? usuario.getEscola().getId() : null;
    }
}
