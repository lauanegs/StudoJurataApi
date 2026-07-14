package studojurata_api.dto;

import studojurata_api.model.enums.TipoUsuario;

public class LoginResponse {

    private Long usuarioId;
    private String username;
    private TipoUsuario tipoUsuario;
    private Long pessoaId;
    private String nomePessoa;

    public LoginResponse(Long usuarioId, String username, TipoUsuario tipoUsuario, Long pessoaId, String nomePessoa) {
        this.usuarioId = usuarioId;
        this.username = username;
        this.tipoUsuario = tipoUsuario;
        this.pessoaId = pessoaId;
        this.nomePessoa = nomePessoa;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getUsername() {
        return username;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public Long getPessoaId() {
        return pessoaId;
    }

    public String getNomePessoa() {
        return nomePessoa;
    }
}
