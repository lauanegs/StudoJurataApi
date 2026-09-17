package studojurata_api.ia.client;

/**
 * Sinaliza a GeracaoQuestaoIAService que deve completar a geração com
 * questões já aprovadas do banco.
 */
public class GeminiIndisponivelException extends RuntimeException {

    public GeminiIndisponivelException(String message) {
        super(message);
    }

    public GeminiIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
