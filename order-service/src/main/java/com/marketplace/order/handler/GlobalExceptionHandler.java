package com.marketplace.order.handler;

import com.marketplace.common.dto.ErrorResponse;
import com.marketplace.order.constant.ExceptionMessages;
import com.marketplace.order.orders.exception.OrderNotFoundException;
import com.marketplace.order.orders.exception.OrderOperationException;
import com.marketplace.order.orders.exception.PaymentFailedException;
import com.marketplace.order.orders.exception.ProductNotAvailableException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

/**
 * Centralised mapping from domain exceptions to HTTP status codes.
 *
 * <p>Returns {@link ErrorResponse} (Java record from {@code common}) so the
 * shape is identical across all microservices.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============ 401 — authentication failures ============

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException ex, WebRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : ExceptionMessages.AUTH_REQUIRED;
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "Unauthorized", message, extractPath(request)));
    }

    // ============ 403 — forbidden ============

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden",
                        ExceptionMessages.ACCESS_DENIED, extractPath(request)));
    }

    // ============ 404 — resource not found ============

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage(), extractPath(request)));
    }

    // ============ 409 — conflict (illegal state) ============

    @ExceptionHandler(OrderOperationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(OrderOperationException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), extractPath(request)));
    }

    // ============ 402 — payment failed ============

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(ErrorResponse.of(402, "Payment Required", ex.getMessage(), extractPath(request)));
    }

    // ============ 400 — bad request / unavailable product ============

    @ExceptionHandler(ProductNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleProductUnavailable(ProductNotAvailableException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage(), extractPath(request)));
    }

    // ============ 400 — validation failures ============

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getAllErrors().stream()
                .map(err -> {
                    if (err instanceof org.springframework.validation.FieldError fe) {
                        return ErrorResponse.FieldError.of(fe.getField(),
                                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid");
                    }
                    return ErrorResponse.FieldError.of(err.getObjectName(),
                            err.getDefaultMessage() != null ? err.getDefaultMessage() : "invalid");
                })
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.validation(400, "Bad Request",
                        ExceptionMessages.VALIDATION_FAILED,
                        extractPath(request), fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   WebRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> ErrorResponse.FieldError.of(
                        v.getPropertyPath().toString(),
                        v.getMessage() != null ? v.getMessage() : "invalid"))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.validation(400, "Bad Request",
                        ExceptionMessages.VALIDATION_FAILED,
                        extractPath(request), fieldErrors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, WebRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Некорректный запрос";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", message, extractPath(request)));
    }

    // ============ 500 — unexpected fallback ============

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error",
                        ExceptionMessages.INTERNAL_ERROR, extractPath(request)));
    }

    private static String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        if (description == null) {
            return "";
        }
        // Strip the leading "uri=" prefix used by WebRequest.getDescription().
        return description.startsWith("uri=") ? description.substring(4) : description;
    }
}
