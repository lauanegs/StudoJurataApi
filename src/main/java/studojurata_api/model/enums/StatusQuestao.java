package studojurata_api.model.enums;

/**
 * PENDENTE  -> aguardando revisão do professor da disciplina (tela de Revisão).
 * APROVADA  -> liberada para uso em simulados e para reaproveitamento futuro.
 * REJEITADA -> reprovada na revisão; não pode ser vinculada a novos simulados.
 */
public enum StatusQuestao {
    PENDENTE,
    APROVADA,
    REJEITADA
}
