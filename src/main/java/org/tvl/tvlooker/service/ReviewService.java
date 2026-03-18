package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.ReviewNotFoundException;
import org.tvl.tvlooker.domain.model.Review;
import org.tvl.tvlooker.persistence.mapper.ReviewEntityMapper;
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
    private final ReviewEntityMapper reviewMapper;

    /**
     * Create a new review.
     *
     * @param review review to persist
     * @return saved review
     */
    public Review create(Review review) {
        var entity = reviewMapper.toEntity(review);
        return reviewMapper.toDomain(reviewRepository.save(entity));
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
                .map(reviewMapper::toDomain)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + id));
    }

    /**
     * Get all reviews.
     *
     * @return list of reviews
     */
    public List<Review> getAll() {
        return reviewRepository.findAll().stream()
                .map(reviewMapper::toDomain)
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
        var entity = reviewMapper.toEntity(review);
        entity.setId(id);
        return reviewMapper.toDomain(reviewRepository.save(entity));
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
                .map(reviewMapper::toDomain);
    }
}
