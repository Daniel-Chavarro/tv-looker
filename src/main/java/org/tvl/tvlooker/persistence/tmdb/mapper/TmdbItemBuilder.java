package org.tvl.tvlooker.persistence.tmdb.mapper;

import lombok.extern.slf4j.Slf4j;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.ActorItemEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds ItemEntity instances from TMDB DTOs using pre-cached entities.
 *
 * This builder pattern eliminates duplication between movie and TV show handling
 * while accepting pre-resolved entity maps (genres, actors, directors) to avoid
 * queries during entity construction.
 *
 * The builder properly sets up the ActorItemEntity join entities, capturing
 * character names and billing order from the TMDB API.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-04-03
 */
@Slf4j
public final class TmdbItemBuilder {

    private static final int MAX_ACTORS_PER_ITEM = 10;

    private TmdbItemBuilder() {
        // Utility class, no instantiation
    }

    /**
     * Builds an ItemEntity from TMDB movie details with pre-cached entities.
     *
     * @param details Movie details DTO (includes appended credits)
     * @param genreCache Map of tmdbId → GenreEntity (pre-loaded)
     * @param actorCache Map of tmdbId → ActorEntity (pre-loaded)
     * @param directorCache Map of tmdbId → DirectorEntity (pre-loaded)
     * @return Constructed ItemEntity with all relationships set
     */
    public static ItemEntity buildFromMovieDetails(
            TmdbMovieDetailsDto details,
            Map<Long, GenreEntity> genreCache,
            Map<Long, ActorEntity> actorCache,
            Map<Long, DirectorEntity> directorCache) {

        log.debug("Building ItemEntity from movie: '{}'", details.title());

        // 1. Build base item
        ItemEntity item = ItemEntity.builder()
                .tmdbId(details.id())
                .tmdbType(TmdbType.MOVIE)
                .title(details.title())
                .overview(details.overview())
                .releaseDate(parseDate(details.releaseDate()))
                .popularity(BigDecimal.valueOf(details.popularity()))
                .voteAverage(BigDecimal.valueOf(details.voteAverage()))
                .build();

        // 2. Set genres
        if (details.genres() != null && !details.genres().isEmpty()) {
            Set<GenreEntity> genres = details.genres().stream()
                    .map(g -> genreCache.get((long) g.id()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            item.setGenres(genres);
        }

        // 3. Set actor items with character names and billing order (NEW!)
        if (details.credits() != null && details.credits().cast() != null) {
            Set<ActorItemEntity> actorItems = new HashSet<>();

            details.credits().cast().stream()
                    .sorted(Comparator.comparingInt(TmdbCreditsDto.CastMember::order))
                    .limit(MAX_ACTORS_PER_ITEM)
                    .forEach(cast -> {
                        ActorEntity actor = actorCache.get(cast.id());
                        if (actor != null) {
                            ActorItemEntity actorItem = ActorItemEntity.builder()
                                    .item(item)
                                    .actor(actor)
                                    .characterName(cast.character())
                                    .billingOrder(cast.order())
                                    .build();
                            actorItems.add(actorItem);
                        }
                    });

            item.setActorItems(actorItems);
        }

        // 4. Set directors
        if (details.credits() != null && details.credits().crew() != null) {
            Set<DirectorEntity> directors = details.credits().crew().stream()
                    .filter(crew -> "Director".equalsIgnoreCase(crew.job()))
                    .map(crew -> directorCache.get(crew.id()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            item.setDirectors(directors);
        }

        return item;
    }

    /**
     * Builds an ItemEntity from TMDB TV show details with pre-cached entities.
     *
     * @param details TV show details DTO (includes appended credits)
     * @param genreCache Map of tmdbId → GenreEntity (pre-loaded)
     * @param actorCache Map of tmdbId → ActorEntity (pre-loaded)
     * @param directorCache Map of tmdbId → DirectorEntity (pre-loaded)
     * @return Constructed ItemEntity with all relationships set
     */
    public static ItemEntity buildFromTvShowDetails(
            TmdbTvShowDetailsDto details,
            Map<Long, GenreEntity> genreCache,
            Map<Long, ActorEntity> actorCache,
            Map<Long, DirectorEntity> directorCache) {

        log.debug("Building ItemEntity from TV show: '{}'", details.name());

        // 1. Build base item
        ItemEntity item = ItemEntity.builder()
                .tmdbId(details.id())
                .tmdbType(TmdbType.TV)
                .title(details.name())
                .overview(details.overview())
                .releaseDate(parseDate(details.firstAirDate()))
                .popularity(BigDecimal.valueOf(details.popularity()))
                .voteAverage(BigDecimal.valueOf(details.voteAverage()))
                .build();

        // 2. Set genres
        if (details.genres() != null && !details.genres().isEmpty()) {
            Set<GenreEntity> genres = details.genres().stream()
                    .map(g -> genreCache.get((long) g.id()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            item.setGenres(genres);
        }

        // 3. Set actor items
        if (details.credits() != null && details.credits().cast() != null) {
            Set<ActorItemEntity> actorItems = new HashSet<>();

            details.credits().cast().stream()
                    .sorted(Comparator.comparingInt(TmdbCreditsDto.CastMember::order))
                    .limit(MAX_ACTORS_PER_ITEM)
                    .forEach(cast -> {
                        ActorEntity actor = actorCache.get(cast.id());
                        if (actor != null) {
                            ActorItemEntity actorItem = ActorItemEntity.builder()
                                    .item(item)
                                    .actor(actor)
                                    .characterName(cast.character())
                                    .billingOrder(cast.order())
                                    .build();
                            actorItems.add(actorItem);
                        }
                    });

            item.setActorItems(actorItems);
        }

        // 4. Set directors
        if (details.credits() != null && details.credits().crew() != null) {
            Set<DirectorEntity> directors = details.credits().crew().stream()
                    .filter(crew -> "Director".equalsIgnoreCase(crew.job()))
                    .map(crew -> directorCache.get(crew.id()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            item.setDirectors(directors);
        }

        return item;
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date: {}", date);
            return null;
        }
    }
}

