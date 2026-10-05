package studojurata_api.dto.autenticacao;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    private String username;
    private String senha;
}
