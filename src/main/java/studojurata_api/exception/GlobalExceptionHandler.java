package studojurata_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/** Traduz as exceções de negócio para HTTP, mantendo os services livres de detalhes de HTTP. */
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return corpo(HttpStatus.BAD_REQUEST, mensagem.isBlank() ? "Dados inválidos." : mensagem, request);
    }

    /**
     * Exclusão física de um registro com dependentes é recusada pelo banco;
     * sem este handler, isso chegaria ao cliente como 500 genérico.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        return corpo(HttpStatus.CONFLICT,
                "Não é possível excluir/alterar este registro pois ele possui outros registros dependentes.", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        return corpo(status, ex.getReason(), request);
    }

    /**
     * AuthController chama o AuthenticationManager fora do filtro padrão do
     * Spring Security, então falhas de login chegam aqui. A mensagem é
     * genérica de propósito: informar se o usuário existe facilitaria a
     * enumeração de contas.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        return corpo(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.", request);
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
