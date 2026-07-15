package studojurata_api.ia.model.enums;

/**
 * De onde vieram, predominantemente, as questões entregues por uma chamada de
 * geração (ver GeracaoQuestaoIAService e item 3.3/9.3 da Análise Crítica).
 *
 * CACHE          -> todas reaproveitadas do banco de questões já aprovadas
 *                   para o mesmo conteúdo/dificuldade (nenhuma chamada à IA
 *                   foi necessária).
 * GEMINI         -> todas geradas nesta chamada pela API do Gemini.
 * MISTA          -> combinação de reaproveitadas (cache) e recém-geradas
 *                   (Gemini) para completar a quantidade solicitada.
 * FALLBACK_BANCO -> a API do Gemini falhou/está indisponível e a quantidade
 *                   solicitada (ou parte dela) foi completada com questões já
 *                   aprovadas do banco, relaxando o filtro de dificuldade
 *                   exata quando necessário (ver item 3.3: "fallback para
 *                   banco de questões existente quando a IA falhar").
 * FALHA          -> nem cache, nem Gemini, nem fallback conseguiram produzir
 *                   questões suficientes; nenhuma questão foi entregue.
 */
public enum OrigemResultadoGeracao {
    CACHE,
    GEMINI,
    MISTA,
    FALLBACK_BANCO,
    FALHA
}
