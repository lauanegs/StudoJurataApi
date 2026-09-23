package studojurata_api.machinelearning.model.enums;

/**
 * De onde veio a decisão da recomendação. Gravado junto de cada recomendação
 * para que o resultado posterior possa ser comparado por origem (e para que
 * ninguém leia uma decisão de fallback como se fosse predição de modelo).
 */
public enum OrigemDecisao {
    /** Classificação produzida pelo modelo Weka treinado com resultados reais. */
    WEKA,
    /** Regra determinística da camada de ML (há histórico, mas não há modelo treinável). */
    REGRA_DETERMINISTICA,
    /** Aluno sem histórico no conteúdo/disciplina: decisão de partida, sem aprendizado. */
    FALLBACK_SEM_HISTORICO
}
