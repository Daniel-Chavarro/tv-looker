package org.tvl.tvlooker.persistence.tmdb.mapper;

import lombok.extern.slf4j.Slf4j;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.ActorItemEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builds ItemEntity instances from TMDB DTOs using pre-cached entities.
 * <p>
 * This builder pattern eliminates duplication between movie and TV show handling
 * while accepting pre-resolved entity maps (genres, actors, directors) to avoid
 * queries during entity construction.
 * <p>
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
     * Builds an ItemEntity from TMDB movie or TV details with pre-cached entities.
     *
     * @param details       Movie details DTO (includes appended credits)
     * @param genreCache    Map of tmdbId → GenreEntity (pre-loaded) of the genres of the item.
     * @param actorCache    Map of tmdbId → ActorEntity (pre-loaded) of the actors of the item
     * @param directorCache Map of tmdbId → DirectorEntity (pre-loaded) of the directors of the item
     * @return Constructed ItemEntity with all relationships set
     */
    public static ItemEntity buildFromItemDetails(
            TmdbMediaDetails details,
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
        item.setGenres(new HashSet<>(genreCache.values()));

        // 3. Set ActorItems
        buildActorItems(item, details, actorCache);

        // 4. Set directors
        item.setDirectors(new HashSet<>(directorCache.values()));

        return item;
    }


    /**
     * Builds ActorItemEntity join entities for the given item based on TMDB credits.
     *
     * @param item       The ItemEntity to which ActorItemEntities will be attached
     * @param details    TMDB media details containing credits information
     * @param actorCache Map of tmdbId → ActorEntity for resolving actors
     */
    public static void buildActorItems(
            ItemEntity item,
            TmdbMediaDetails details,
            Map<Long, ActorEntity> actorCache) {
        Set<ActorItemEntity> actorItems = new HashSet<>();
        if (details.credits() != null && details.credits().cast() != null) {
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
        }
        item.setActorItems(actorItems);

    }

    /**
     * Parses a date string in ISO format (yyyy-MM-dd) to LocalDate.
     *
     * @param date the date string to parse
     * @return the parsed LocalDate, or null if the input is null, blank, or invalid
     */
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

