package org.tvl.tvlooker.domain.model.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.dto.ListFavorite;
import org.tvl.tvlooker.domain.model.entity.ListFavoriteEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;

import java.util.stream.Collectors;

/**
 * Mapper for converting between ListFavorite entities and domain models.
 */
@RequiredArgsConstructor
@Component
public class ListFavoriteEntityMapper {

    /**
     * Converts a ListFavoriteEntity to a ListFavorite domain model.
     *
     * @param entity the JPA entity
     * @return the domain model
     */
    public static ListFavorite toDomain(ListFavoriteEntity entity) {
        if (entity == null) {return null;}
        return ListFavorite.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .items(entity.getItems() != null
                        ? entity.getItems()
                                .stream()
                                .map(ItemEntityMapper::toDomain)
                                .collect(Collectors.toSet())
                        : null)
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }

    // Note: We only map the user ID to avoid loading the entire UserEntity, which can be expensive.
    /**
     * Converts a ListFavorite domain model to a ListFavoriteEntity JPA entity.
     *
     * @param domain the domain model
     * @return the JPA entity
     */
    public static ListFavoriteEntity toEntity(ListFavorite domain) {
        if (domain == null) {return null;}
        return ListFavoriteEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .user(domain.getUserId() != null ? UserEntity.builder().id(domain.getUserId()).build() : null)
                .items(domain.getItems() != null
                        ? domain.getItems()
                                .stream()
                                .map(ItemEntityMapper::toEntity)
                                .collect(Collectors.toSet())
                        : null)
                .build();
    }
}
