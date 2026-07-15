package studojurata_api.ia.model.enums;

/**
 * Nível de domínio estimado de um aluno sobre um conteúdo específico,
 * usado pelo reforço adaptativo (ver item 1.5 da Análise Crítica) para
 * calibrar a urgência/dificuldade dos próximos reforços.
 *
 * MVP: atribuído heuristicamente (ver RevisaoConteudoService) a partir da
 * quantidade de reforços já realizados e do desempenho recente do aluno
 * naquele conteúdo (item 7.4). Não há, nesta etapa, um modelo estatístico
 * mais sofisticado — o objetivo é apenas estruturar o dado para que uma
 * geração adaptativa mais refinada possa vir a consumi-lo no futuro.
 */
public enum NivelDominio {
    BAIXO,
    MEDIO,
    ALTO
}
