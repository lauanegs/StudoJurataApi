package studojurata_api.exception;

/**
 * Lançada quando parâmetros de entrada estão ausentes ou são inválidos
 * (ex.: quantidade <= 0, lista obrigatória vazia). Traduzida para HTTP 400
 * por GlobalExceptionHandler.
 */
public class RequisicaoInvalidaException extends RuntimeException {

    public RequisicaoInvalidaException(String message) {
        super(message);
    }
}
