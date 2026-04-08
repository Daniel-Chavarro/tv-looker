package org.tvl.tvlooker.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaDetails;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbItemBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared service containing common TMDB item persistence and mapping operations.
 *
 * <p>This service extracts duplicated logic from {@link TmdbDataCollectorService} and
 * {@link TmdbDataSynchronizerService} for:</p>
 * <ul>
 *   <li>Persisting movies and TV shows with their genres, actors, and directors</li>
 *   <li>Mapping TMDB DTOs to domain entities</li>
 *   <li>Rate limiting API calls via throttling</li>
 * </ul>
 *
 * <p>All persistence methods are transactional to ensure data consistency.</p>
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-15
 */
@Service
@Slf4j
public class TmdbItemPersistenceService {

    private final ItemRepository itemRepository;
    private final EntityCacheService entityCacheService;
    private final TmdbDataFetcher fetcher;

    public TmdbItemPersistenceService(
            ItemRepository itemRepository,
            EntityCacheService entityCacheService,
            TmdbDataFetcher fetcher) {
        this.itemRepository = itemRepository;
        this.entityCacheService = entityCacheService;
        this.fetcher = fetcher;
    }


    // ===================== PERSISTENCE METHODS =====================

    /**
     * Persists a batch of movies or TV shows with their associated genres, actors, and directors.
     *
     * @param details List of TMDB media details DTOs (must include appended credits)
     */
    @Transactional
    @Async("tmdbTaskExecutor")
    public <T extends TmdbMediaDetails> void persistItems(List<T> details) {
        if (details == null || details.isEmpty()) {
            return;
        }

        for (TmdbMediaDetails itemDetails : details) {
            Map<Long, GenreEntity> genreCache = new HashMap<>();
            Map<Long, ActorEntity> actorCache = new HashMap<>();
            Map<Long, DirectorEntity> directorCache = new HashMap<>();

            if (itemDetails.genres() != null) {
                genreCache = entityCacheService.findOrCreateGenres(itemDetails.genres());
            }

            if (itemDetails.credits() != null) {
                if (itemDetails.credits().cast() != null) {
                    actorCache = entityCacheService.findOrCreateActors(itemDetails.credits().cast());
                }
                if (itemDetails.credits().crew() != null) {
                    directorCache = entityCacheService.findOrCreateDirectors(itemDetails.credits().crew());
                }
            }

            // Can be persisted batches in one call after created the items instead of one call per item
            ItemEntity persisted = itemRepository.save(TmdbItemBuilder
                    .buildFromItemDetails(itemDetails, genreCache, actorCache, directorCache));

            log.debug("Persisted movie (tmdbId={}) with BD ID={}", persisted.getTmdbId(), persisted.getId());
        }

    }

    /**
     * Persists genres from TMDB genre list.
     *
     * @param genreList TMDB genre list DTO containing genres for movies and TV shows
     */
    @Transactional
    @Async("tmdbTaskExecutor")
    public void persistGenres(TmdbGenreListDto genreList) {
        if (genreList == null || genreList.genres() == null) {
            log.warn("No genres to persist");
            return;
        }

        entityCacheService.findOrCreateGenres(genreList.genres());

        log.info("Persisted genres");
    }

    /**
     * Discovers new movies from a list of TMDB movie DTOs, fetches their details, and persists them.
     *
     * @param movies List of TMDB movie DTOs to check for new entries
     * @return number of new movies discovered and persisted
     */
    @Transactional
    public int discoverAndPersistNewMovies(List<TmdbMovieDto> movies) {
        if (movies == null || movies.isEmpty()) {
            return 0;
        }

        List<TmdbMovieDto> newMovies = movies.stream()
                .filter(movie -> !itemRepository.existsByTmdbIdAndTmdbType(movie.id(), TmdbType.MOVIE))
                .toList();

        if (newMovies.isEmpty()) {
            return 0;
        }

        List<Long> ids = newMovies.stream().map(TmdbMovieDto::id).toList();
        List<TmdbMovieDetailsDto> details = fetcher.fetchMoviesDetailsBatch(ids);

        persistItems(details);

        return details.size();
    }

    /**
     * Discovers new TV shows from a list of TMDB TV show DTOs, fetches their details, and persists them.
     *
     * @param tvShows List of TMDB TV show DTOs to check for new entries
     * @return number of new TV shows discovered and persisted
     */
    @Transactional
    public int discoverAndPersistNewTvShows(List<TmdbTvShowDto> tvShows) {
        if (tvShows == null || tvShows.isEmpty()) {
            return 0;
        }

        List<TmdbTvShowDto> newTvShows = tvShows.stream()
                .filter(tvShow -> !itemRepository.existsByTmdbIdAndTmdbType(tvShow.id(), TmdbType.TV))
                .toList();

        if (newTvShows.isEmpty()) {
            return 0;
        }

        List<Long> ids = newTvShows.stream().map(TmdbTvShowDto::id).toList();
        List<TmdbTvShowDetailsDto> details = fetcher.fetchTvShowsDetailsBatch(ids);

        persistItems(details);

        return details.size();
    }

    /**
     * Updates an existing item with fresh details from TMDB, including genres, actors, and directors.
     *
     * @param item    existing item entity to update (must already exist in database)
     * @param details fresh details from TMDB (must include genres and credits)
     */
    @Transactional
    @Async("tmdbTaskExecutor")
    public void updateItem(ItemEntity item, TmdbMediaDetails details) {
        if (details.genres() == null || details.credits() == null) {
            log.warn("Skipping item update (tmdbId={}) due to missing genres or credits", item.getTmdbId());
            return;
        }

        Map<Long, GenreEntity> genreCache = entityCacheService.findOrCreateGenres(details.genres());
        Map<Long, ActorEntity> actorCache = entityCacheService.findOrCreateActors(details.credits().cast());
        Map<Long, DirectorEntity> directorCache = entityCacheService.findOrCreateDirectors(details.credits().crew());

        TmdbItemBuilder.buildActorItems(item, details, actorCache);
        item.setGenres(Set.copyOf(genreCache.values()));
        item.setDirectors(Set.copyOf(directorCache.values()));

        itemRepository.save(item);
        log.debug("Updated item (tmdbId={})", item.getTmdbId());
    }
}

