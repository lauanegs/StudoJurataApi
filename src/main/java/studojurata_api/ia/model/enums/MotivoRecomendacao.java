package studojurata_api.ia.model.enums;

/**
 * Motivo pelo qual um conteúdo é recomendado para reforço de um aluno
 * (ver RecomendacaoService, itens 1.4, 1.5 e 7.4 da Análise Crítica).
 *
 * REPETICAO_ESPACADA  -> a data de próximo reforço (RevisaoConteudo,
 *                        calculada por 2ⁿ dias) já foi atingida.
 * BAIXO_APROVEITAMENTO -> a taxa de acerto do aluno nas questões daquele
 *                        conteúdo está abaixo de 60% (limiar definido no
 *                        item 1.4 da Análise Crítica).
 */
public enum MotivoRecomendacao {
    REPETICAO_ESPACADA,
    BAIXO_APROVEITAMENTO
}
