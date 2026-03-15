package org.tvl.tvlooker.service.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.Actor;
import org.tvl.tvlooker.domain.model.entity.Director;
import org.tvl.tvlooker.domain.model.entity.Genre;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;
import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbGenreMapper;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbItemMapper;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbPersonMapper;

import java.util.HashSet;
import java.util.List;
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
public class TmdbItemPersistenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbItemPersistenceService.class);

    private final TmdbClient tmdbClient;
    private final ItemRepository itemRepository;
    private final GenreRepository genreRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;

    @Value("${tmdb.collector.request-delay-ms:40}")
    private long requestDelayMs;

    /** Maximum number of actors to store per item (top billed). */
    private static final int MAX_ACTORS_PER_ITEM = 10;

    public TmdbItemPersistenceService(
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

    // ===================== PERSISTENCE METHODS =====================

    /**
     * Persists a new movie with its genres, actors, and directors.
     *
     * @param movieDto the TMDB movie data transfer object
     */
    @Transactional
    public void persistMovie(TmdbMovieDto movieDto) {
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

    /**
     * Persists a new TV show with its genres, actors, and directors.
     *
     * @param tvDto the TMDB TV show data transfer object
     */
    @Transactional
    public void persistTvShow(TmdbTvShowDto tvDto) {
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

    // ===================== MAPPING METHODS =====================

    /**
     * Maps a list of TMDB genre DTOs to Genre entities.
     * Uses find-or-create pattern to avoid duplicates.
     *
     * @param genreDtos list of TMDB genre DTOs
     * @return set of Genre entities
     */
    public Set<Genre> mapGenres(List<TmdbGenreDto> genreDtos) {
        Set<Genre> genres = new HashSet<>();
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
    public Set<Actor> mapActors(TmdbCreditsDto credits) {
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

    /**
     * Maps TMDB crew members to Director entities.
     * Only crew members with job="Director" are included.
     *
     * @param credits TMDB credits containing crew information
     * @return set of Director entities
     */
    public Set<Director> mapDirectors(TmdbCreditsDto credits) {
        Set<Director> directors = new HashSet<>();
        if (credits.crew() != null) {
            credits.crew().stream()
                    .filter(c -> "Director".equalsIgnoreCase(c.job()))
                    .forEach(c -> directors.add(
                            TmdbPersonMapper.findOrCreateDirector(c, directorRepository)));
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
            LOGGER.warn("Throttle interrupted");
        }
    }
}
