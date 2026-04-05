package org.tvl.tvlooker.service.tmdb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaDetails;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbItemMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
@Slf4j
@Profile("!test")
public class TmdbDataSynchronizerService {

    private final TmdbDataFetcher fetcher;
    private final ItemRepository itemRepository;
    private final TmdbItemPersistenceService persistenceService;

    @Value("${tmdb.sync.popular-pages:5}")
    private int popularPages;
    
    @Getter
    private LocalDate lastSyncDate = LocalDate.now().minusDays(1);

    public TmdbDataSynchronizerService(
            TmdbDataFetcher fetcher,
            ItemRepository itemRepository,
            TmdbItemPersistenceService persistenceService) {
        this.fetcher = fetcher;
        this.itemRepository = itemRepository;
        this.persistenceService = persistenceService;
    }

    /**
     * Main scheduled sync method.
     * Default: runs every 24 hours, first run 60 seconds after startup.
     */
    @ConditionalOnProperty(name = "tmdb.sync.enabled", havingValue = "true", matchIfMissing = true)
    @Scheduled(
            fixedDelayString = "${tmdb.sync.interval-ms:86400000}",
            initialDelayString = "${tmdb.sync.initial-delay-ms:60000}")
    public void synchronize() {
        log.info("========== TMDB SYNC STARTED (changes since {}) ==========", lastSyncDate);

        LocalDate today = LocalDate.now();

        try {
            int updatedMovies = syncChanges(TmdbType.MOVIE, lastSyncDate, today);
            int updatedTvShows = syncChanges(TmdbType.TV, lastSyncDate, today);
            int newMovies = discoverNewPopularItems(TmdbType.MOVIE);
            int newTvShows = discoverNewPopularItems(TmdbType.TV);

            lastSyncDate = today;

            log.info("========== TMDB SYNC COMPLETED ==========");
            log.info("Updated: {} movies, {} TV shows | New: {} movies, {} TV shows",
                    updatedMovies, updatedTvShows, newMovies, newTvShows);
        } catch (Exception e) {
            log.error("TMDB sync failed: {}", e.getMessage(), e);
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
        log.info("Syncing {} changes from {} to {}", type, startDate, endDate);

        int updatedCount = 0;
        int page = 1;
        int totalPages = 1;
        TmdbMediaType mediaType = type == TmdbType.MOVIE ? TmdbMediaType.MOVIE : TmdbMediaType.TV;

        while (page <= totalPages) {
            TmdbPagedResponseDto<TmdbChangesDto> changes = fetcher.fetchChangesAsync(
                    mediaType, startDate, endDate, page).join();
            
            if (changes == null || changes.results() == null) {
                break;
            }

            totalPages = changes.totalPages();

            List<Long> existingIds = changes.results().stream()
                .filter(change -> itemRepository.findByTmdbIdAndTmdbType(change.id(), type).isPresent())
                .map(TmdbChangesDto::id)
                .toList();

            if (!existingIds.isEmpty()) {
                List<CompletableFuture<TmdbMediaDetails>> futures = existingIds.stream()
                    .map(id -> fetcher.fetchDetailsWithCreditsAsync(mediaType, id))
                    .toList();

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                for (int i = 0; i < existingIds.size(); i++) {
                    long tmdbId = existingIds.get(i);
                    try {
                        TmdbMediaDetails details = futures.get(i).join();
                        Optional<ItemEntity> existing = itemRepository.findByTmdbIdAndTmdbType(tmdbId, type);
                        if (existing.isPresent() && details != null) {
                            updateExistingItem(existing.get(), details);
                            updatedCount++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to sync {} (tmdbId={}): {}", type, tmdbId, e.getMessage());
                    }
                }
            }

            page++;
        }

        log.info("Synced {} {} updates", updatedCount, type);
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
        log.info("Discovering new popular {} (first {} pages)", type, popularPages);

        int newCount = 0;

        for (int page = 1; page <= popularPages; page++) {
            try {
                if (type == TmdbType.MOVIE) {
                    newCount += discoverNewMovies(page);
                } else {
                    newCount += discoverNewTvShows(page);
                }
            } catch (Exception e) {
                log.warn("Error discovering new {} at page {}: {}", type, page, e.getMessage());
            }
        }

        return newCount;
    }

    private int discoverNewMovies(int page) {
        int count = 0;
        TmdbPagedResponseDto<TmdbMovieDto> response = fetcher.fetchPopularMoviesAsync(page).join();

        if (response != null && response.results() != null) {
            for (TmdbMovieDto movie : response.results()) {
                if (!itemRepository.existsByTmdbIdAndTmdbType(movie.id(), TmdbType.MOVIE)) {
                    try {
                        persistenceService.persistMovie(movie);
                        count++;
                    } catch (Exception e) {
                        log.warn("Failed to add new movie '{}': {}", movie.title(), e.getMessage());
                    }
                }
            }
        }
        return count;
    }

    private int discoverNewTvShows(int page) {
        int count = 0;
        TmdbPagedResponseDto<TmdbTvShowDto> response = fetcher.fetchPopularTvShowsAsync(page).join();

        if (response != null && response.results() != null) {
            for (TmdbTvShowDto tvShow : response.results()) {
                if (!itemRepository.existsByTmdbIdAndTmdbType(tvShow.id(), TmdbType.TV)) {
                    try {
                        persistenceService.persistTvShow(tvShow);
                        count++;
                    } catch (Exception e) {
                        log.warn("Failed to add new TV show '{}': {}", tvShow.name(), e.getMessage());
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
    protected void updateExistingItem(ItemEntity item, Object details) {
        if (details instanceof TmdbMovieDetailsDto movieDetails) {
            TmdbItemMapper.updateFromMovie(item, movieDetails);
            if (movieDetails.genres() != null) {
                item.setGenres(persistenceService.mapGenres(movieDetails.genres()));
            }
            if (movieDetails.credits() != null) {
                item.setActorItems(persistenceService.mapActors(movieDetails.credits()));
                item.setDirectors(persistenceService.mapDirectors(movieDetails.credits()));
            }
        } else if (details instanceof TmdbTvShowDetailsDto tvDetails) {
            TmdbItemMapper.updateFromTvShow(item, tvDetails);
            if (tvDetails.genres() != null) {
                item.setGenres(persistenceService.mapGenres(tvDetails.genres()));
            }
            if (tvDetails.credits() != null) {
                item.setActorItems(persistenceService.mapActors(tvDetails.credits()));
                item.setDirectors(persistenceService.mapDirectors(tvDetails.credits()));
            }
        }

        itemRepository.save(item);
        log.debug("Updated item '{}' (tmdbId={})", item.getTitle(), item.getTmdbId());
    }
}

