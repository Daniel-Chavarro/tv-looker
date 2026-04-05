package org.tvl.tvlooker.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.exception.TmdbCollectionInProgressException;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbGenreMapper;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbItemBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    private final TmdbClient tmdbClient;
    private final ItemRepository itemRepository;
    private final GenreRepository genreRepository;
    private final TmdbDataFetcher dataFetcher;
    private final EntityCacheService entityCacheService;

    /** Prevents concurrent collection operations */
    private final AtomicBoolean collectionInProgress = new AtomicBoolean(false);

    @Value("${tmdb.collector.max-pages:50}")
    private int maxPages;

    @Value("${tmdb.persistence.batch-size:50}")
    private int batchSize;

    public TmdbDataCollectorService(
            TmdbClient tmdbClient,
            ItemRepository itemRepository,
            GenreRepository genreRepository,
            TmdbDataFetcher dataFetcher,
            EntityCacheService entityCacheService) {
        this.tmdbClient = tmdbClient;
        this.itemRepository = itemRepository;
        this.genreRepository = genreRepository;
        this.dataFetcher = dataFetcher;
        this.entityCacheService = entityCacheService;
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

        TmdbGenreListDto movieGenres = tmdbClient.getGenres(TmdbMediaType.MOVIE);
        TmdbGenreListDto tvGenres = tmdbClient.getGenres(TmdbMediaType.TV);

        Set<Integer> seen = new HashSet<>();
        int count = 0;

        count += persistGenreList(movieGenres, seen);
        count += persistGenreList(tvGenres, seen);

        log.info("Genres collected: {} total", count);
    }

    /**
     * Fetches popular movies from TMDB page by page and persists each batch.
     *
     * NEW APPROACH:
     * 1. Fetch all movie IDs from pages sequentially
     * 2. Batch fetch all movie details with credits in parallel
     * 3. Bulk cache all entities (actors, directors, genres)
     * 4. Build items with pre-cached entities
     * 5. Save batch transactionally
     */
    public void collectPopularMovies() {
        log.info("Collecting popular movies (max {} pages)...", maxPages);

        // Collect all movie IDs first
        List<Long> movieIds = new ArrayList<>();
        int skipped = 0;

        for (int page = 1; page <= maxPages; page++) {
            TmdbPagedResponseDto<TmdbMovieDto> response = tmdbClient.getPopular(TmdbMediaType.MOVIE, page);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                break;
            }

            for (TmdbMovieDto movie : response.results()) {
                if (!itemRepository.existsByTmdbIdAndTmdbType(movie.id(), TmdbType.MOVIE)) {
                    movieIds.add(movie.id());
                } else {
                    skipped++;
                }
            }

            if (page >= response.totalPages()) {
                break;
            }

            if (page % 10 == 0) {
                log.info("Movies progress: page {}/{}, collected IDs={}, skipped={}",
                        page, Math.min(maxPages, response.totalPages()), movieIds.size(), skipped);
            }
        }

        log.info("Fetched {} movie IDs (skipped {}), now batch fetching details...", movieIds.size(), skipped);

        // Batch fetch all movie details with credits in parallel
        List<TmdbMovieDetailsDto> movieDetails = dataFetcher.fetchMoviesBatch(movieIds);
        log.info("Fetched details for {} movies, now building and persisting...", movieDetails.size());

        // Batch persist in chunks
        persistMoviesBatch(movieDetails);

        log.info("Popular movies done: {} collected, {} skipped", movieDetails.size(), skipped);
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
            TmdbPagedResponseDto<TmdbTvShowDto> response = tmdbClient.getPopular(TmdbMediaType.TV, page);

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
        List<TmdbTvShowDetailsDto> tvShowDetails = dataFetcher.fetchTvShowsBatch(tvShowIds);
        log.info("Fetched details for {} TV shows, now building and persisting...", tvShowDetails.size());

        // Batch persist in chunks
        persistTvShowsBatch(tvShowDetails);

        log.info("Popular TV shows done: {} collected, {} skipped", tvShowDetails.size(), skipped);
    }

    // ===================== PRIVATE HELPERS =====================

    /**
     * Persists a batch of movie details.
     *
     * Strategy:
     * 1. Extract all unique entities (actors, directors, genres) from batch
     * 2. Bulk cache them (find or create)
     * 3. Build ItemEntities using pre-cached entities
     * 4. Save all items in one transaction
     *
     * @param movieDetails List of TmdbMovieDetailsDto to persist
     */
    @Transactional
    private void persistMoviesBatch(List<TmdbMovieDetailsDto> movieDetails) {
        if (movieDetails.isEmpty()) {
            return;
        }

        // Collect all unique entities
        Set<TmdbGenreDto> allGenres = new HashSet<>();
        List<org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto.CastMember> allCast = new ArrayList<>();
        List<org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto.CrewMember> allCrew = new ArrayList<>();

        for (TmdbMovieDetailsDto movie : movieDetails) {
            if (movie.genres() != null) {
                allGenres.addAll(movie.genres());
            }
            if (movie.credits() != null) {
                if (movie.credits().cast() != null) {
                    allCast.addAll(movie.credits().cast());
                }
                if (movie.credits().crew() != null) {
                    allCrew.addAll(movie.credits().crew());
                }
            }
        }

        // Bulk cache all entities
        Map<Long, GenreEntity> genreCache = entityCacheService.findOrCreateGenres(new ArrayList<>(allGenres));
        Map<Long, ActorEntity> actorCache = entityCacheService.findOrCreateActors(allCast);
        Map<Long, DirectorEntity> directorCache = entityCacheService.findOrCreateDirectors(allCrew);

        // Build all items with pre-cached entities
        List<ItemEntity> items = movieDetails.stream()
                .map(details -> TmdbItemBuilder.buildFromMovieDetails(
                        details, genreCache, actorCache, directorCache))
                .collect(Collectors.toList());

        // Save all in one transaction
        itemRepository.saveAll(items);
        log.info("Persisted {} movies in batch", items.size());
    }

    /**
     * Persists a batch of TV show details.
     * Same pattern as persistMoviesBatch.
     *
     * @param tvShowDetails List of TmdbTvShowDetailsDto to persist
     */
    @Transactional
    private void persistTvShowsBatch(List<TmdbTvShowDetailsDto> tvShowDetails) {
        if (tvShowDetails.isEmpty()) {
            return;
        }

        // Collect all unique entities
        Set<TmdbGenreDto> allGenres = new HashSet<>();
        List<org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto.CastMember> allCast = new ArrayList<>();
        List<org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto.CrewMember> allCrew = new ArrayList<>();

        for (TmdbTvShowDetailsDto tvShow : tvShowDetails) {
            if (tvShow.genres() != null) {
                allGenres.addAll(tvShow.genres());
            }
            if (tvShow.credits() != null) {
                if (tvShow.credits().cast() != null) {
                    allCast.addAll(tvShow.credits().cast());
                }
                if (tvShow.credits().crew() != null) {
                    allCrew.addAll(tvShow.credits().crew());
                }
            }
        }

        // Bulk cache all entities
        Map<Long, GenreEntity> genreCache = entityCacheService.findOrCreateGenres(new ArrayList<>(allGenres));
        Map<Long, ActorEntity> actorCache = entityCacheService.findOrCreateActors(allCast);
        Map<Long, DirectorEntity> directorCache = entityCacheService.findOrCreateDirectors(allCrew);

        // Build all items with pre-cached entities
        List<ItemEntity> items = tvShowDetails.stream()
                .map(details -> TmdbItemBuilder.buildFromTvShowDetails(
                        details, genreCache, actorCache, directorCache))
                .collect(Collectors.toList());

        // Save all in one transaction
        itemRepository.saveAll(items);
        log.info("Persisted {} TV shows in batch", items.size());
    }

    private int persistGenreList(TmdbGenreListDto genreList, Set<Integer> seen) {
        int count = 0;
        if (genreList != null && genreList.genres() != null) {
            for (TmdbGenreDto dto : genreList.genres()) {
                if (seen.add(dto.id())) {
                    TmdbGenreMapper.findOrCreate(dto, genreRepository);
                    count++;
                }
            }
        }
        return count;
    }
}

