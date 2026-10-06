package studojurata_api.ia.client;

public class GeminiIndisponivelException extends RuntimeException {
    public GeminiIndisponivelException(String message) {
        super(message);
    }

    public GeminiIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
