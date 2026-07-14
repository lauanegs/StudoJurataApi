package studojurata_api.model.enums;

/**
 * Status da Turma. Substitui o campo String livre anterior, evitando
 * inconsistências de valores (ex.: "ativo" vs "Ativo" vs "ATIVO").
 */
public enum StatusTurma {
    ATIVA,
    INATIVA
}
