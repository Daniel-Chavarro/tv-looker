package org.tvl.tvlooker.service.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbItemMapper;

import java.time.LocalDate;
import java.util.Optional;

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
@Profile("!test")
public class TmdbDataSynchronizerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbDataSynchronizerService.class);

    private final TmdbClient tmdbClient;
    private final ItemRepository itemRepository;
    private final TmdbItemPersistenceService persistenceService;

    @Value("${tmdb.sync.popular-pages:5}")
    private int popularPages;

    /** Tracks when the last successful sync completed. */
    private LocalDate lastSyncDate = LocalDate.now().minusDays(1);

    public TmdbDataSynchronizerService(
            TmdbClient tmdbClient,
            ItemRepository itemRepository,
            TmdbItemPersistenceService persistenceService) {
        this.tmdbClient = tmdbClient;
        this.itemRepository = itemRepository;
        this.persistenceService = persistenceService;
    }

    /**
     * Returns the date of the last successful synchronization.
     *
     * @return the last sync date
     */
    public LocalDate getLastSyncDate() {
        return lastSyncDate;
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
            persistenceService.throttle();

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
        persistenceService.throttle();

        if (response != null && response.results() != null) {
            for (TmdbMovieDto movie : response.results()) {
                if (!itemRepository.existsByTmdbIdAndTmdbType(movie.id(), TmdbType.MOVIE)) {
                    try {
                        persistenceService.persistMovie(movie);
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
        persistenceService.throttle();

        if (response != null && response.results() != null) {
            for (TmdbTvShowDto tvShow : response.results()) {
                if (!itemRepository.existsByTmdbIdAndTmdbType(tvShow.id(), TmdbType.TV)) {
                    try {
                        persistenceService.persistTvShow(tvShow);
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
            persistenceService.throttle();
            if (details != null) {
                TmdbItemMapper.updateFromMovie(item, details);
                if (details.genres() != null) {
                    item.setGenres(persistenceService.mapGenres(details.genres()));
                }
            }

            TmdbCreditsDto credits = tmdbClient.getMovieCredits(item.getTmdbId());
            persistenceService.throttle();
            if (credits != null) {
                item.setActors(persistenceService.mapActors(credits));
                item.setDirectors(persistenceService.mapDirectors(credits));
            }
        } else {
            TmdbTvShowDto details = tmdbClient.getTvShowDetails(item.getTmdbId());
            persistenceService.throttle();
            if (details != null) {
                TmdbItemMapper.updateFromTvShow(item, details);
                if (details.genres() != null) {
                    item.setGenres(persistenceService.mapGenres(details.genres()));
                }
            }

            TmdbCreditsDto credits = tmdbClient.getTvShowCredits(item.getTmdbId());
            persistenceService.throttle();
            if (credits != null) {
                item.setActors(persistenceService.mapActors(credits));
                item.setDirectors(persistenceService.mapDirectors(credits));
            }
        }

        itemRepository.save(item);
        LOGGER.debug("Updated {} '{}' (tmdbId={})", type, item.getTitle(), item.getTmdbId());
    }
}

