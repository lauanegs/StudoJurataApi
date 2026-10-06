package studojurata_api.exception;

/** Requisição bem formada que o estado atual não permite (ex */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String message) {
        super(message);
    }
}
