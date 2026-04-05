package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.ReviewNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Review;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.entity.ReviewEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.persistence.repository.ReviewRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Unit Tests")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserService userService;

    @Mock
    private ItemService itemService;

    @InjectMocks
    private ReviewService reviewService;

    private Long testReviewId;
    private Review testReview;
    private ReviewEntity testReviewEntity;
    private User testUser;
    private Item testItem;
    private UUID testUserId;
    private Long testItemId;

    @BeforeEach
    void setUp() {
        testReviewId = 1L;
        testUserId = UUID.randomUUID();
        testItemId = 1L;

        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@test.com")
                .name("Test User")
                .build();

        testItem = Item.builder()
                .id(testItemId)
                .title("Test Movie")
                .overview("Test overview")
                .build();

        testReview = Review.builder()
                .id(testReviewId)
                .userId(testUserId)
                .itemId(testItemId)
                .reviewText("Great movie!")
                .score(8)
                .build();

        testReviewEntity = ReviewEntity.builder()
                .id(testReviewId)
                .user(UserEntity.builder().id(testUserId).username("testuser").email("test@test.com").name("Test User").build())
                .item(ItemEntity.builder().id(testItemId).title("Test Movie").overview("Test overview").build())
                .reviewText("Great movie!")
                .score(8)
                .build();
    }

    @Test
    @DisplayName("create - should save and return review")
    void create_shouldSaveAndReturnReview() {
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(itemService.getById(testItemId)).thenReturn(testItem);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(testReviewEntity);

        Review result = reviewService.create(testReview);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testReviewId);
        assertThat(result.getReviewText()).isEqualTo("Great movie!");
        verify(reviewRepository, times(1)).save(any(ReviewEntity.class));
    }

    @Test
    @DisplayName("getById - should return review when review exists")
    void getById_shouldReturnReview_whenReviewExists() {
        when(reviewRepository.findById(testReviewId)).thenReturn(Optional.of(testReviewEntity));

        Review result = reviewService.getById(testReviewId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testReviewId);
        assertThat(result.getReviewText()).isEqualTo("Great movie!");
        verify(reviewRepository, times(1)).findById(testReviewId);
    }

    @Test
    @DisplayName("getById - should throw ReviewNotFoundException when review does not exist")
    void getById_shouldThrowReviewNotFoundException_whenReviewDoesNotExist() {
        Long nonExistentId = 999L;
        when(reviewRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getById(nonExistentId))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("Review not found: " + nonExistentId);
        verify(reviewRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAll - should return all reviews")
    void getAll_shouldReturnAllReviews() {
        ReviewEntity review2 = ReviewEntity.builder()
                .id(2L)
                .user(UserEntity.builder().id(UUID.randomUUID()).username("user2").email("user2@test.com").name("User 2").build())
                .item(ItemEntity.builder().id(2L).title("Movie 2").overview("Overview 2").build())
                .reviewText("Excellent!")
                .score(9)
                .build();
        List<ReviewEntity> reviewEntities = List.of(testReviewEntity, review2);
        when(reviewRepository.findAll()).thenReturn(reviewEntities);

        List<Review> result = reviewService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(reviewRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no reviews exist")
    void getAll_shouldReturnEmptyList_whenNoReviewsExist() {
        when(reviewRepository.findAll()).thenReturn(List.of());

        List<Review> result = reviewService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(reviewRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("update - should update and return review when review exists")
    void update_shouldUpdateAndReturnReview_whenReviewExists() {
        Review updatedReview = Review.builder()
                .userId(testUserId)
                .itemId(testItemId)
                .reviewText("Updated review")
                .score(10)
                .build();
        ReviewEntity savedReviewEntity = ReviewEntity.builder()
                .id(testReviewId)
                .user(UserEntity.builder().id(testUserId).username("testuser").email("test@test.com").name("Test User").build())
                .item(ItemEntity.builder().id(testItemId).title("Test Movie").overview("Test overview").build())
                .reviewText("Updated review")
                .score(10)
                .build();

        when(reviewRepository.existsById(testReviewId)).thenReturn(true);
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(itemService.getById(testItemId)).thenReturn(testItem);
        when(reviewRepository.getReferenceById(testReviewId)).thenReturn(testReviewEntity);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(savedReviewEntity);

        Review result = reviewService.update(testReviewId, updatedReview);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testReviewId);
        assertThat(result.getReviewText()).isEqualTo("Updated review");
        assertThat(result.getScore()).isEqualTo(10);
        verify(reviewRepository, times(1)).existsById(testReviewId);
        verify(reviewRepository, times(1)).save(any(ReviewEntity.class));
    }

    @Test
    @DisplayName("update - should set ID on review before saving")
    void update_shouldSetIdOnReview_beforeSaving() {
        Review updatedReview = Review.builder()
                .userId(testUserId)
                .itemId(testItemId)
                .reviewText("Updated review")
                .score(10)
                .build();

        when(reviewRepository.existsById(testReviewId)).thenReturn(true);
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(itemService.getById(testItemId)).thenReturn(testItem);
        when(reviewRepository.getReferenceById(testReviewId)).thenReturn(testReviewEntity);
        when(reviewRepository.save(any(ReviewEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reviewService.update(testReviewId, updatedReview);

        verify(reviewRepository, times(1)).save(any(ReviewEntity.class));
    }

    @Test
    @DisplayName("update - should throw ReviewNotFoundException when review does not exist")
    void update_shouldThrowReviewNotFoundException_whenReviewDoesNotExist() {
        Long nonExistentId = 999L;
        Review updatedReview = Review.builder()
                .userId(testUserId)
                .itemId(testItemId)
                .reviewText("Updated review")
                .score(10)
                .build();

        when(reviewRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.update(nonExistentId, updatedReview))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("Review not found: " + nonExistentId);
        verify(reviewRepository, times(1)).existsById(nonExistentId);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - should delete review when review exists")
    void deleteById_shouldDeleteReview_whenReviewExists() {
        when(reviewRepository.existsById(testReviewId)).thenReturn(true);
        doNothing().when(reviewRepository).deleteById(testReviewId);

        reviewService.deleteById(testReviewId);

        verify(reviewRepository, times(1)).existsById(testReviewId);
        verify(reviewRepository, times(1)).deleteById(testReviewId);
    }

    @Test
    @DisplayName("delete - should throw ReviewNotFoundException when review does not exist")
    void deleteById_shouldThrowReviewNotFoundException_whenReviewDoesNotExist() {
        Long nonExistentId = 999L;
        when(reviewRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.deleteById(nonExistentId))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("Review not found: " + nonExistentId);
        verify(reviewRepository, times(1)).existsById(nonExistentId);
        verify(reviewRepository, never()).deleteById(any());
    }
}
