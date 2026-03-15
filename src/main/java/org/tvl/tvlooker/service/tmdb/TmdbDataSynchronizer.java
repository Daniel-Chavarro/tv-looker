package org.tvl.tvlooker.service.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.Actor;
import org.tvl.tvlooker.domain.model.entity.Director;
import org.tvl.tvlooker.domain.model.entity.Genre;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.infrastructure.tmdb.TmdbClient;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.infrastructure.tmdb.mapper.TmdbGenreMapper;
import org.tvl.tvlooker.infrastructure.tmdb.mapper.TmdbItemMapper;
import org.tvl.tvlooker.infrastructure.tmdb.mapper.TmdbPersonMapper;
import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;
import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Periodically synchronizes TMDB data with the local database.
 *
 * <p>Sync Strategy:</p>
 * <ol>
 *   <li>Uses TMDB's /changes endpoints to discover items that changed since last sync</li>
 *   <li>Re-fetches details + credits for changed items that exist in our DB</li>
 *   <li>Fetches first pages of popular movies/TV to discover new items</li>
 * </ol>
 *
 * <p>Non-blocking guarantee: each item is updated in its own transaction (millisecond locks),
 * so users are never affected by the sync process.</p>
 *
 * <p>Activated only when {@code tmdb.sync.enabled=true} (default).</p>
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-10
 */
