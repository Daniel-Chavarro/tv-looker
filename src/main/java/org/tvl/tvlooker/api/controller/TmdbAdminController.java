package org.tvl.tvlooker.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tvl.tvlooker.api.dto.response.TmdbOperationResponse;
import org.tvl.tvlooker.api.dto.response.TmdbStatusResponse;
import org.tvl.tvlooker.service.tmdb.TmdbDataCollectorService;
import org.tvl.tvlooker.service.tmdb.TmdbDataSynchronizerService;

import java.time.Instant;

/**
 * REST API controller for TMDB admin operations.
 * Provides endpoints to manually trigger TMDB data collection and synchronization.
 */
@RestController
@RequestMapping("/api/v1/admin/tmdb")
@RequiredArgsConstructor
public class TmdbAdminController {

    private final TmdbDataCollectorService collector;
    private final TmdbDataSynchronizerService synchronizer;

    /**
     * Triggers full TMDB data collection (genres + movies + TV shows) asynchronously.
     *
     * @return 202 Accepted with operation response
     */
    @PostMapping("/collect")
    public ResponseEntity<TmdbOperationResponse> collectAll() {
        collector.collectAllAsync();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(TmdbOperationResponse.started("TMDB data collection initiated"));
    }

    /**
     * Collects only genres from TMDB (synchronous operation).
     *
     * @return 200 OK with operation response
     */
    @PostMapping("/collect/genres")
    public ResponseEntity<TmdbOperationResponse> collectGenres() {
        collector.collectGenres();
        return ResponseEntity.ok(
                TmdbOperationResponse.completed("Genre collection completed"));
    }

    /**
     * Triggers movie collection asynchronously.
     *
     * @return 202 Accepted with operation response
     */
    @PostMapping("/collect/movies")
    public ResponseEntity<TmdbOperationResponse> collectMovies() {
        collector.collectPopularMoviesAsync();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(TmdbOperationResponse.started("Movie collection initiated"));
    }

    /**
     * Triggers TV show collection asynchronously.
     *
     * @return 202 Accepted with operation response
     */
    @PostMapping("/collect/tvshows")
    public ResponseEntity<TmdbOperationResponse> collectTvShows() {
        collector.collectPopularTvShowsAsync();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(TmdbOperationResponse.started("TV show collection initiated"));
    }

    /**
     * Manually triggers TMDB synchronization (synchronous operation).
     *
     * @return 200 OK with operation response
     */
    @PostMapping("/sync")
    public ResponseEntity<TmdbOperationResponse> sync() {
        synchronizer.synchronize();
        return ResponseEntity.ok(
                TmdbOperationResponse.completed("TMDB synchronization completed"));
    }

    /**
     * Gets the current status of TMDB collection and synchronization.
     *
     * @return 200 OK with status response
     */
    @GetMapping("/status")
    public ResponseEntity<TmdbStatusResponse> getStatus() {
        return ResponseEntity.ok(TmdbStatusResponse.builder()
                .collectorRunning(collector.isCollectionInProgress())
                .lastSyncDate(synchronizer.getLastSyncDate())
                .syncEnabled(true)
                .timestamp(Instant.now())
                .build());
    }
}
