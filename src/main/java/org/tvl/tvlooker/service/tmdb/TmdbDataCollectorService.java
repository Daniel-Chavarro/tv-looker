package org.tvl.tvlooker.service.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbGenreMapper;

import java.util.HashSet;
import java.util.Set;

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
public class TmdbDataCollectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbDataCollectorService.class);

    private final TmdbClient tmdbClient;
    private final ItemRepository itemRepository;
    private final GenreRepository genreRepository;
    private final TmdbItemPersistenceService persistenceService;

    @Value("${tmdb.collector.max-pages:50}")
    private int maxPages;

    public TmdbDataCollectorService(
            TmdbClient tmdbClient,
            ItemRepository itemRepository,
            GenreRepository genreRepository,
            TmdbItemPersistenceService persistenceService) {
        this.tmdbClient = tmdbClient;
        this.itemRepository = itemRepository;
        this.genreRepository = genreRepository;
        this.persistenceService = persistenceService;
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
        LOGGER.info("Collecting genres...");

        TmdbGenreListDto movieGenres = tmdbClient.getMovieGenres();
        persistenceService.throttle();
        TmdbGenreListDto tvGenres = tmdbClient.getTvGenres();
        persistenceService.throttle();

        Set<Integer> seen = new HashSet<>();
        int count = 0;

        count += persistGenreList(movieGenres, seen);
        count += persistGenreList(tvGenres, seen);

        LOGGER.info("Genres collected: {} total", count);
    }

    /**
     * Fetches popular movies from TMDB page by page and persists each one.
     */
    public void collectPopularMovies() {
        LOGGER.info("Collecting popular movies (max {} pages)...", maxPages);

        int collected = 0;
        int skipped = 0;

        for (int page = 1; page <= maxPages; page++) {
            TmdbPagedResponseDto<TmdbMovieDto> response = tmdbClient.getPopularMovies(page);
            persistenceService.throttle();

            if (response == null || response.results() == null || response.results().isEmpty()) {
                break;
            }

            for (TmdbMovieDto movie : response.results()) {
                try {
                    if (itemRepository.existsByTmdbIdAndTmdbType(movie.id(), TmdbType.MOVIE)) {
                        skipped++;
                        continue;
                    }
                    persistenceService.persistMovie(movie);
                    collected++;
                } catch (Exception e) {
                    LOGGER.warn("Failed to persist movie '{}' (tmdbId={}): {}",
                            movie.title(), movie.id(), e.getMessage());
                }
            }

            if (page >= response.totalPages()) {
                break;
            }

            if (page % 10 == 0) {
                LOGGER.info("Movies progress: page {}/{}, collected={}, skipped={}",
                        page, Math.min(maxPages, response.totalPages()), collected, skipped);
            }
        }

        LOGGER.info("Popular movies done: {} collected, {} skipped", collected, skipped);
    }

    /**
     * Fetches popular TV shows from TMDB page by page and persists each one.
     */
    public void collectPopularTvShows() {
        LOGGER.info("Collecting popular TV shows (max {} pages)...", maxPages);

        int collected = 0;
        int skipped = 0;

        for (int page = 1; page <= maxPages; page++) {
            TmdbPagedResponseDto<TmdbTvShowDto> response = tmdbClient.getPopularTvShows(page);
            persistenceService.throttle();

            if (response == null || response.results() == null || response.results().isEmpty()) {
                break;
            }

            for (TmdbTvShowDto tvShow : response.results()) {
                try {
                    if (itemRepository.existsByTmdbIdAndTmdbType(tvShow.id(), TmdbType.TV)) {
                        skipped++;
                        continue;
                    }
                    persistenceService.persistTvShow(tvShow);
                    collected++;
                } catch (Exception e) {
                    LOGGER.warn("Failed to persist TV show '{}' (tmdbId={}): {}",
                            tvShow.name(), tvShow.id(), e.getMessage());
                }
            }

            if (page >= response.totalPages()) {
                break;
            }

            if (page % 10 == 0) {
                LOGGER.info("TV shows progress: page {}/{}, collected={}, skipped={}",
                        page, Math.min(maxPages, response.totalPages()), collected, skipped);
            }
        }

        LOGGER.info("Popular TV shows done: {} collected, {} skipped", collected, skipped);
    }

    // ===================== PRIVATE HELPERS =====================

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

