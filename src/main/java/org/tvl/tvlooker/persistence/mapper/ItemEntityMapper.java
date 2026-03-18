package org.tvl.tvlooker.persistence.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.Item;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ItemEntityMapper {

    public static Item toDomain(ItemEntity entity) {
        if (entity == null) return null;
        return Item.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .overview(entity.getOverview())
                .releaseDate(entity.getReleaseDate())
                .popularity(entity.getPopularity())
                .voteAverage(entity.getVoteAverage())
                .tmdbType(entity.getTmdbType())
                .tmdbId(entity.getTmdbId())
                .genres(entity.getGenres() != null ? entity.getGenres().stream().map(GenreEntityMapper::toDomain).collect(Collectors.toSet()) : null)
                .directors(entity.getDirectors() != null ? entity.getDirectors().stream().map(DirectorEntityMapper::toDomain).collect(Collectors.toSet()) : null)
                .actors(entity.getActors() != null ? entity.getActors().stream().map(ActorEntityMapper::toDomain).collect(Collectors.toSet()) : null)
                .build();
    }

    // Note: We only map the user ID to avoid loading the entire UserEntity, which can be expensive.
    public static ItemEntity toEntity(Item domain) {
        if (domain == null) return null;
        return ItemEntity.builder()
                .id(domain.getId())
                .title(domain.getTitle())
                .overview(domain.getOverview())
                .releaseDate(domain.getReleaseDate())
                .popularity(domain.getPopularity())
                .voteAverage(domain.getVoteAverage())
                .tmdbType(domain.getTmdbType())
                .tmdbId(domain.getTmdbId())
                .genres(domain.getGenres() != null ? domain.getGenres().stream().map(GenreEntityMapper::toEntity).collect(Collectors.toSet()) : null)
                .directors(domain.getDirectors() != null ? domain.getDirectors().stream().map(DirectorEntityMapper::toEntity).collect(Collectors.toSet()) : null)
                .actors(domain.getActors() != null ? domain.getActors().stream().map(ActorEntityMapper::toEntity).collect(Collectors.toSet()) : null)
                .build();
    }
}
