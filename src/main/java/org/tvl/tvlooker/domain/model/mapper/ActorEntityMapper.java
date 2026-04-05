package org.tvl.tvlooker.domain.model.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.dto.Actor;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;

/**
 * Mapper for converting between Actor entities and domain models.
 */
@Component
public class ActorEntityMapper {
    /**
     * Converts an ActorEntity to an Actor domain model.
     *
     * @param entity the JPA entity
     * @return the domain model
     */
    public static Actor toDomain(ActorEntity entity) {
        if (entity == null) {return null;}
        return Actor.builder()
                .id(entity.getId())
                .tmdbId(entity.getTmdbId())
                .name(entity.getName())
                .build();
    }

    /**
     * Converts an Actor domain model to an ActorEntity JPA entity.
     *
     * @param domain the domain model
     * @return the JPA entity
     */
    public static ActorEntity toEntity(Actor domain) {
        if (domain == null) {return null;}        return ActorEntity.builder()
                .id(domain.getId())
                .tmdbId(domain.getTmdbId())
                .name(domain.getName())
                .build();
    }
}
