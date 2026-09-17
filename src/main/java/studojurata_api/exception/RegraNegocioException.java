package studojurata_api.exception;

/**
 * Requisição bem formada que o estado atual não permite (ex.: lançar simulado
 * com questões não aprovadas). HTTP 409.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }
}
