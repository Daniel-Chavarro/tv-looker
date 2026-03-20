package org.tvl.tvlooker.persistence.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.Director;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;

/**
 * Mapper for converting between Director entities and domain models.
 */
@Component
public class DirectorEntityMapper {
    /**
     * Converts a DirectorEntity to a Director domain model.
     *
     * @param entity the JPA entity
     * @return the domain model
     */
    public static Director toDomain(DirectorEntity entity) {
        if (entity == null) return null;
        return Director.builder()
                .id(entity.getId())
                .tmdbId(entity.getTmdbId())
                .name(entity.getName())
                .build();
    }

    /**
     * Converts a Director domain model to a DirectorEntity JPA entity.
     *
     * @param domain the domain model
     * @return the JPA entity
     */
    public static DirectorEntity toEntity(Director domain) {
        if (domain == null) return null;
        return DirectorEntity.builder()
                .id(domain.getId())
                .tmdbId(domain.getTmdbId())
                .name(domain.getName())
                .build();
    }
}
