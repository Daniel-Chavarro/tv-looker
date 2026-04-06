package org.tvl.tvlooker.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaDetails;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbItemBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    //TODO: Refactor to use a single method persist for items (same logic)

    /**
     * Persists a batch of movies with their associated genres, actors, and directors.
     *
     * @param movieDetails List of TMDB movie details DTOs (must include appended credits)
     */
    @Transactional
    public void persistMovies(List<TmdbMovieDetailsDto> movieDetails) {
        if (movieDetails == null || movieDetails.isEmpty()) {
            return;
        }

        Set<TmdbGenreDto> allGenres = new HashSet<>();
        List<TmdbCreditsDto.CastMember> allCast = new ArrayList<>();
        List<TmdbCreditsDto.CrewMember> allCrew = new ArrayList<>();

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

        Map<Long, GenreEntity> genreCache = entityCacheService.findOrCreateGenres(new ArrayList<>(allGenres));
        Map<Long, ActorEntity> actorCache = entityCacheService.findOrCreateActors(allCast);
        Map<Long, DirectorEntity> directorCache = entityCacheService.findOrCreateDirectors(allCrew);

        List<ItemEntity> items = movieDetails.stream()
                .map(details -> TmdbItemBuilder.buildFromMovieDetails(details, genreCache, actorCache, directorCache))
                .collect(Collectors.toList());

        itemRepository.saveAll(items);
        log.info("Persisted {} movies in batch", items.size());
    }

    /**
     * Persists a batch of TV shows with their associated genres, actors, and directors.
     *
     * @param tvShowDetails List of TMDB TV show details DTOs (must include appended credits)
     */
    @Transactional
    public void persistTvShows(List<TmdbTvShowDetailsDto> tvShowDetails) {
        if (tvShowDetails == null || tvShowDetails.isEmpty()) {
            return;
        }

        Set<TmdbGenreDto> allGenres = new HashSet<>();
        List<TmdbCreditsDto.CastMember> allCast = new ArrayList<>();
        List<TmdbCreditsDto.CrewMember> allCrew = new ArrayList<>();

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

        Map<Long, GenreEntity> genreCache = entityCacheService.findOrCreateGenres(new ArrayList<>(allGenres));
        Map<Long, ActorEntity> actorCache = entityCacheService.findOrCreateActors(allCast);
        Map<Long, DirectorEntity> directorCache = entityCacheService.findOrCreateDirectors(allCrew);

        List<ItemEntity> items = tvShowDetails.stream()
                .map(details -> TmdbItemBuilder.buildFromTvShowDetails(details, genreCache, actorCache, directorCache))
                .collect(Collectors.toList());

        itemRepository.saveAll(items);
        log.info("Persisted {} TV shows in batch", items.size());
    }

    /**
     * Persists genres from TMDB genre list.
     *
     * @param genreList TMDB genre list DTO containing genres for movies and TV shows
     */
    @Transactional
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

        persistMovies(details);

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

        persistTvShows(details);

        return details.size();
    }

    /**
     * Updates an existing item with fresh details from TMDB, including genres, actors, and directors.
     *
     * @param item    existing item entity to update (must already exist in database)
     * @param details fresh details from TMDB (must include genres and credits)
     */
    @Transactional
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

