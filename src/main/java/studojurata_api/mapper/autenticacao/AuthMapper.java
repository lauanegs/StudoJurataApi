package studojurata_api.mapper.autenticacao;

import org.springframework.stereotype.Component;
import studojurata_api.dto.autenticacao.LoginResponse;
import studojurata_api.model.pessoa.Usuario;

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
