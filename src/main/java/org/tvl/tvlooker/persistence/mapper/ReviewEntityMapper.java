package org.tvl.tvlooker.persistence.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.Review;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.ReviewEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;

@Component
public class ReviewEntityMapper {

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

    // Note: We only map the user ID and item ID to avoid loading the entire UserEntity and ItemEntity, which can be expensive.
    public static ReviewEntity toEntity(Review domain) {
        if (domain == null) return null;
        return ReviewEntity.builder()
                .id(domain.getId())
                .reviewText(domain.getReviewText())
                .score(domain.getScore() != null ? domain.getScore() : 0)
                .reviewDate(domain.getReviewDate())
                .user(domain.getUserId() != null ? UserEntity.builder().id(domain.getUserId()).build() : null)
                .item(domain.getItemId() != null ? ItemEntity.builder().id(domain.getItemId()).build() : null)
                .build();
    }
}
