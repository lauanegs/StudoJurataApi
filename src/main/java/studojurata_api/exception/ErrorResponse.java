package studojurata_api.exception;

import java.time.LocalDateTime;

/**
 * Corpo padrão de erro devolvido pela API (ver GlobalExceptionHandler).
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
