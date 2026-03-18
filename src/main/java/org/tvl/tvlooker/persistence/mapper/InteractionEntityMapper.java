package org.tvl.tvlooker.persistence.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.Interaction;
import org.tvl.tvlooker.domain.model.Review;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.model.entity.InteractionEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;

@Component
public class InteractionEntityMapper {

    public static Interaction toDomain(InteractionEntity entity) {
        if (entity == null) return null;
        return Interaction.builder()
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .itemId(entity.getItem() != null ? entity.getItem().getId() : null)
                .interactionType(entity.getInteractionType() != null ? entity.getInteractionType() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // Note: We only map the user ID to avoid loading the entire UserEntity, which can be expensive.
    public static InteractionEntity toEntity(Interaction domain, Review review) {
        if (domain == null) return null;
        return InteractionEntity.builder()
                .user(domain.getUserId() != null ? UserEntity.builder().id(domain.getUserId()).build() : null)
                .item(domain.getItemId() != null ? ItemEntity.builder().id(domain.getItemId()).build() : null)
                .interactionType(domain.getInteractionType() != null ? domain.getInteractionType() : null)
                .review(review != null ? ReviewEntityMapper.toEntity(review) : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
