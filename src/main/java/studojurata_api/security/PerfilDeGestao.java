package studojurata_api.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.model.pessoa.Usuario;
import studojurata_api.model.enums.TipoUsuario;

/** Perfil de gestão (PROFESSOR ou ADMINISTRADOR) */
public final class PerfilDeGestao {
    private PerfilDeGestao() {
    }

    /** 403 com a mensagem recebida quando o usuário não é PROFESSOR nem ADMINISTRADOR */
    public static void exigir(Usuario usuario, String mensagem) {
        if (usuario == null
                || (usuario.getTipoUsuario() != TipoUsuario.PROFESSOR
                        && usuario.getTipoUsuario() != TipoUsuario.ADMINISTRADOR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, mensagem);
        }
    }
}
