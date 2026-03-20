package org.tvl.tvlooker.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        List<Review> reviews = reviewService.getAll();
        return ResponseEntity.ok(reviews.stream()
                .map(ReviewMapper::toResponse)
                .toList());
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        Review review = ReviewMapper.fromCreateRequest(request);
        Review created = reviewService.create(review);
        return ResponseEntity.ok(ReviewMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        Review review = reviewService.getById(id);
        return ResponseEntity.ok(ReviewMapper.toResponse(review));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody CreateReviewRequest request){
        Review review = ReviewMapper.fromCreateRequest(request);
        Review updated = reviewService.update(id, review);
        return ResponseEntity.ok(ReviewMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteById(id);
        return ResponseEntity.noContent().build();}
}
