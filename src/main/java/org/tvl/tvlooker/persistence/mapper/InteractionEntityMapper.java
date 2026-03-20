package org.tvl.tvlooker.persistence.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.Interaction;
import org.tvl.tvlooker.domain.model.Item;
import org.tvl.tvlooker.domain.model.Review;
import org.tvl.tvlooker.domain.model.User;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.model.entity.InteractionEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;

/**
 * Mapper for converting between Interaction entities and domain models.
 */
@Component
public class InteractionEntityMapper {

    /**
     * Converts an InteractionEntity to an Interaction domain model.
     *
     * @param entity the JPA entity
     * @return the domain model
     */
    public static Interaction toDomain(InteractionEntity entity) {
        if (entity == null) return null;
        return Interaction.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .itemId(entity.getItem() != null ? entity.getItem().getId() : null)
                .reviewId(entity.getReview() != null ? entity.getReview().getId() : null)
                .interactionType(entity.getInteractionType() != null ? entity.getInteractionType() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Converts an Interaction domain model and associated entities to an InteractionEntity JPA entity.
     *
     * @param domain the domain model
     * @param user the associated user model
     * @param item the associated item model
     * @param review the associated review model
     * @return the JPA entity
     */
    public static InteractionEntity toEntity(Interaction domain, User user, Item item, Review review) {
        if (domain == null) return null;
        return InteractionEntity.builder()
                .user(user != null ? UserEntityMapper.toEntity(user) : null)
                .item(item != null ? ItemEntityMapper.toEntity(item) : null)
                .interactionType(domain.getInteractionType() != null ? domain.getInteractionType() : null)
                .review(review != null ? ReviewEntityMapper.toEntity(review, user, item) : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
