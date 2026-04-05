package org.tvl.tvlooker.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.ActorItemEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;
import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbGenreMapper;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbItemMapper;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbCastMemberMapper;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbItemBuilder;
import org.tvl.tvlooker.service.EntityCacheService;

import java.util.ArrayList;
import java.util.Comparator;
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

    private final TmdbClient tmdbClient;
    private final ItemRepository itemRepository;
    private final GenreRepository genreRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final EntityCacheService entityCacheService;
    private final TmdbDataFetcher fetcher;

    @Value("${tmdb.collector.request-delay-ms:40}")
    private long requestDelayMs;

    /** Maximum number of actors to store per item (top billed). */
    private static final int MAX_ACTORS_PER_ITEM = 10;

    public TmdbItemPersistenceService(
            TmdbClient tmdbClient,
            ItemRepository itemRepository,
            GenreRepository genreRepository,
            ActorRepository actorRepository,
            DirectorRepository directorRepository,
            EntityCacheService entityCacheService,
            TmdbDataFetcher fetcher) {
        this.tmdbClient = tmdbClient;
        this.itemRepository = itemRepository;
        this.genreRepository = genreRepository;
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
        this.entityCacheService = entityCacheService;
        this.fetcher = fetcher;
    }

    // ===================== PERSISTENCE METHODS =====================

    /**
     * Persists a new movie with its genres, actors, and directors.
     * WARNING: This uses the OLD single-item persistence approach.
     * New code should use TmdbDataCollectorService.persistMoviesBatch() instead.
     *
     * @param movieDto the TMDB movie data transfer object
     * @deprecated Use persistMoviesBatch() for better performance
     */
    @Transactional
    @Deprecated(forRemoval = true)
    public void persistMovie(TmdbMovieDto movieDto) {
        ItemEntity item = TmdbItemMapper.fromMovie(movieDto);

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
            // Note: OLD approach - now we use actorItems instead of a simple actors set
            // For backward compatibility, we'll create ActorItemEntity objects
            mapActorsToActorItems(item, credits);
            item.setDirectors(mapDirectors(credits));
        }

        itemRepository.save(item);
        log.debug("Persisted movie: '{}' (tmdbId={})", movieDto.title(), movieDto.id());
    }

    /**
     * Persists a new TV show with its genres, actors, and directors.
     * WARNING: This uses the OLD single-item persistence approach.
     * New code should use TmdbDataCollectorService.persistTvShowsBatch() instead.
     *
     * @param tvDto the TMDB TV show data transfer object
     * @deprecated Use persistTvShowsBatch() for better performance
     */
    @Transactional
    @Deprecated(forRemoval = true)
    public void persistTvShow(TmdbTvShowDto tvDto) {
        ItemEntity item = TmdbItemMapper.fromTvShow(tvDto);

        TmdbTvShowDto details = tmdbClient.getTvShowDetails(tvDto.id());
        throttle();
        if (details != null && details.genres() != null) {
            item.setGenres(mapGenres(details.genres()));
        }

        TmdbCreditsDto credits = tmdbClient.getTvShowCredits(tvDto.id());
        throttle();
        if (credits != null) {
            // Note: OLD approach - now we use actorItems instead of a simple actors set
            mapActorsToActorItems(item, credits);
            item.setDirectors(mapDirectors(credits));
        }

        itemRepository.save(item);
        log.debug("Persisted TV show: '{}' (tmdbId={})", tvDto.name(), tvDto.id());
    }

    // ===================== MAPPING METHODS =====================

    /**
     * Maps a list of TMDB genre DTOs to Genre entities.
     * Uses find-or-create pattern to avoid duplicates.
     *
     * @param genreDtos list of TMDB genre DTOs
     * @return set of Genre entities
     */
    public Set<GenreEntity> mapGenres(List<TmdbGenreDto> genreDtos) {
        Set<GenreEntity> genres = new HashSet<>();
        for (TmdbGenreDto dto : genreDtos) {
            genres.add(TmdbGenreMapper.findOrCreate(dto, genreRepository));
        }
        return genres;
    }

    /**
     * Maps TMDB cast members to Actor entities.
     * Only the top {@value MAX_ACTORS_PER_ITEM} actors (by billing order) are included.
     *
     * @param credits TMDB credits containing cast information
     * @return set of Actor entities
     */
    public Set<ActorItemEntity> mapActors(TmdbCreditsDto credits) {
        Set<ActorItemEntity> actors = new HashSet<>();
        if (credits.cast() != null) {
            credits.cast().stream()
                    .sorted(Comparator.comparingInt(TmdbCreditsDto.CastMember::order))
                    .limit(MAX_ACTORS_PER_ITEM)
                    .forEach(c -> actors.add(
                            TmdbCastMemberMapper.findOrCreateActor(c, actorRepository)));
        }
        return actors;
    }

    /**
     * Maps TMDB cast members to ActorItemEntity instances.
     * Creates ActorItemEntity objects with character name and billing order.
     * Only the top {@value MAX_ACTORS_PER_ITEM} actors (by billing order) are included.
     *
     * @param item the ItemEntity to associate actors with
     * @param credits TMDB credits containing cast information
     */
    public void mapActorsToActorItems(ItemEntity item, TmdbCreditsDto credits) {
        if (credits.cast() == null) {
            return;
        }
        Set<ActorItemEntity> actorItems = new HashSet<>();
        credits.cast().stream()
                .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                .limit(MAX_ACTORS_PER_ITEM)
                .forEach(c -> {
                    ActorEntity actor = TmdbCastMemberMapper.findOrCreateActor(c, actorRepository);
                    ActorItemEntity actorItem = ActorItemEntity.builder()
                            .item(item)
                            .actor(actor)
                            .characterName(c.character())
                            .billingOrder(c.order())
                            .build();
                    actorItems.add(actorItem);
                });
        item.setActorItems(actorItems);
    }

    /**
     * Maps TMDB crew members to Director entities.
     * Only crew members with job="Director" are included.
     *
     * @param credits TMDB credits containing crew information
     * @return set of Director entities
     */
    public Set<DirectorEntity> mapDirectors(TmdbCreditsDto credits) {
        Set<DirectorEntity> directors = new HashSet<>();
        if (credits.crew() != null) {
            credits.crew().stream()
                    .filter(c -> "Director".equalsIgnoreCase(c.job()))
                    .forEach(c -> directors.add(
                            TmdbCastMemberMapper.findOrCreateDirector(c, directorRepository)));
        }
        return directors;
    }

    // ===================== UTILITY METHODS =====================

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
            if (movieDetails.genres() != null) {
                item.setGenres(mapGenres(movieDetails.genres()));
            }
            if (movieDetails.credits() != null) {
                item.setActorItems(mapActors(movieDetails.credits()));
                item.setDirectors(mapDirectors(movieDetails.credits()));
            }
        } else if (details instanceof TmdbTvShowDetailsDto tvDetails) {
            TmdbItemMapper.updateFromTvShow(item, tvDetails);
            if (tvDetails.genres() != null) {
                item.setGenres(mapGenres(tvDetails.genres()));
            }
            if (tvDetails.credits() != null) {
                item.setActorItems(mapActors(tvDetails.credits()));
                item.setDirectors(mapDirectors(tvDetails.credits()));
            }
        }

        itemRepository.save(item);
        log.debug("Updated item (tmdbId={})", item.getTmdbId());
    }
}
