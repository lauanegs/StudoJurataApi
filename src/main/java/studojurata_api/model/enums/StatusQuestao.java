package studojurata_api.model.enums;

/**
 * Status de moderação de uma {@link studojurata_api.model.Questao}.
 *
 * Ver item 7.3 da Análise Crítica: questões geradas pela IA nascem com status
 * PENDENTE e só podem ser reaproveitadas em novos simulados (banco de
 * reaproveitamento) após aprovação do professor. Questões digitadas
 * diretamente pelo professor (origem PROFESSOR) nascem APROVADA.
 *
 * PENDENTE  -> aguardando revisão do professor da disciplina (tela de Revisão).
 * APROVADA  -> liberada para uso em simulados e para reaproveitamento futuro.
 * REJEITADA -> reprovada na revisão; não pode ser vinculada a novos simulados.
 */
public enum StatusQuestao {
    PENDENTE,
    APROVADA,
    REJEITADA
}
