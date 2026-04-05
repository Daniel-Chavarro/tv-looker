package org.tvl.tvlooker.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;
import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbGenreMapper;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbItemMapper;
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
    private final GenreRepository genreRepository;
    private final EntityCacheService entityCacheService;
    private final TmdbDataFetcher fetcher;

    @Value("${tmdb.collector.request-delay-ms:40}")
    private long requestDelayMs;

    public TmdbItemPersistenceService(
            ItemRepository itemRepository,
            GenreRepository genreRepository,
            EntityCacheService entityCacheService,
            TmdbDataFetcher fetcher) {
        this.itemRepository = itemRepository;
        this.genreRepository = genreRepository;
        this.entityCacheService = entityCacheService;
        this.fetcher = fetcher;
    }



    /**
     * Introduces a delay between API calls to respect TMDB rate limits.
     * Delay duration is configured via {@code tmdb.collector.request-delay-ms}.
     */
    public void throttle() {
        try {
            Thread.sleep(requestDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Throttle interrupted");
        }
    }

    // ===================== PERSISTENCE METHODS =====================

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

    @Transactional
    public void persistGenres(TmdbGenreListDto genreList, Set<Integer> seen) {
        int count = 0;
        if (genreList != null && genreList.genres() != null) {
            for (TmdbGenreDto dto : genreList.genres()) {
                if (seen.add(dto.id())) {
                    TmdbGenreMapper.findOrCreate(dto, genreRepository);
                    count++;
                }
            }
        }
        log.info("Persisted {} genres", count);
    }

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

    @Transactional
    public void updateItem(ItemEntity item, Object details) {
        if (details instanceof TmdbMovieDetailsDto movieDetails) {
            TmdbItemMapper.updateFromMovie(item, movieDetails);
            if (movieDetails.genres() != null && movieDetails.credits() != null) {
                Map<Long, GenreEntity> genreCache = entityCacheService.findOrCreateGenres(movieDetails.genres());
                Map<Long, ActorEntity> actorCache = entityCacheService.findOrCreateActors(movieDetails.credits().cast());
                Map<Long, DirectorEntity> directorCache = entityCacheService.findOrCreateDirectors(movieDetails.credits().crew());

                TmdbItemBuilder.buildActorItems(item, movieDetails.credits(), actorCache);
                item.setGenres(genreCache.values().stream()
                        .filter(g -> movieDetails.genres().stream()
                                .anyMatch(dto -> dto.id() == g.getTmdbId().intValue()))
                        .collect(Collectors.toSet()));
                item.setDirectors(directorCache.values());
            }
        } else if (details instanceof TmdbTvShowDetailsDto tvDetails) {
            TmdbItemMapper.updateFromTvShow(item, tvDetails);
            if (tvDetails.genres() != null && tvDetails.credits() != null) {
                Map<Long, GenreEntity> genreCache = entityCacheService.findOrCreateGenres(tvDetails.genres());
                Map<Long, ActorEntity> actorCache = entityCacheService.findOrCreateActors(tvDetails.credits().cast());
                Map<Long, DirectorEntity> directorCache = entityCacheService.findOrCreateDirectors(tvDetails.credits().crew());

                TmdbItemBuilder.buildActorItems(item, tvDetails.credits(), actorCache);
                item.setGenres(genreCache.values().stream()
                        .filter(g -> tvDetails.genres().stream()
                                .anyMatch(dto -> dto.id() == g.getTmdbId().intValue()))
                        .collect(Collectors.toSet()));
                item.setDirectors(directorCache.values());
            }
        }

        itemRepository.save(item);
        log.debug("Updated item (tmdbId={})", item.getTmdbId());
    }
}
