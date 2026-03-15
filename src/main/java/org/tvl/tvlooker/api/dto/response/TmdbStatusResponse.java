package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for TMDB system status.
 * Provides information about the current state of the TMDB collector and synchronizer.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-15
 */
@Builder
@Getter
@AllArgsConstructor
public class TmdbStatusResponse {
    private boolean collectorRunning;
    private LocalDate lastSyncDate;
    private boolean syncEnabled;
    private Instant timestamp;
}
