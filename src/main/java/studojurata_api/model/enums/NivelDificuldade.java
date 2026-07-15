package studojurata_api.model.enums;

/**
 * Peso de dificuldade de uma {@link studojurata_api.model.Questao}.
 *
 * Ver item 7.1/7.2 da Análise Crítica: cada questão deve carregar um peso de
 * dificuldade explícito, tanto para permitir calibração progressiva dos
 * simulados quanto para servir futuramente de sinal para a geração adaptativa
 * (a geração em si está fora do escopo deste módulo).
 */
public enum NivelDificuldade {
    FACIL,
    MEDIA,
    DIFICIL
}
