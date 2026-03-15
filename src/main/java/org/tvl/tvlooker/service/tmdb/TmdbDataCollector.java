package org.tvl.tvlooker.service.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.Actor;
import org.tvl.tvlooker.domain.model.entity.Director;
import org.tvl.tvlooker.domain.model.entity.Genre;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;
import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbGenreMapper;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbItemMapper;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbPersonMapper;

import java.util.HashSet;
import java.util.List;
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
@ConditionalOnProperty(name = "tmdb.collector.run-on-startup", havingValue = "true")
public class TmdbDataCollector implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbDataCollector.class);

    private final TmdbClient tmdbClient;
    private final ItemRepository itemRepository;
    private final GenreRepository genreRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;

    @Value("${tmdb.collector.max-pages:50}")
    private int maxPages;

    @Value("${tmdb.collector.request-delay-ms:40}")
    private long requestDelayMs;

    /** Maximum number of actors to store per item (top billed). */
    private static final int MAX_ACTORS_PER_ITEM = 10;

    public TmdbDataCollector(
            TmdbClient tmdbClient,
            ItemRepository itemRepository,
            GenreRepository genreRepository,
            ActorRepository actorRepository,
            DirectorRepository directorRepository) {
        this.tmdbClient = tmdbClient;
        this.itemRepository = itemRepository;
        this.genreRepository = genreRepository;
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        LOGGER.info("========== TMDB DATA COLLECTION STARTED ==========");
        collectAll();
        LOGGER.info("========== TMDB DATA COLLECTION FINISHED ==========");
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
        throttle();
        TmdbGenreListDto tvGenres = tmdbClient.getTvGenres();
        throttle();

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
            throttle();

            if (response == null || response.results() == null || response.results().isEmpty()) {
                break;
            }

            for (TmdbMovieDto movie : response.results()) {
                try {
                    if (itemRepository.existsByTmdbIdAndTmdbType(movie.id(), TmdbType.MOVIE)) {
                        skipped++;
                        continue;
                    }
                    persistMovie(movie);
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
            throttle();

            if (response == null || response.results() == null || response.results().isEmpty()) {
                break;
            }

            for (TmdbTvShowDto tvShow : response.results()) {
                try {
                    if (itemRepository.existsByTmdbIdAndTmdbType(tvShow.id(), TmdbType.TV)) {
                        skipped++;
                        continue;
                    }
                    persistTvShow(tvShow);
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

    @Transactional
    protected void persistMovie(TmdbMovieDto movieDto) {
        Item item = TmdbItemMapper.fromMovie(movieDto);

        // Fetch full details for genre objects
        TmdbMovieDto details = tmdbClient.getMovieDetails(movieDto.id());
        throttle();
        if (details != null && details.genres() != null) {
            item.setGenres(mapGenres(details.genres()));
        }

        // Fetch credits for actors and directors
        TmdbCreditsDto credits = tmdbClient.getMovieCredits(movieDto.id());
        throttle();
        if (credits != null) {
            item.setActors(mapActors(credits));
            item.setDirectors(mapDirectors(credits));
        }

        itemRepository.save(item);
        LOGGER.debug("Persisted movie: '{}' (tmdbId={})", movieDto.title(), movieDto.id());
    }

    @Transactional
    protected void persistTvShow(TmdbTvShowDto tvDto) {
        Item item = TmdbItemMapper.fromTvShow(tvDto);

        TmdbTvShowDto details = tmdbClient.getTvShowDetails(tvDto.id());
        throttle();
        if (details != null && details.genres() != null) {
            item.setGenres(mapGenres(details.genres()));
        }

        TmdbCreditsDto credits = tmdbClient.getTvShowCredits(tvDto.id());
        throttle();
        if (credits != null) {
            item.setActors(mapActors(credits));
            item.setDirectors(mapDirectors(credits));
        }

        itemRepository.save(item);
        LOGGER.debug("Persisted TV show: '{}' (tmdbId={})", tvDto.name(), tvDto.id());
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

    private Set<Genre> mapGenres(List<TmdbGenreDto> genreDtos) {
        Set<Genre> genres = new HashSet<>();
        for (TmdbGenreDto dto : genreDtos) {
            genres.add(TmdbGenreMapper.findOrCreate(dto, genreRepository));
        }
        return genres;
    }

    private Set<Actor> mapActors(TmdbCreditsDto credits) {
        Set<Actor> actors = new HashSet<>();
        if (credits.cast() != null) {
            credits.cast().stream()
                    .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                    .limit(MAX_ACTORS_PER_ITEM)
                    .forEach(c -> actors.add(
                            TmdbPersonMapper.findOrCreateActor(c, actorRepository)));
        }
        return actors;
    }

    private Set<Director> mapDirectors(TmdbCreditsDto credits) {
        Set<Director> directors = new HashSet<>();
        if (credits.crew() != null) {
            credits.crew().stream()
                    .filter(c -> "Director".equalsIgnoreCase(c.job()))
                    .forEach(c -> directors.add(
                            TmdbPersonMapper.findOrCreateDirector(c, directorRepository)));
        }
        return directors;
    }

    private void throttle() {
        try {
            Thread.sleep(requestDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Throttle interrupted");
        }
    }
}

