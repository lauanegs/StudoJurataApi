package studojurata_api.machinelearning.model.enums;

/**
 * Decisão explícita sobre a origem das questões de um simulado individual:
 * reaproveitar o banco existente ou pedir questões novas para a IA.
 */
public enum DecisaoGeracao {
    REUTILIZAR_BANCO,
    GERAR_POR_IA
}
