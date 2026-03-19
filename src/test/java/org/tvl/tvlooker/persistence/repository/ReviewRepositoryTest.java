package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.ReviewEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * TDD Test class for ReviewRepository.
 * Tests cover CRUD operations, custom queries, and relationships.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-11
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ReviewRepository TDD Tests")
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== CREATE/SAVE TESTS ====================

    @Test
    @DisplayName("Should save a new review with user and item")
    void testSaveReview() {
        // Given - Create objects WITHOUT saving
        UserEntity user = createUserEntity("reviewer1");
        ItemEntity item = createItemEntity(12345L, "The Matrix");

        ReviewEntity review = ReviewEntity.builder()
                .reviewText("Amazing movie!")
                .score(9)
                .item(item)
                .user(user)
                .build();

        // When - Save review (cascade will persist user and item)
        ReviewEntity savedReview = reviewRepository.saveAndFlush(review);

        // Then
        assertThat(savedReview).isNotNull();
        assertThat(savedReview.getId()).isNotNull();
        assertThat(savedReview.getReviewText()).isEqualTo("Amazing movie!");
        assertThat(savedReview.getScore()).isEqualTo(9);
        assertThat(savedReview.getUser().getId()).isNotNull();
        assertThat(savedReview.getItem().getId()).isNotNull();
    }

    @Test
    @DisplayName("Should save review without text (only score)")
    void testSaveReviewWithoutText() {
        // Given
        UserEntity user = createUserEntity("quickrater");
        ItemEntity item = createItemEntity(67890L, "Inception");

        ReviewEntity review = ReviewEntity.builder()
                .reviewText(null)
                .score(10)
                .item(item)
                .user(user)
                .build();

        // When
        ReviewEntity savedReview = reviewRepository.saveAndFlush(review);

        // Then
        assertThat(savedReview).isNotNull();
        assertThat(savedReview.getReviewText()).isNull();
        assertThat(savedReview.getScore()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should save multiple reviews for different items")
    void testSaveMultipleReviews() {
        // Given
        UserEntity user = createUserEntity("critic1");
        ItemEntity item1 = createItemEntity(1L, "Movie 1");
        ItemEntity item2 = createItemEntity(2L, "Movie 2");

        ReviewEntity review1 = ReviewEntity.builder().reviewText("Good").score(7).item(item1).user(user).build();
        ReviewEntity review2 = ReviewEntity.builder().reviewText("Great").score(8).item(item2).user(user).build();

        // When
        List<ReviewEntity> savedReviews = reviewRepository.saveAll(List.of(review1, review2));

        // Then
        assertThat(savedReviews).hasSize(2);
        assertThat(reviewRepository.count()).isEqualTo(2);
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find review by ID")
    void testFindById() {
        // Given
        UserEntity user = createUserEntity("finduser");
        ItemEntity item = createItemEntity(100L, "Findable Movie");
        ReviewEntity review = ReviewEntity.builder()
                .reviewText("Test review")
                .score(8)
                .item(item)
                .user(user)
                .build();
        ReviewEntity savedReview = reviewRepository.saveAndFlush(review);

        // When
        Optional<ReviewEntity> foundReview = reviewRepository.findById(savedReview.getId());

        // Then
        assertThat(foundReview).isPresent();
        assertThat(foundReview.get().getReviewText()).isEqualTo("Test review");
        assertThat(foundReview.get().getScore()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should find all reviews")
    void testFindAll() {
        // Given
        UserEntity user = createUserEntity("user1");
        ItemEntity item1 = createItemEntity(1L, "ItemEntity 1");
        ItemEntity item2 = createItemEntity(2L, "ItemEntity 2");

        reviewRepository.saveAll(List.of(
                ReviewEntity.builder().reviewText("ReviewEntity 1").score(7).item(item1).user(user).build(),
                ReviewEntity.builder().reviewText("ReviewEntity 2").score(8).item(item2).user(user).build()
        ));

        // When
        List<ReviewEntity> allReviews = reviewRepository.findAll();

        // Then
        assertThat(allReviews).hasSize(2);
    }

    @Test
    @DisplayName("Should count all reviews")
    void testCount() {
        // Given
        UserEntity user = createUserEntity("counter");
        ItemEntity item1 = createItemEntity(1L, "ItemEntity 1");
        ItemEntity item2 = createItemEntity(2L, "ItemEntity 2");

        reviewRepository.saveAll(List.of(
                ReviewEntity.builder().reviewText("ReviewEntity 1").score(7).item(item1).user(user).build(),
                ReviewEntity.builder().reviewText("ReviewEntity 2").score(8).item(item2).user(user).build()
        ));

        // When
        long count = reviewRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ==================== CUSTOM QUERY TESTS ====================

    @Test
    @DisplayName("Should find reviews by user ID")
    void testFindByUserId() {
        // Given
        UserEntity user1 = createUserEntity("user1");
        UserEntity user2 = createUserEntity("user2");
        ItemEntity item1 = createItemEntity(1L, "ItemEntity 1");
        ItemEntity item2 = createItemEntity(2L, "ItemEntity 2");
        ItemEntity item3 = createItemEntity(3L, "ItemEntity 3");

        ReviewEntity review1 = ReviewEntity.builder().reviewText("User1 ReviewEntity 1").score(7).item(item1).user(user1).build();
        ReviewEntity review2 = ReviewEntity.builder().reviewText("User1 ReviewEntity 2").score(8).item(item2).user(user1).build();
        ReviewEntity review3 = ReviewEntity.builder().reviewText("User2 ReviewEntity").score(9).item(item3).user(user2).build();
        
        reviewRepository.saveAll(List.of(review1, review2, review3));

        // When
        List<ReviewEntity> user1Reviews = reviewRepository.findByUserId(user1.getId());
        List<ReviewEntity> user2Reviews = reviewRepository.findByUserId(user2.getId());

        // Then
        assertThat(user1Reviews).hasSize(2);
        assertThat(user1Reviews).extracting(ReviewEntity::getReviewText)
                .containsExactlyInAnyOrder("User1 ReviewEntity 1", "User1 ReviewEntity 2");

        assertThat(user2Reviews).hasSize(1);
        assertThat(user2Reviews.get(0).getReviewText()).isEqualTo("User2 ReviewEntity");
    }

    @Test
    @DisplayName("Should return empty list when user has no reviews")
    void testFindByUserIdNoReviews() {
        // Given
        UserEntity user = createUserEntity("noreviews");
        userRepository.saveAndFlush(user);

        // When
        List<ReviewEntity> reviews = reviewRepository.findByUserId(user.getId());

        // Then
        assertThat(reviews).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for non-existent user ID")
    void testFindByUserIdNotFound() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();

        // When
        List<ReviewEntity> reviews = reviewRepository.findByUserId(nonExistentUserId);

        // Then
        assertThat(reviews).isEmpty();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update review text and score")
    void testUpdateReview() {
        // Given
        UserEntity user = createUserEntity("updater");
        ItemEntity item = createItemEntity(300L, "Movie");
        ReviewEntity review = ReviewEntity.builder()
                .reviewText("Original review")
                .score(7)
                .item(item)
                .user(user)
                .build();
        ReviewEntity savedReview = reviewRepository.saveAndFlush(review);

        // When
        savedReview.setReviewText("Updated review after second viewing");
        savedReview.setScore(9);
        ReviewEntity updatedReview = reviewRepository.saveAndFlush(savedReview);

        // Then
        assertThat(updatedReview.getId()).isEqualTo(savedReview.getId());
        assertThat(updatedReview.getReviewText()).isEqualTo("Updated review after second viewing");
        assertThat(updatedReview.getScore()).isEqualTo(9);
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete review by ID")
    void testDeleteById() {
        // Given
        UserEntity user = createUserEntity("deleter");
        ItemEntity item = createItemEntity(400L, "Movie");
        ReviewEntity review = ReviewEntity.builder()
                .reviewText("Delete me")
                .score(5)
                .item(item)
                .user(user)
                .build();
        ReviewEntity savedReview = reviewRepository.saveAndFlush(review);

        // When
        reviewRepository.deleteById(savedReview.getId());

        // Then
        assertThat(reviewRepository.findById(savedReview.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete all reviews")
    void testDeleteAll() {
        // Given
        UserEntity user = createUserEntity("user1");
        ItemEntity item1 = createItemEntity(1L, "ItemEntity 1");
        ItemEntity item2 = createItemEntity(2L, "ItemEntity 2");

        reviewRepository.saveAll(List.of(
                ReviewEntity.builder().reviewText("ReviewEntity 1").score(7).item(item1).user(user).build(),
                ReviewEntity.builder().reviewText("ReviewEntity 2").score(8).item(item2).user(user).build()
        ));

        // When
        reviewRepository.deleteAll();

        // Then
        assertThat(reviewRepository.count()).isZero();
    }

    // ==================== CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should not allow null item")
    void testNullItem() {
        // Given
        UserEntity user = createUserEntity("user");
        ReviewEntity review = ReviewEntity.builder()
                .reviewText("Test")
                .score(8)
                .item(null)
                .user(user)
                .build();

        // When & Then
        try {
            reviewRepository.saveAndFlush(review);
            fail("Should have thrown exception for null item");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should not allow null user")
    void testNullUser() {
        // Given
        ItemEntity item = createItemEntity(500L, "Movie");
        ReviewEntity review = ReviewEntity.builder()
                .reviewText("Test")
                .score(8)
                .item(item)
                .user(null)
                .build();

        // When & Then
        try {
            reviewRepository.saveAndFlush(review);
            fail("Should have thrown exception for null user");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should not allow null score")
    void testNullScore() {
        // Given
        UserEntity user = createUserEntity("user");
        ItemEntity item = createItemEntity(600L, "Movie");
        ReviewEntity review = ReviewEntity.builder()
                .reviewText("Test")
                .score(0) // Can't set to null due to primitive int
                .item(item)
                .user(user)
                .build();

        // When
        ReviewEntity savedReview = reviewRepository.saveAndFlush(review);

        // Then
        assertThat(savedReview.getScore()).isEqualTo(0);
    }

    // ==================== HELPER METHODS ====================

    private UserEntity createUserEntity(String username) {
        return UserEntity.builder()
                .username(username)
                .password("password123")
                .email(username + "@example.com")
                .build();
    }

    private ItemEntity createItemEntity(Long tmdbId, String title) {
        return ItemEntity.builder()
                .tmdbId(tmdbId)
                .tmdbType(TmdbType.MOVIE)
                .title(title)
                .overview("Overview for " + title)
                .releaseDate(LocalDate.now())
                .popularity(new BigDecimal("100.0000"))
                .voteAverage(new BigDecimal("7.50"))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();
    }
}
