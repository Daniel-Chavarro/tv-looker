package org.tvl.tvlooker.persistence.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.Genre;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;

@Component
public class GenreEntityMapper {
    public static Genre toDomain(GenreEntity entity) {
        if (entity == null) return null;
        return Genre.builder()
                .id(entity.getId())
                .tmdbId(entity.getTmdbId())
                .name(entity.getName())
                .build();
    }

    public static GenreEntity toEntity(Genre domain) {
        if (domain == null) return null;
        return GenreEntity.builder()
                .id(domain.getId())
                .tmdbId(domain.getTmdbId())
                .name(domain.getName())
                .build();
    }
}
