package org.tvl.tvlooker.persistence.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.Actor;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;

@Component
public class ActorEntityMapper {
    public static Actor toDomain(ActorEntity entity) {
        if (entity == null) return null;
        return Actor.builder()
                .id(entity.getId())
                .tmdbId(entity.getTmdbId())
                .name(entity.getName())
                .build();
    }

    public static ActorEntity toEntity(Actor domain) {
        if (domain == null) return null;
        return ActorEntity.builder()
                .id(domain.getId())
                .tmdbId(domain.getTmdbId())
                .name(domain.getName())
                .build();
    }
}
