package studojurata_api.exception;

/**
 * Lançada quando um recurso (entidade) referenciado por id não existe.
 * Traduzida para HTTP 404 por GlobalExceptionHandler.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
