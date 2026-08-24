package com.marketplace.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error envelope returned by every REST endpoint across the marketplace.
 *
 * <p>Returned from each service's {@code GlobalExceptionHandler}. The shape is
 * stable so that the API gateway and front-end can rely on it.</p>
 *
 * <p><b>IMMUTABLE contract</b> — shape is shared across all REST APIs.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors
) {

    /**
     * Validation error entry: which input field failed and why.
     */
    public record FieldError(String field, String message) {

        public static FieldError of(String field, String message) {
            return new FieldError(field, message);
        }
    }

    /**
     * Convenience factory for plain (non-validation) errors.
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now(), null);
    }

    /**
     * Convenience factory for validation errors (e.g., {@code @Valid} failures).
     */
    public static ErrorResponse validation(int status, String error, String message,
                                           String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(status, error, message, path, Instant.now(), fieldErrors);
    }
}