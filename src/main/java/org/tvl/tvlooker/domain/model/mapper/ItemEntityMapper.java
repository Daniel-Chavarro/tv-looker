package org.tvl.tvlooker.domain.model.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.Item;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;

import java.util.stream.Collectors;

/**
 * Mapper for converting between Item entities and domain models.
 */
@Component
@RequiredArgsConstructor
public class    ItemEntityMapper {

    /**
     * Converts an ItemEntity to an Item domain model.
     *
     * @param entity the JPA entity
     * @return the domain model
     */
    public static Item toDomain(ItemEntity entity) {
        if (entity == null) {return null;}
        return Item.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .overview(entity.getOverview())
                .releaseDate(entity.getReleaseDate())
                .popularity(entity.getPopularity())
                .voteAverage(entity.getVoteAverage())
                .tmdbType(entity.getTmdbType())
                .tmdbId(entity.getTmdbId())
                .genres(entity.getGenres() != null
                        ? entity.getGenres().stream()
                        .map(GenreEntityMapper::toDomain)
                        .collect(Collectors.toSet())
                        : null)
                .directors(entity.getDirectors() != null
                        ? entity.getDirectors().stream()
                        .map(DirectorEntityMapper::toDomain)
                        .collect(Collectors.toSet()) :
                        null)
                .actors(entity.getActorItems() != null
                        ? entity.getActorItems().stream()
                        .map(ai -> ActorEntityMapper.toDomain(ai.getActor()))
                        .collect(Collectors.toSet())
                        : null)
                .build();
    }

    // Note: We only map the user ID to avoid loading the entire UserEntity, which can be expensive.
    /**
     * Converts an Item domain model to an ItemEntity JPA entity.
     * NOTE: The 'actors' field is now stored in 'actorItems' join entity and is handled separately.
     *
     * @param domain the domain model
     * @return the JPA entity
     */
    public static ItemEntity toEntity(Item domain) {
        if (domain == null) {return null;}
        return ItemEntity.builder()
                .id(domain.getId())
                .title(domain.getTitle())
                .overview(domain.getOverview())
                .releaseDate(domain.getReleaseDate())
                .popularity(domain.getPopularity())
                .voteAverage(domain.getVoteAverage())
                .tmdbType(domain.getTmdbType())
                .tmdbId(domain.getTmdbId())
                .genres(domain.getGenres() != null
                        ? domain.getGenres().stream()
                        .map(GenreEntityMapper::toEntity)
                        .collect(Collectors.toSet())
                        : null)
                .directors(domain.getDirectors() != null
                        ? domain.getDirectors().stream()
                        .map(DirectorEntityMapper::toEntity)
                        .collect(Collectors.toSet())
                        : null)
                // Note: actors are now in actorItems, not set here
                .build();
    }
}
