package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for TMDB admin operations.
 * Used to indicate whether an operation has started (async) or completed (sync).
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-15
 */
@Builder
@Getter
@AllArgsConstructor
public class TmdbOperationResponse {
    private String status;      // "started" or "completed"
    private String message;
    private Instant timestamp;
    private Map<String, Object> data;

    /**
     * Creates a response indicating an async operation has started.
     *
     * @param message descriptive message
     * @return response with status "started"
     */
    public static TmdbOperationResponse started(String message) {
        return TmdbOperationResponse.builder()
                .status("started")
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a response indicating a sync operation has completed.
     *
     * @param message descriptive message
     * @return response with status "completed"
     */
    public static TmdbOperationResponse completed(String message) {
        return TmdbOperationResponse.builder()
                .status("completed")
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a response indicating a sync operation has completed with data.
     *
     * @param message descriptive message
     * @param data    additional data about the operation
     * @return response with status "completed" and data
     */
    public static TmdbOperationResponse completed(String message, Map<String, Object> data) {
        return TmdbOperationResponse.builder()
                .status("completed")
                .message(message)
                .timestamp(Instant.now())
                .data(data)
                .build();
    }
}
