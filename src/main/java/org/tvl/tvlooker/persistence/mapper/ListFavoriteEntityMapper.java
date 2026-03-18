package org.tvl.tvlooker.persistence.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.ListFavorite;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.ListFavoriteEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ListFavoriteEntityMapper {

    public static ListFavorite toDomain(ListFavoriteEntity entity) {
        if (entity == null) return null;
        return ListFavorite.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .items(entity.getItems() != null ?
                        entity.getItems()
                                .stream()
                                .map(ItemEntityMapper::toDomain)
                                .collect(Collectors.toSet())
                        : null)
                .name(entity.getName())
                .build();
    }

    // Note: We only map the user ID to avoid loading the entire UserEntity, which can be expensive.
    public static ListFavoriteEntity toEntity(ListFavorite domain) {
        if (domain == null) return null;
        return ListFavoriteEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .user(domain.getUserId() != null ? UserEntity.builder().id(domain.getUserId()).build() : null)
                .items(domain.getItems() != null ?
                        domain.getItems()
                                .stream()
                                .map(ItemEntityMapper::toEntity)
                                .collect(Collectors.toSet())
                        : null)
                .build();
    }
}
