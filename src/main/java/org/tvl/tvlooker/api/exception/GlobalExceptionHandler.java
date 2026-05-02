package org.tvl.tvlooker.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.tvl.tvlooker.domain.exception.NoRecommendationsAvailableException;
import org.tvl.tvlooker.domain.exception.NoDataProviderException;
import org.tvl.tvlooker.domain.exception.InsufficientDataException;
import org.tvl.tvlooker.domain.exception.InvalidEngineConfigurationException;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.exception.ItemNotFoundException;
import org.tvl.tvlooker.domain.exception.ReviewNotFoundException;
import org.tvl.tvlooker.domain.exception.ActorNotFoundException;
import org.tvl.tvlooker.domain.exception.DirectorNotFoundException;
import org.tvl.tvlooker.domain.exception.GenreNotFoundException;
import org.tvl.tvlooker.domain.exception.InteractionNotFoundException;
import org.tvl.tvlooker.domain.exception.ListFavoriteNotFoundException;
import org.tvl.tvlooker.domain.exception.TmdbCollectionInProgressException;

import java.net.URI;
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
    public ResponseEntity<ErrorResponse> handleTmdbCollectionInProgress(
            TmdbCollectionInProgressException ex,
            HttpServletRequest request) {
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

        /** Handles ReviewNotFoundException when a requested review is not found.
        *
        * @param ex the exception
        * @return 404 Not Found with error details
        */
    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReviewNotFound(
            ReviewNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/review-not-found",
                "Review Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.warn("Review not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /** Handles ActorNotFoundException when a requested actor is not found.
     *
     * @param ex the exception
     * @return 404 Not Found with error details
     */
    @ExceptionHandler(ActorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleActorNotFound(
            ActorNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/actor-not-found",
                "Actor Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.warn("Actor not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /** Handles DirectorNotFoundException when a requested director is not found.
     *
     * @param ex the exception
     * @return 404 Not Found with error details
     */
    @ExceptionHandler(DirectorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDirectorNotFound(
            DirectorNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/director-not-found",
                "Director Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.warn("Director not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /** Handles GenreNotFoundException when a requested genre is not found.
     *
     * @param ex the exception
     * @return 404 Not Found with error details
     */
    @ExceptionHandler(GenreNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGenreNotFound(
            GenreNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/genre-not-found",
                "Genre Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.warn("Genre not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /** Handles InteractionNotFoundException when a requested user-item interaction is not found.
     *
     * @param ex the exception
     * @return 404 Not Found with error details
     */
    @ExceptionHandler(InteractionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInteractionNotFound(
            InteractionNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/interaction-not-found",
                "Interaction Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.warn("Interaction not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ListFavoriteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleListFavoriteNotFound(
            ListFavoriteNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/list-favorite-not-found",
                "List Favorite Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.warn("List favorite not found: {}", ex.getMessage());
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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/unauthorized",
                "Unauthorized",
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
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

    /** Handles InvalidEngineConfigurationException when the recommendation engine is misconfigured.
     *
     * @param ex the exception
     * @return 500 Internal Server Error with error details
     */
    @ExceptionHandler(InvalidEngineConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEngineConfiguration(
            InvalidEngineConfigurationException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/invalid-engine-configuration",
                "Invalid Engine Configuration",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.error("Invalid engine configuration: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /** Handles NoDataProviderException when no data provider is available for generating recommendations.
     *
     * @param ex the exception
     * @return 500 Internal Server Error with error details
     */
    @ExceptionHandler(NoDataProviderException.class)
    public ResponseEntity<ErrorResponse> handleNoDataProvider(
            NoDataProviderException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/no-data-provider",
                "No Data Provider",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.error("No data provider available: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /** Handles NoRecommendationsAvailableException when the recommendation engine cannot generate any recommendations.
     *
     * @param ex the exception
     * @return 500 Internal Server Error with error details
     */
    @ExceptionHandler(NoRecommendationsAvailableException.class)
    public ResponseEntity<ErrorResponse> handleNoRecommendationsAvailable(
            NoRecommendationsAvailableException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
                ex,
                "/errors/no-recommendations-available",
                "No Recommendations Available",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        log.error("No recommendations available: {}", ex.getMessage());
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
