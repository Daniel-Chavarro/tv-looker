package org.tvl.tvlooker.domain.exception;

/**
 * Thrown when attempting to start TMDB data collection while another collection is already in progress.
 * This exception ensures that only one collection operation runs at a time to prevent conflicts.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-15
 */
public class TmdbCollectionInProgressException extends RuntimeException {
    
    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public TmdbCollectionInProgressException(String message) {
        super(message);
    }
}
