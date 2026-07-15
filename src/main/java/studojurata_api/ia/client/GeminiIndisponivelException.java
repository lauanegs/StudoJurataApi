package studojurata_api.ia.client;

/**
 * Lançada quando não foi possível obter questões da API do Gemini (chave não
 * configurada, timeout, erro HTTP, resposta em formato inesperado, etc.).
 * Sinaliza para GeracaoQuestaoIAService acionar o fallback para o banco de
 * questões já aprovadas (ver item 3.3 da Análise Crítica).
 */
public class GeminiIndisponivelException extends RuntimeException {

    public GeminiIndisponivelException(String message) {
        super(message);
    }

    public GeminiIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
