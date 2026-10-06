package studojurata_api.security;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.model.pessoa.Usuario;
import studojurata_api.model.enums.TipoUsuario;

/** Proteção contra acesso indevido entre professores */
@Component
public class ProfessorAccessGuard {
    /** Lança 403 se o professor logado tentar acessar dados de outro professor */
    public void garantir(Long professorId) {
        CustomUserDetails principal = usuarioLogado();
        if (principal == null) {
            // Não deveria acontecer atrás do Spring Security (a rota já exige autenticação)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não autenticado.");
        }

        Usuario usuario = principal.getUsuario();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return;
        }
        if (usuario.getTipoUsuario() != TipoUsuario.PROFESSOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas professor ou administrador pode acessar dados de professor.");
        }
        if (!mesmoProfessor(usuario, professorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode acessar os seus próprios dados.");
        }
    }

    /** Id do professor logado */
    public Optional<Long> professorLogadoId() {
        CustomUserDetails principal = usuarioLogado();
        if (principal == null) {
            return Optional.empty();
        }

        Usuario usuario = principal.getUsuario();
        if (usuario.getTipoUsuario() != TipoUsuario.PROFESSOR || usuario.getProfessor() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usuario.getProfessor().getId());
    }

    /** O perfil logado é administrador — usado para decidir recorte irrestrito */
    public boolean ehAdministrador() {
        CustomUserDetails principal = usuarioLogado();
        return principal != null
                && principal.getUsuario().getTipoUsuario() == TipoUsuario.ADMINISTRADOR;
    }

    private boolean mesmoProfessor(Usuario usuario, Long professorId) {
        return professorId != null
                && usuario.getProfessor() != null
                && professorId.equals(usuario.getProfessor().getId());
    }

    private CustomUserDetails usuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }
}
