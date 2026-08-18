package studojurata_api.dto;

import lombok.Getter;
import studojurata_api.model.enums.TipoUsuario;

@Getter
public class LoginResponse {

    private final Long usuarioId;
    private final String username;
    private final TipoUsuario tipoUsuario;
    private final Long pessoaId;
    private final String nomePessoa;

    public LoginResponse(Long usuarioId, String username, TipoUsuario tipoUsuario, Long pessoaId, String nomePessoa) {
        this.usuarioId = usuarioId;
        this.username = username;
        this.tipoUsuario = tipoUsuario;
        this.pessoaId = pessoaId;
        this.nomePessoa = nomePessoa;
    }
}
