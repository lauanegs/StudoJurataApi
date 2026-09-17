package studojurata_api.mapper;

import org.springframework.stereotype.Component;
import studojurata_api.dto.LoginResponse;
import studojurata_api.model.Usuario;

@Component
public class AuthMapper {

    public LoginResponse toLoginResponse(Usuario usuario) {
        return new LoginResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getTipoUsuario(),
                usuario.getPessoa() != null ? usuario.getPessoa().getId() : null,
                usuario.getPessoa() != null ? usuario.getPessoa().getNome() : null
        );
    }
}
