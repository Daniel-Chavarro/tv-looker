package org.tvl.tvlooker.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.TmdbCollectionInProgressException;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Collects and persists data from the TMDB API into the local database.
 *
 * <p>Responsible for the initial bulk load of:</p>
 * <ul>
 *   <li>Genres (from /genre/movie/list and /genre/tv/list)</li>
 *   <li>Popular movies (from /movie/popular, paginated)</li>
 *   <li>Popular TV shows (from /tv/popular, paginated)</li>
 *   <li>Credits for each item (actors and directors)</li>
 * </ul>
 *
 * <p>Design Principles:</p>
 * <ul>
 *   <li>Idempotent: Re-running does not create duplicates (checks by tmdbId)</li>
 *   <li>Per-item error isolation: One item failing does not abort the batch</li>
 *   <li>Rate-limit aware: Adds configurable delay between API calls</li>
 * </ul>
 *
 * <p>Activated only when {@code tmdb.collector.run-on-startup=true}</p>
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-10
 */
@Service
@Slf4j
public class TmdbDataCollectorService {
    private final ItemRepository itemRepository;
    private final TmdbDataFetcher dataFetcher;
    private final TmdbItemPersistenceService persistenceService;

    /**
     * Prevents concurrent collection operations
     */
    private final AtomicBoolean collectionInProgress = new AtomicBoolean(false);

    @Value("${tmdb.collector.max-pages:50}")
    private int maxPages;

    @Value("${tmdb.persistence.batch-size:50}")
    private int batchSize;

    public TmdbDataCollectorService(
            ItemRepository itemRepository,
            TmdbDataFetcher dataFetcher,
            TmdbItemPersistenceService persistenceService) {
        this.itemRepository = itemRepository;
        this.dataFetcher = dataFetcher;
        this.persistenceService = persistenceService;
    }

    /**
     * Async version of collectAll() for REST API trigger.
     * Prevents concurrent collection operations.
     *
     * @return CompletableFuture that completes when collection is done
     * @throws TmdbCollectionInProgressException if collection is already running
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> collectAllAsync() {
        if (!collectionInProgress.compareAndSet(false, true)) {
            throw new TmdbCollectionInProgressException(
                    "TMDB data collection is already in progress");
        }
        try {
            log.info("========== TMDB DATA COLLECTION STARTED (ASYNC) ==========");
            collectAll();
            log.info("========== TMDB DATA COLLECTION FINISHED ==========");
            return CompletableFuture.completedFuture(null);
        } finally {
            collectionInProgress.set(false);
        }
    }

    /**
     * Async version of collectPopularMovies() for REST API trigger.
     *
     * @return CompletableFuture that completes when movie collection is done
     * @throws TmdbCollectionInProgressException if collection is already running
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> collectPopularMoviesAsync() {
        if (!collectionInProgress.compareAndSet(false, true)) {
            throw new TmdbCollectionInProgressException(
                    "TMDB data collection is already in progress");
        }
        try {
            log.info("========== TMDB MOVIE COLLECTION STARTED (ASYNC) ==========");
            collectPopularMovies();
            log.info("========== TMDB MOVIE COLLECTION FINISHED ==========");
            return CompletableFuture.completedFuture(null);
        } finally {
            collectionInProgress.set(false);
        }
    }

    /**
     * Async version of collectPopularTvShows() for REST API trigger.
     *
     * @return CompletableFuture that completes when TV show collection is done
     * @throws TmdbCollectionInProgressException if collection is already running
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> collectPopularTvShowsAsync() {
        if (!collectionInProgress.compareAndSet(false, true)) {
            throw new TmdbCollectionInProgressException(
                    "TMDB data collection is already in progress");
        }
        try {
            log.info("========== TMDB TV SHOW COLLECTION STARTED (ASYNC) ==========");
            collectPopularTvShows();
            log.info("========== TMDB TV SHOW COLLECTION FINISHED ==========");
            return CompletableFuture.completedFuture(null);
        } finally {
            collectionInProgress.set(false);
        }
    }

    /**
     * Checks if a collection operation is currently in progress.
     *
     * @return true if collection is running, false otherwise
     */
    public boolean isCollectionInProgress() {
        return collectionInProgress.get();
    }

