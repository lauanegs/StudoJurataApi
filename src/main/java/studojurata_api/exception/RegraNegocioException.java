package studojurata_api.exception;

/**
 * Lançada quando uma requisição, embora bem formada, viola uma regra de
 * negócio (ex.: lançar um simulado sem questões aprovadas, finalizar uma
 * tentativa já concluída). Traduzida para HTTP 409 por
 * GlobalExceptionHandler.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }
}