@Service
@ConditionalOnProperty(name = "tmdb.sync.enabled", havingValue = "true", matchIfMissing = true)
public class TmdbDataSynchronizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbDataSynchronizer.class);

    private final TmdbClient tmdbClient;
    private final ItemRepository itemRepository;
    private final GenreRepository genreRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;

    @Value("${tmdb.collector.request-delay-ms:40}")
    private long requestDelayMs;

    @Value("${tmdb.sync.popular-pages:5}")
    private int popularPages;

    /** Maximum number of actors to store per item (top billed). */
    private static final int MAX_ACTORS_PER_ITEM = 10;

    /** Tracks when the last successful sync completed. */
    private LocalDate lastSyncDate = LocalDate.now().minusDays(1);

    public TmdbDataSynchronizer(
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

    /**
     * Main scheduled sync method.
     * Default: runs every 24 hours, first run 60 seconds after startup.
     */
    @Scheduled(
            fixedDelayString = "${tmdb.sync.interval-ms:86400000}",
            initialDelayString = "${tmdb.sync.initial-delay-ms:60000}")
    public void synchronize() {
        LOGGER.info("========== TMDB SYNC STARTED (changes since {}) ==========", lastSyncDate);

        LocalDate today = LocalDate.now();

        try {
            int updatedMovies = syncChanges(TmdbType.MOVIE, lastSyncDate, today);
            int updatedTvShows = syncChanges(TmdbType.TV, lastSyncDate, today);
            int newMovies = discoverNewPopularItems(TmdbType.MOVIE);
            int newTvShows = discoverNewPopularItems(TmdbType.TV);

            lastSyncDate = today;

            LOGGER.info("========== TMDB SYNC COMPLETED ==========");
            LOGGER.info("Updated: {} movies, {} TV shows | New: {} movies, {} TV shows",
                    updatedMovies, updatedTvShows, newMovies, newTvShows);
        } catch (Exception e) {
            LOGGER.error("TMDB sync failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Uses TMDB /changes endpoint to find and update items that changed since last sync.
     * Only updates items that already exist in our database.
     *
     * @param type      MOVIE or TV
     * @param startDate start of change window
     * @param endDate   end of change window
     * @return number of items updated
     */
    private int syncChanges(TmdbType type, LocalDate startDate, LocalDate endDate) {
        LOGGER.info("Syncing {} changes from {} to {}", type, startDate, endDate);

        int updatedCount = 0;
        int page = 1;
        int totalPages = 1;

        while (page <= totalPages) {
            TmdbPagedResponseDto<TmdbChangesDto> changes;

            if (type == TmdbType.MOVIE) {
                changes = tmdbClient.getMovieChanges(startDate, endDate, page);
            } else {
                changes = tmdbClient.getTvShowChanges(startDate, endDate, page);
            }
            throttle();

            if (changes == null || changes.results() == null) {
                break;
            }

            totalPages = changes.totalPages();

            for (TmdbChangesDto change : changes.results()) {
                try {
                    Optional<Item> existing = itemRepository.findByTmdbIdAndTmdbType(
                            change.id(), type);

                    if (existing.isPresent()) {
                        updateExistingItem(existing.get(), type);
                        updatedCount++;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to sync {} (tmdbId={}): {}",
                            type, change.id(), e.getMessage());
                }
            }

            page++;
        }

        LOGGER.info("Synced {} {} updates", updatedCount, type);
        return updatedCount;
    }

    /**
     * Fetches the first N pages of popular movies/TV to discover new items
     * that are not yet in our database.
     *
     * @param type MOVIE or TV
     * @return number of new items added
     */
    private int discoverNewPopularItems(TmdbType type) {
        LOGGER.info("Discovering new popular {} (first {} pages)", type, popularPages);

        int newCount = 0;

        for (int page = 1; page <= popularPages; page++) {
            try {
                if (type == TmdbType.MOVIE) {
                    newCount += discoverNewMovies(page);
                } else {
                    newCount += discoverNewTvShows(page);
                }
            } catch (Exception e) {
                LOGGER.warn("Error discovering new {} at page {}: {}", type, page, e.getMessage());
            }
        }

        return newCount;
    }

    private int discoverNewMovies(int page) {
        int count = 0;
        TmdbPagedResponseDto<TmdbMovieDto> response = tmdbClient.getPopularMovies(page);
        throttle();

        if (response != null && response.results() != null) {
            for (TmdbMovieDto movie : response.results()) {
                if (!itemRepository.existsByTmdbIdAndTmdbType(movie.id(), TmdbType.MOVIE)) {
                    try {
                        persistNewMovie(movie);
                        count++;
                    } catch (Exception e) {
                        LOGGER.warn("Failed to add new movie '{}': {}", movie.title(), e.getMessage());
                    }
                }
            }
        }
        return count;
    }

    private int discoverNewTvShows(int page) {
        int count = 0;
        TmdbPagedResponseDto<TmdbTvShowDto> response = tmdbClient.getPopularTvShows(page);
        throttle();

        if (response != null && response.results() != null) {
            for (TmdbTvShowDto tvShow : response.results()) {
                if (!itemRepository.existsByTmdbIdAndTmdbType(tvShow.id(), TmdbType.TV)) {
                    try {
                        persistNewTvShow(tvShow);
                        count++;
                    } catch (Exception e) {
                        LOGGER.warn("Failed to add new TV show '{}': {}", tvShow.name(), e.getMessage());
                    }
                }
            }
        }
        return count;
    }

    // ===================== UPDATE / PERSIST HELPERS =====================

    /**
     * Re-fetches details and credits from TMDB and updates an existing item.
     */
    @Transactional
    protected void updateExistingItem(Item item, TmdbType type) {
        if (type == TmdbType.MOVIE) {
            TmdbMovieDto details = tmdbClient.getMovieDetails(item.getTmdbId());
            throttle();
            if (details != null) {
                TmdbItemMapper.updateFromMovie(item, details);
                if (details.genres() != null) {
                    item.setGenres(mapGenres(details.genres()));
                }
            }

            TmdbCreditsDto credits = tmdbClient.getMovieCredits(item.getTmdbId());
            throttle();
            if (credits != null) {
                item.setActors(mapActors(credits));
                item.setDirectors(mapDirectors(credits));
            }
        } else {
            TmdbTvShowDto details = tmdbClient.getTvShowDetails(item.getTmdbId());
            throttle();
            if (details != null) {
                TmdbItemMapper.updateFromTvShow(item, details);
                if (details.genres() != null) {
                    item.setGenres(mapGenres(details.genres()));
                }
            }

            TmdbCreditsDto credits = tmdbClient.getTvShowCredits(item.getTmdbId());
            throttle();
            if (credits != null) {
                item.setActors(mapActors(credits));
                item.setDirectors(mapDirectors(credits));
            }
        }

        itemRepository.save(item);
        LOGGER.debug("Updated {} '{}' (tmdbId={})", type, item.getTitle(), item.getTmdbId());
    }

    @Transactional
    protected void persistNewMovie(TmdbMovieDto movieDto) {
        Item item = TmdbItemMapper.fromMovie(movieDto);

        TmdbMovieDto details = tmdbClient.getMovieDetails(movieDto.id());
        throttle();
        if (details != null && details.genres() != null) {
            item.setGenres(mapGenres(details.genres()));
        }

        TmdbCreditsDto credits = tmdbClient.getMovieCredits(movieDto.id());
        throttle();
        if (credits != null) {
            item.setActors(mapActors(credits));
            item.setDirectors(mapDirectors(credits));
        }

        itemRepository.save(item);
        LOGGER.debug("Added new movie: '{}' (tmdbId={})", movieDto.title(), movieDto.id());
    }

    @Transactional
    protected void persistNewTvShow(TmdbTvShowDto tvDto) {
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
        LOGGER.debug("Added new TV show: '{}' (tmdbId={})", tvDto.name(), tvDto.id());
    }

    // ===================== MAPPING HELPERS =====================

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

