package org.tvl.tvlooker.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.TmdbCollectionInProgressException;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final TmdbDataFetcher dataFetcher;
    private final TmdbItemPersistenceService persistenceService;

    /**
     * Prevents concurrent collection operations
     */
    private final AtomicBoolean collectionInProgress = new AtomicBoolean(false);

    @Value("${tmdb.collector.max-pages:50}")
    private int maxPages;

    @Value("${tmdb.collector.page-window-size:10}")
    private int pageWindowSize;

    @Value("${tmdb.persistence.batch-size:50}")
    private int batchSize;

    public TmdbDataCollectorService(
            ItemRepository itemRepository,
            TmdbDataFetcher dataFetcher,
            TmdbItemPersistenceService persistenceService) {
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
    @Async("tmdbOrchestrationExecutor")
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
    @Async("tmdbOrchestrationExecutor")
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
    @Async("tmdbOrchestrationExecutor")
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
     * :
     * 1. Fetch the first page to get total pages
     * 2. Persist the first page
     * 3. Fetch remaining pages asynchronously in parallel
     * 4. Persist each page as it completes
     * 5. Wait for all pages to complete
     */
    public void collectPopularMovies() {
        log.info("Collecting popular movies (max {} pages)...", maxPages);

        AtomicInteger totalCollected = new AtomicInteger(0);
        AtomicInteger totalSkipped = new AtomicInteger(0);

        // Fetch first page to get total pages
        TmdbPagedResponseDto<TmdbMovieDto> firstPageResponse = dataFetcher.fetchPopularMoviesAsync(1).join();
        if (firstPageResponse == null || firstPageResponse.results() == null || firstPageResponse.results().isEmpty()) {
            log.warn("No movies found on the first page");
            return;}


        // Save first page
        int collected = persistenceService.discoverAndPersistNewMovies(firstPageResponse.results());

        totalCollected.addAndGet(collected);
        totalSkipped.addAndGet(firstPageResponse.results().size() - collected);

        int pagesToFetch = Math.min(firstPageResponse.totalPages(), maxPages);

        if (pagesToFetch <= 1){
            return;
        }

        log.info("Pipelining movies pages from 2 to {}...", pagesToFetch);

        for (int windowStart = 2; windowStart <= pagesToFetch; windowStart += effectivePageWindowSize()) {
            int windowEnd = Math.min(windowStart + effectivePageWindowSize() - 1, pagesToFetch);
            List<CompletableFuture<Void>> pageTasks = new ArrayList<>();

            for (int page = windowStart; page <= windowEnd; page++) {
                int currentPage = page;
                CompletableFuture<Void> pageTask = dataFetcher.fetchPopularMoviesAsync(currentPage)
                        .thenAccept(response -> {
                            if (response != null && response.results() != null && !response.results().isEmpty()) {
                                int collectedResult = persistenceService.discoverAndPersistNewMovies(response.results());
                                totalCollected.addAndGet(collectedResult);
                                totalSkipped.addAndGet(response.results().size() - collectedResult);
                                log.info("Movies progress: page {}/{}, collected={}, skipped={}",
                                        currentPage, pagesToFetch, totalCollected.get(), totalSkipped.get());
                            } else {
                                log.warn("No movies found on page {}", currentPage);
                            }
                        })
                        .exceptionally(ex -> {
                            log.error("Error fetching or persisting movies for page {}: {}", currentPage, ex.getMessage());
                            return null;
                        });
                pageTasks.add(pageTask);
            }

            CompletableFuture.allOf(pageTasks.toArray(new CompletableFuture[0])).join();
        }

        log.info("Popular movies collected, collected={}, skipped={}", totalCollected.get(), totalSkipped.get());
    }

    /**
     * Fetches popular TV shows from TMDB page by page and persists each batch.
     * <p>
     * CONCURRENT APPROACH (same as movies):
     * 1. Fetch the first page to get total pages
     * 2. Persist the first page
     * 3. Fetch remaining pages asynchronously in parallel
     * 4. Persist each page as it completes
     * 5. Wait for all pages to complete
     */
    public void collectPopularTvShows() {
        log.info("Collecting popular TV shows (max {} pages)...", maxPages);

        AtomicInteger totalCollected = new AtomicInteger(0);
        AtomicInteger totalSkipped = new AtomicInteger(0);

        // Fetch first page to get total pages
        TmdbPagedResponseDto<TmdbTvShowDto> firstPageResponse = dataFetcher.fetchPopularTvShowsAsync(1).join();
        if (firstPageResponse == null || firstPageResponse.results() == null || firstPageResponse.results().isEmpty()) {
            log.warn("No TV shows found on the first page");
            return;
        }

        // Save first page
        int collected = persistenceService.discoverAndPersistNewTvShows(firstPageResponse.results());
        totalCollected.addAndGet(collected);
        totalSkipped.addAndGet(firstPageResponse.results().size() - collected);

        int pagesToFetch = Math.min(firstPageResponse.totalPages(), maxPages);

        if (pagesToFetch <= 1) {
            log.info("Popular TV shows done: {} collected, {} skipped", totalCollected.get(), totalSkipped.get());
            return;
        }

        log.info("Pipelining TV shows pages from 2 to {}...", pagesToFetch);

        for (int windowStart = 2; windowStart <= pagesToFetch; windowStart += effectivePageWindowSize()) {
            int windowEnd = Math.min(windowStart + effectivePageWindowSize() - 1, pagesToFetch);
            List<CompletableFuture<Void>> pageTasks = new ArrayList<>();

            for (int page = windowStart; page <= windowEnd; page++) {
                int currentPage = page;
                CompletableFuture<Void> pageTask = dataFetcher.fetchPopularTvShowsAsync(currentPage)
                        .thenAccept(response -> {
                            if (response != null && response.results() != null && !response.results().isEmpty()) {
                                int collectedResult = persistenceService.discoverAndPersistNewTvShows(response.results());
                                totalCollected.addAndGet(collectedResult);
                                totalSkipped.addAndGet(response.results().size() - collectedResult);
                                log.info("TV shows progress: page {}/{}, collected={}, skipped={}",
                                        currentPage, pagesToFetch, totalCollected.get(), totalSkipped.get());
                            } else {
                                log.warn("No TV shows found on page {}", currentPage);
                            }
                        })
                        .exceptionally(ex -> {
                            log.error("Error fetching or persisting TV shows for page {}: {}",
                                    currentPage, ex.getMessage());
                            return null;
                        });
                pageTasks.add(pageTask);
            }

            CompletableFuture.allOf(pageTasks.toArray(new CompletableFuture[0])).join();
        }

        log.info("Popular TV shows collected, collected={}, skipped={}", totalCollected.get(), totalSkipped.get());
    }

    private int effectivePageWindowSize() {
        return Math.max(1, pageWindowSize);
    }
}

