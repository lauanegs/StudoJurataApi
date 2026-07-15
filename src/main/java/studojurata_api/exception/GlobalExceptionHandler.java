package studojurata_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Tradução centralizada das exceptions de negócio da API para respostas HTTP
 * padronizadas. Mantém os services livres de detalhes de HTTP (fora os casos
 * legados que ainda usam ResponseStatusException diretamente, também
 * tratados aqui para compatibilidade).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return corpo(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErrorResponse> handleRegraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        return corpo(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleRequisicaoInvalida(RequisicaoInvalidaException ex, HttpServletRequest request) {
        return corpo(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Compatibilidade com orElseThrow() sem argumentos ainda presentes em services legados. */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException ex, HttpServletRequest request) {
        return corpo(HttpStatus.NOT_FOUND, "Recurso não encontrado.", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        return corpo(status, ex.getReason(), request);
    }

    private ResponseEntity<ErrorResponse> corpo(HttpStatus status, String mensagem, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