    /**
     * Main entry point: orchestrates the full data collection.
     */
    public void collectAll() {
        collectGenres();
        collectPopularMovies();
        collectPopularTvShows();
    }

    /**
     * Fetches and persists all genres from TMDB (movie + TV, deduplicated).
     */
    public void collectGenres() {
        log.info("Collecting genres...");

        CompletableFuture<TmdbGenreListDto> movieGenresFuture = dataFetcher.fetchGenresAsync(TmdbMediaType.MOVIE);
        CompletableFuture<TmdbGenreListDto> tvGenresFuture = dataFetcher.fetchGenresAsync(TmdbMediaType.TV);

        TmdbGenreListDto movieGenres = movieGenresFuture.join();
        TmdbGenreListDto tvGenres = tvGenresFuture.join();

        persistenceService.persistGenres(movieGenres);
        persistenceService.persistGenres(tvGenres);

        log.info("Genres collected");
    }

    /**
     * Fetches popular movies from TMDB page by page and persists each batch.
     * <p>
     * NEW APPROACH:
     * 1. Fetch all movie IDs from pages sequentially
     * 2. Batch fetch all movie details with credits in parallel
     * 3. Bulk cache all entities (actors, directors, genres)
     * 4. Build items with pre-cached entities
     * 5. Save batch transactionally
     */
    public void collectPopularMovies() {
        log.info("Collecting popular movies (max {} pages)...", maxPages);

        int totalCollected = 0;
        int totalSkipped = 0;

        for (int page = 1; page <= maxPages; page++) {
            TmdbPagedResponseDto<TmdbMovieDto> response = dataFetcher.fetchPopularMoviesAsync(page).join();


            if (response == null || response.results() == null || response.results().isEmpty()) {
                break;
            }

            if (page >= response.totalPages()) {
                break;
            }

            int collected = persistenceService.discoverAndPersistNewMovies(response.results());
            totalCollected += collected;
            totalSkipped += response.results().size() - collected;


            if (page % 10 == 0) {
                log.info("Movies progress: page {}/{}, collected={}, skipped={}",
                        page, Math.min(maxPages, response.totalPages()), totalCollected, totalSkipped);
            }
        }

        log.info("Popular movies done: {} collected, {} skipped", totalCollected, totalSkipped);
    }

    /**
     * Fetches popular TV shows from TMDB page by page and persists each batch.
     * Same batch approach as movies.
     */
    public void collectPopularTvShows() {
        log.info("Collecting popular TV shows (max {} pages)...", maxPages);

        // Collect all TV show IDs first
        List<Long> tvShowIds = new ArrayList<>();
        int skipped = 0;

        for (int page = 1; page <= maxPages; page++) {
            TmdbPagedResponseDto<TmdbTvShowDto> response = dataFetcher.fetchPopularTvShowsAsync(page).join();

            if (response == null || response.results() == null || response.results().isEmpty()) {
                break;
            }

            for (TmdbTvShowDto tvShow : response.results()) {
                if (!itemRepository.existsByTmdbIdAndTmdbType(tvShow.id(), TmdbType.TV)) {
                    tvShowIds.add(tvShow.id());
                } else {
                    skipped++;
                }
            }

            if (page >= response.totalPages()) {
                break;
            }

            if (page % 10 == 0) {
                log.info("TV shows progress: page {}/{}, collected IDs={}, skipped={}",
                        page, Math.min(maxPages, response.totalPages()), tvShowIds.size(), skipped);
            }
        }

        log.info("Fetched {} TV show IDs (skipped {}), now batch fetching details...", tvShowIds.size(), skipped);

        // Batch fetch all TV show details with credits in parallel
        List<TmdbTvShowDetailsDto> tvShowDetails = dataFetcher.fetchTvShowsDetailsBatch(tvShowIds);
        log.info("Fetched details for {} TV shows, now building and persisting...", tvShowDetails.size());

        // Use persistence service
        persistenceService.persistTvShows(tvShowDetails);

        log.info("Popular TV shows done: {} collected, {} skipped", tvShowDetails.size(), skipped);
    }
}

