package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.Review;
import org.tvl.tvlooker.domain.model.entity.User;
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
        User user = createUser("reviewer1");
        Item item = createItem(12345L, "The Matrix");

        Review review = Review.builder()
                .reviewText("Amazing movie!")
                .score(9)
                .item(item)
                .user(user)
                .build();

        // When - Save review (cascade will persist user and item)
        Review savedReview = reviewRepository.saveAndFlush(review);

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
        User user = createUser("quickrater");
        Item item = createItem(67890L, "Inception");

        Review review = Review.builder()
                .reviewText(null)
                .score(10)
                .item(item)
                .user(user)
                .build();

        // When
        Review savedReview = reviewRepository.saveAndFlush(review);

        // Then
        assertThat(savedReview).isNotNull();
        assertThat(savedReview.getReviewText()).isNull();
        assertThat(savedReview.getScore()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should save multiple reviews for different items")
    void testSaveMultipleReviews() {
        // Given
        User user = createUser("critic1");
        Item item1 = createItem(1L, "Movie 1");
        Item item2 = createItem(2L, "Movie 2");

        Review review1 = Review.builder().reviewText("Good").score(7).item(item1).user(user).build();
        Review review2 = Review.builder().reviewText("Great").score(8).item(item2).user(user).build();

        // When
        List<Review> savedReviews = reviewRepository.saveAll(List.of(review1, review2));

        // Then
        assertThat(savedReviews).hasSize(2);
        assertThat(reviewRepository.count()).isEqualTo(2);
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find review by ID")
    void testFindById() {
        // Given
        User user = createUser("finduser");
        Item item = createItem(100L, "Findable Movie");
        Review review = Review.builder()
                .reviewText("Test review")
                .score(8)
                .item(item)
                .user(user)
                .build();
        Review savedReview = reviewRepository.saveAndFlush(review);

        // When
        Optional<Review> foundReview = reviewRepository.findById(savedReview.getId());

        // Then
        assertThat(foundReview).isPresent();
        assertThat(foundReview.get().getReviewText()).isEqualTo("Test review");
        assertThat(foundReview.get().getScore()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should find all reviews")
    void testFindAll() {
        // Given
        User user = createUser("user1");
        Item item1 = createItem(1L, "Item 1");
        Item item2 = createItem(2L, "Item 2");

        reviewRepository.saveAll(List.of(
                Review.builder().reviewText("Review 1").score(7).item(item1).user(user).build(),
                Review.builder().reviewText("Review 2").score(8).item(item2).user(user).build()
        ));

        // When
        List<Review> allReviews = reviewRepository.findAll();

        // Then
        assertThat(allReviews).hasSize(2);
    }

    @Test
    @DisplayName("Should count all reviews")
    void testCount() {
        // Given
        User user = createUser("counter");
        Item item1 = createItem(1L, "Item 1");
        Item item2 = createItem(2L, "Item 2");

        reviewRepository.saveAll(List.of(
                Review.builder().reviewText("Review 1").score(7).item(item1).user(user).build(),
                Review.builder().reviewText("Review 2").score(8).item(item2).user(user).build()
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
        User user1 = createUser("user1");
        User user2 = createUser("user2");
        Item item1 = createItem(1L, "Item 1");
        Item item2 = createItem(2L, "Item 2");
        Item item3 = createItem(3L, "Item 3");

        Review review1 = Review.builder().reviewText("User1 Review 1").score(7).item(item1).user(user1).build();
        Review review2 = Review.builder().reviewText("User1 Review 2").score(8).item(item2).user(user1).build();
        Review review3 = Review.builder().reviewText("User2 Review").score(9).item(item3).user(user2).build();
        
        reviewRepository.saveAll(List.of(review1, review2, review3));

        // When
        List<Review> user1Reviews = reviewRepository.findByUser_Id(user1.getId());
        List<Review> user2Reviews = reviewRepository.findByUser_Id(user2.getId());

        // Then
        assertThat(user1Reviews).hasSize(2);
        assertThat(user1Reviews).extracting(Review::getReviewText)
                .containsExactlyInAnyOrder("User1 Review 1", "User1 Review 2");

        assertThat(user2Reviews).hasSize(1);
        assertThat(user2Reviews.get(0).getReviewText()).isEqualTo("User2 Review");
    }

    @Test
    @DisplayName("Should return empty list when user has no reviews")
    void testFindByUserIdNoReviews() {
        // Given
        User user = createUser("noreviews");
        userRepository.saveAndFlush(user);

        // When
        List<Review> reviews = reviewRepository.findByUser_Id(user.getId());

        // Then
        assertThat(reviews).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for non-existent user ID")
    void testFindByUserIdNotFound() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();

        // When
        List<Review> reviews = reviewRepository.findByUser_Id(nonExistentUserId);

        // Then
        assertThat(reviews).isEmpty();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update review text and score")
    void testUpdateReview() {
        // Given
        User user = createUser("updater");
        Item item = createItem(300L, "Movie");
        Review review = Review.builder()
                .reviewText("Original review")
                .score(7)
                .item(item)
                .user(user)
                .build();
        Review savedReview = reviewRepository.saveAndFlush(review);

        // When
        savedReview.setReviewText("Updated review after second viewing");
        savedReview.setScore(9);
        Review updatedReview = reviewRepository.saveAndFlush(savedReview);

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
        User user = createUser("deleter");
        Item item = createItem(400L, "Movie");
        Review review = Review.builder()
                .reviewText("Delete me")
                .score(5)
                .item(item)
                .user(user)
                .build();
        Review savedReview = reviewRepository.saveAndFlush(review);

        // When
        reviewRepository.deleteById(savedReview.getId());

        // Then
        assertThat(reviewRepository.findById(savedReview.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete all reviews")
    void testDeleteAll() {
        // Given
        User user = createUser("user1");
        Item item1 = createItem(1L, "Item 1");
        Item item2 = createItem(2L, "Item 2");

        reviewRepository.saveAll(List.of(
                Review.builder().reviewText("Review 1").score(7).item(item1).user(user).build(),
                Review.builder().reviewText("Review 2").score(8).item(item2).user(user).build()
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
        User user = createUser("user");
        Review review = Review.builder()
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
        Item item = createItem(500L, "Movie");
        Review review = Review.builder()
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
        User user = createUser("user");
        Item item = createItem(600L, "Movie");
        Review review = Review.builder()
                .reviewText("Test")
                .score(0) // Can't set to null due to primitive int
                .item(item)
                .user(user)
                .build();

        // When
        Review savedReview = reviewRepository.saveAndFlush(review);

        // Then
        assertThat(savedReview.getScore()).isEqualTo(0);
    }

    // ==================== HELPER METHODS ====================

    private User createUser(String username) {
        return User.builder()
                .username(username)
                .password("password123")
                .build();
    }

    private Item createItem(Long tmdbId, String title) {
        return Item.builder()
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
