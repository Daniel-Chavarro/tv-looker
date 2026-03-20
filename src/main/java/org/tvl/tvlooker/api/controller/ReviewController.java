package org.tvl.tvlooker.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.mapper.ReviewMapper;
import org.tvl.tvlooker.api.dto.request.CreateReviewRequest;
import org.tvl.tvlooker.api.dto.response.ReviewResponse;
import org.tvl.tvlooker.domain.model.Review;
import org.tvl.tvlooker.service.ReviewService;

import java.util.List;

/**
 * REST controller for managing reviews.
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    /**
     * Retrieves all reviews.
     * @return a list of review responses
     */
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        List<Review> reviews = reviewService.getAll();
        return ResponseEntity.ok(reviews.stream()
                .map(ReviewMapper::toResponse)
                .toList());
    }

    /**
     * Creates a new review.
     * @param request the request containing review details
     * @return the created review response
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        Review review = ReviewMapper.fromCreateRequest(request);
        Review created = reviewService.create(review);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/v1/users/" + created.getId())
                .body(ReviewMapper.toResponse(created));
    }

    /**
     * Retrieves a review by its ID.
     * @param id the review ID
     * @return the review response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        Review review = reviewService.getById(id);
        return ResponseEntity.ok(ReviewMapper.toResponse(review));
    }

    /**
     * Updates an existing review.
     * @param id the review ID
     * @param request the request containing updated review details
     * @return the updated review response
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody CreateReviewRequest request){
        Review review = ReviewMapper.fromCreateRequest(request);
        Review updated = reviewService.update(id, review);
        return ResponseEntity.ok(ReviewMapper.toResponse(updated));
    }

    /**
     * Deletes a review by its ID.
     * @param id the review ID
     * @return empty response with status 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteById(id);
        return ResponseEntity.noContent().build();}
}
