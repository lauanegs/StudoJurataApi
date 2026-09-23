package studojurata_api.machinelearning.model.enums;

/**
 * Classe alvo do modelo Weka e da regra determinística: o quanto o aluno
 * precisa revisar o conteúdo agora. É a decisão que orienta quantidade,
 * distribuição de dificuldade e intervalo até a próxima revisão.
 */
public enum NecessidadeRevisao {
    REVISAR_AGORA,
    REVISAR_EM_BREVE,
    DOMINIO_ESTAVEL
}
