package studojurata_api.exception;

/** Entrada ausente ou inválida, independente do estado do sistema */
public class RequisicaoInvalidaException extends RuntimeException {
    public RequisicaoInvalidaException(String message) {
        super(message);
    }
}
