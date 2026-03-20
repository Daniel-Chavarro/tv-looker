package org.tvl.tvlooker.api.dto.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.api.dto.request.CreateReviewRequest;
import org.tvl.tvlooker.api.dto.request.UpdateReviewRequest;
import org.tvl.tvlooker.api.dto.response.ReviewResponse;
import org.tvl.tvlooker.domain.model.Review;

@Component
public class ReviewMapper {

    public static Review toModel(ReviewResponse response){
        return Review.builder()
                .id(response.getId())
                .userId(response.getUserId())
                .itemId(response.getItemId())
                .score(response.getRating())
                .reviewText(response.getComment())
                .reviewDate(response.getCreatedAt())
                .build();
    }

    public static ReviewResponse toResponse(Review review){
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .itemId(review.getItemId())
                .rating(review.getScore())
                .comment(review.getReviewText())
                .createdAt(review.getReviewDate())
                .build();
    }

    public static Review fromCreateRequest(CreateReviewRequest request){
        return Review.builder()
                .itemId(request.getItemId())
                .userId(request.getUserId())
                .score(request.getRating())
                .reviewText(request.getComment())
                .build();
    }

    public static Review fromUpdateRequest(UpdateReviewRequest request){
        return Review.builder()
                .score(request.getScore())
                .reviewText(request.getReviewText())
                .build();
    }
}
