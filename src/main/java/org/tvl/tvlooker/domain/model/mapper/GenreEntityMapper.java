package org.tvl.tvlooker.domain.model.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.Genre;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;

/**
 * Mapper for converting between Genre entities and domain models.
 */
@Component
public class GenreEntityMapper {
    /**
     * Converts a GenreEntity to a Genre domain model.
     *
     * @param entity the JPA entity
     * @return the domain model
     */
    public static Genre toDomain(GenreEntity entity) {
        if (entity == null) {return null;}
        return Genre.builder()
                .id(entity.getId())
                .tmdbId(entity.getTmdbId())
                .name(entity.getName())
                .build();
    }

    /**
     * Converts a Genre domain model to a GenreEntity JPA entity.
     *
     * @param domain the domain model
     * @return the JPA entity
     */
    public static GenreEntity toEntity(Genre domain) {
        if (domain == null) {return null;}
        return GenreEntity.builder()
                .id(domain.getId())
                .tmdbId(domain.getTmdbId())
                .name(domain.getName())
                .build();
    }
}
