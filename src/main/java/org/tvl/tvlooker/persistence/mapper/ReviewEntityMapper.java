package org.tvl.tvlooker.persistence.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.Item;
import org.tvl.tvlooker.domain.model.Review;
import org.tvl.tvlooker.domain.model.User;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.ReviewEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;

/**
 * Mapper for converting between Review entities and domain models.
 */
@Component
public class ReviewEntityMapper {

    /**
     * Converts a ReviewEntity to a Review domain model.
     *
     * @param entity the JPA entity
     * @return the domain model
     */
    public static Review toDomain(ReviewEntity entity) {
        if (entity == null) return null;
        return Review.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .itemId(entity.getItem() != null ? entity.getItem().getId() : null)
                .score(entity.getScore())
                .reviewText(entity.getReviewText())
                .reviewDate(entity.getReviewDate())
                .build();
    }

    /**
     * Converts a Review domain model and associated entities to a ReviewEntity JPA entity.
     *
     * @param domain the domain model
     * @param user the associated user model
     * @param item the associated item model
     * @return the JPA entity
     */
    public static ReviewEntity toEntity(Review domain, User user, Item item) {
        if (domain == null) return null;
        return ReviewEntity.builder()
                .id(domain.getId())
                .user(user != null ? UserEntityMapper.toEntity(user) : null)
                .item(item != null ? ItemEntityMapper.toEntity(item) : null)
                .score(domain.getScore())
                .reviewText(domain.getReviewText())
                .reviewDate(domain.getReviewDate())
                .build();

    }
}
