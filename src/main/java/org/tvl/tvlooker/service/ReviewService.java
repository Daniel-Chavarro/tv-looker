package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.ReviewNotFoundException;
import org.tvl.tvlooker.domain.model.entity.Review;
import org.tvl.tvlooker.persistence.repository.ReviewRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Review entity operations.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;

    /**
     * Create a new review.
     *
     * @param review review to persist
     * @return saved review
     */
    public Review create(Review review) {
        return reviewRepository.save(review);
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
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + id));
    }

    /**
     * Get all reviews.
     *
     * @return list of reviews
     */
    public List<Review> getAll() {
        return reviewRepository.findAll();
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
        review.setId(id);
        return reviewRepository.save(review);
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
        return reviewRepository.findByUserIdAndItemId(userId, itemId);
    }
}
