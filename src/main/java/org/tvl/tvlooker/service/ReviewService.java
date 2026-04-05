package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.ReviewNotFoundException;
import org.tvl.tvlooker.domain.model.Item;
import org.tvl.tvlooker.domain.model.Review;
import org.tvl.tvlooker.domain.model.User;
import org.tvl.tvlooker.domain.model.entity.ReviewEntity;
import org.tvl.tvlooker.domain.model.mapper.ReviewEntityMapper;
import org.tvl.tvlooker.persistence.repository.ReviewRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Review entity operations.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final ItemService itemService;

    /**
     * Create a new review.
     *
     * @param review review to persist
     * @return saved review
     */
    public Review create(Review review) {
        User user = userService.getById(review.getUserId());
        Item item = itemService.getById(review.getItemId());
        ReviewEntity entity = ReviewEntityMapper.toEntity(review, user, item);
        return ReviewEntityMapper.toDomain(reviewRepository.save(entity));
    }

    /**
     * Get a review by id.
     *
     * @param id review id
     * @return review
     * @throws ReviewNotFoundException when the review does not exist
     */
    public Review getById(Long id) {
        return reviewRepository.findById(id)
                .map(ReviewEntityMapper::toDomain)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + id));
    }

    /**
     * Get all reviews.
     *
     * @return list of reviews
     */
    public List<Review> getAll() {
        return reviewRepository.findAll().stream()
                .map(ReviewEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Update a review.
     *
     * @param id review id
     * @param review review data to update
     * @return updated review
     * @throws ReviewNotFoundException when the review does not exist
     */
    public Review update(Long id, Review review) {
        if (!reviewRepository.existsById(id)) {
            throw new ReviewNotFoundException("Review not found: " + id);
        }

        User user = userService.getById(review.getUserId());
        Item item = itemService.getById(review.getItemId());
        ReviewEntity update = ReviewEntityMapper.toEntity(review, user, item);
        ReviewEntity actual = reviewRepository.getReferenceById(id);

        if (update.getReviewDate() != null) {
            actual.setReviewDate(update.getReviewDate());
        }

        if (update.getReviewText() != null) {
            actual.setReviewText(update.getReviewText());
        }

        if (0 <= update.getScore() && update.getScore() <= 5) {
            actual.setScore(update.getScore());
        }

        return ReviewEntityMapper.toDomain(reviewRepository.save(actual));
    }

    /**
     * Delete a review by id.
     *
     * @param id review id
     * @throws ReviewNotFoundException when the review does not exist
     */
    public void deleteById(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ReviewNotFoundException("Review not found: " + id);
        }
        reviewRepository.deleteById(id);
    }

    /**
     * Find a review by user ID and item ID.
     *
     * @param userId user ID
     * @param itemId item ID
     * @return optional review.
     */
    public Optional<Review> findByUserIdAndItemId(UUID userId, Long itemId) {
        return reviewRepository.findByUserIdAndItemId(userId, itemId)
                .map(ReviewEntityMapper::toDomain);
    }
}
