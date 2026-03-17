package org.tvl.tvlooker.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.tvl.tvlooker.domain.exception.InsufficientDataException;
import org.tvl.tvlooker.domain.exception.ItemNotFoundException;
import org.tvl.tvlooker.domain.exception.TmdbCollectionInProgressException;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;

import java.net.URI;
import java.security.Timestamp;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API endpoints.
 * Handles TMDB-specific and general exceptions with appropriate HTTP status codes.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles TmdbCollectionInProgressException when a collection operation is already running.
     *
     * @param ex the exception
     * @return 409 Conflict with error details
     */
    @ExceptionHandler(TmdbCollectionInProgressException.class)
    public ResponseEntity<ErrorResponse> handleTmdbCollectionInProgress(TmdbCollectionInProgressException ex, HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/tmdb-collection-in-progress",
                "TMDB Collection In Progress",
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.warn("TMDB collection in progress: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /** Handles UserNotFoundException when a requested user is not found.
     *
     * @param ex the exception
     * @return 404 Not Found with error details
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/user-not-found",
                "User Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /** Handles ItemNotFoundException when a requested item (movie/TV show) is not found.
     *
     * @param ex the exception
     * @return 404 Not Found with error details
     */
    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleItemNotFound(
            ItemNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/item-not-found",
                "Item Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.warn("Item not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /** Handles IllegalArgumentException for invalid input parameters.
     *
     * @param ex the exception
     * @return 400 Bad Request with error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/bad-request",
                "Invalid Request",
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /** Handles validation errors from @Valid annotated request bodies.
     *
     * @param ex the exception
     * @return 400 Bad Request with detailed validation error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/validation-failed",
                "Validation Failed",
                HttpStatus.BAD_REQUEST.value(),
                errors,
                request.getRequestURI()
        );
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /** Handles InsufficientDataException when there is not enough data to generate recommendations.
     *
     * @param ex the exception
     * @return 500 Internal Server Error with error details
     */
    @ExceptionHandler(InsufficientDataException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientData(
            InsufficientDataException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/insufficient-data",
                "Insufficient Data",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.error("Insufficient data for recommendations: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /** Handles any uncaught exceptions that occur during request processing.
     *
     * @param ex the exception
     * @return 500 Internal Server Error with generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/internal-error",
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                request.getRequestURI()
        );
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /** Utility method to build ErrorResponse objects with consistent structure.
     *
     * @param ex the exception
     * @param type a URI that identifies the error type
     * @param title a short, human-readable summary of the error
     * @param status the HTTP status code
     * @param detail a detailed message about the error
     * @param instance the URI of the request that caused the error
     * @return an ErrorResponse object containing the error details
     */
    private ErrorResponse buildErrorResponse(
            Exception ex,
            String type,
            String title,
            int status,
            String detail,
            String instance) {

        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(status), detail)
                .title(title)
                .type(URI.create(type))
                .instance(URI.create(instance))
                .build();

    }
}
