package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.ReviewNotFoundException;
import org.tvl.tvlooker.domain.model.entity.Review;
import org.tvl.tvlooker.persistence.repository.ReviewRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReviewService.
 * Tests all CRUD operations and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Unit Tests")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewService reviewService;

    private Long testReviewId;
    private Review testReview;

    @BeforeEach
    void setUp() {
        testReviewId = 1L;
        testReview = Review.builder()
                .id(testReviewId)
                .reviewText("Great movie!")
                .score(8)
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("create - should save and return review")
    void create_shouldSaveAndReturnReview() {
        // Arrange
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        // Act
        Review result = reviewService.create(testReview);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testReviewId);
        assertThat(result.getReviewText()).isEqualTo("Great movie!");
        verify(reviewRepository, times(1)).save(testReview);
    }

    // ========== GET BY ID TESTS ==========

    @Test
    @DisplayName("getById - should return review when review exists")
    void getById_shouldReturnReview_whenReviewExists() {
        // Arrange
        when(reviewRepository.findById(testReviewId)).thenReturn(Optional.of(testReview));

        // Act
        Review result = reviewService.getById(testReviewId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testReviewId);
        assertThat(result.getReviewText()).isEqualTo("Great movie!");
        verify(reviewRepository, times(1)).findById(testReviewId);
    }

    @Test
    @DisplayName("getById - should throw ReviewNotFoundException when review does not exist")
    void getById_shouldThrowReviewNotFoundException_whenReviewDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(reviewRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reviewService.getById(nonExistentId))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("Review not found: " + nonExistentId);
        verify(reviewRepository, times(1)).findById(nonExistentId);
    }

    // ========== GET ALL TESTS ==========

    @Test
    @DisplayName("getAll - should return all reviews")
    void getAll_shouldReturnAllReviews() {
        // Arrange
        Review review2 = Review.builder()
                .id(2L)
                .reviewText("Excellent!")
                .score(9)
                .build();
        List<Review> reviews = List.of(testReview, review2);
        when(reviewRepository.findAll()).thenReturn(reviews);

        // Act
        List<Review> result = reviewService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testReview, review2);
        verify(reviewRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no reviews exist")
    void getAll_shouldReturnEmptyList_whenNoReviewsExist() {
        // Arrange
        when(reviewRepository.findAll()).thenReturn(List.of());

        // Act
        List<Review> result = reviewService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(reviewRepository, times(1)).findAll();
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("update - should update and return review when review exists")
    void update_shouldUpdateAndReturnReview_whenReviewExists() {
        // Arrange
        Review updatedReview = Review.builder()
                .reviewText("Updated review")
                .score(10)
                .build();
        Review savedReview = Review.builder()
                .id(testReviewId)
                .reviewText("Updated review")
                .score(10)
                .build();

        when(reviewRepository.existsById(testReviewId)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // Act
        Review result = reviewService.update(testReviewId, updatedReview);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testReviewId);
        assertThat(result.getReviewText()).isEqualTo("Updated review");
        assertThat(result.getScore()).isEqualTo(10);
        verify(reviewRepository, times(1)).existsById(testReviewId);
        verify(reviewRepository, times(1)).save(updatedReview);
    }

    @Test
    @DisplayName("update - should set ID on review before saving")
    void update_shouldSetIdOnReview_beforeSaving() {
        // Arrange
        Review updatedReview = Review.builder()
                .reviewText("Updated review")
                .score(10)
                .build();

        when(reviewRepository.existsById(testReviewId)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(updatedReview);

        // Act
        reviewService.update(testReviewId, updatedReview);

        // Assert
        assertThat(updatedReview.getId()).isEqualTo(testReviewId);
        verify(reviewRepository, times(1)).save(updatedReview);
    }

    @Test
    @DisplayName("update - should throw ReviewNotFoundException when review does not exist")
    void update_shouldThrowReviewNotFoundException_whenReviewDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        Review updatedReview = Review.builder()
                .reviewText("Updated review")
                .score(10)
                .build();

        when(reviewRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.update(nonExistentId, updatedReview))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("Review not found: " + nonExistentId);
        verify(reviewRepository, times(1)).existsById(nonExistentId);
        verify(reviewRepository, never()).save(any());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("deleteById - should delete review when review exists")
    void deleteById_shouldDeleteReview_whenReviewExists() {
        // Arrange
        when(reviewRepository.existsById(testReviewId)).thenReturn(true);
        doNothing().when(reviewRepository).deleteById(testReviewId);

        // Act
        reviewService.deleteById(testReviewId);

        // Assert
        verify(reviewRepository, times(1)).existsById(testReviewId);
        verify(reviewRepository, times(1)).deleteById(testReviewId);
    }

    @Test
    @DisplayName("deleteById - should throw ReviewNotFoundException when review does not exist")
    void deleteById_shouldThrowReviewNotFoundException_whenReviewDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(reviewRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.deleteById(nonExistentId))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("Review not found: " + nonExistentId);
        verify(reviewRepository, times(1)).existsById(nonExistentId);
        verify(reviewRepository, never()).deleteById(any());
    }
}
