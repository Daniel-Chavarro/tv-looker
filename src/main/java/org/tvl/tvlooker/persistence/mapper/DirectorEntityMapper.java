package org.tvl.tvlooker.persistence.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.Director;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;

@Component
public class DirectorEntityMapper {
    public static Director toDomain(DirectorEntity entity) {
        if (entity == null) return null;
        return Director.builder()
                .id(entity.getId())
                .tmdbId(entity.getTmdbId())
                .name(entity.getName())
                .build();
    }

    public static DirectorEntity toEntity(Director domain) {
        if (domain == null) return null;
        return DirectorEntity.builder()
                .id(domain.getId())
                .tmdbId(domain.getTmdbId())
                .name(domain.getName())
                .build();
    }
}
