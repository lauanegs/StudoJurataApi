package studojurata_api.model.enums;

/**
 * Origem de criação de uma {@link studojurata_api.model.Questao}.
 *
 * Ver item 7.2 da Análise Crítica: registrar a origem permite auditoria de
 * qualidade (comparar desempenho/uso de questões geradas por IA vs. digitadas
 * manualmente pelo professor) e é pré-requisito para a regra de moderação do
 * item 7.3 (questões de origem IA nascem PENDENTE; questões de origem
 * PROFESSOR são consideradas aprovadas de imediato, pois já passaram pelo
 * julgamento humano no momento da criação).
 */
public enum OrigemQuestao {
    PROFESSOR,
    IA
}
