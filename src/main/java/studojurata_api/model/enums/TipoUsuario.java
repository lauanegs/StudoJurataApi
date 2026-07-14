package studojurata_api.model.enums;

/**
 * Papel (role) do usuário dentro da plataforma.
 * Usado tanto para regra de negócio (qual perfil o Usuario representa)
 * quanto para autorização via Spring Security (prefixo ROLE_ + name()).
 */
public enum TipoUsuario {
    ADMINISTRADOR,
    PROFESSOR,
    ALUNO
}
