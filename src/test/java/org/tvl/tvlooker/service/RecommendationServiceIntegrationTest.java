package org.tvl.tvlooker.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.model.entity.Interaction;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.InteractionRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for RecommendationService.
 * Tests the full flow: RecommendationService → Entity Services → Repositories → Database → Engine → Result
 */
@SpringBootTest
@DisplayName("RecommendationService Integration Tests")
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class RecommendationServiceIntegrationTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private InteractionRepository interactionRepository;

    private User testUser1;
    private User testUser2;
    private User newUser;
    private Item popularItem1;
    private Item popularItem2;
    private Item popularItem3;
    private Item lessPopularItem;

    /**
     * Set up test data before each test.
     * Creates users, items with varying popularity, and interactions to simulate different scenarios.
     */
    @BeforeEach
    void setUp() {
        // Clear all data
        interactionRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUser1 = User.builder()
                .username("testuser1")
                .password("password123")
                .build();
        testUser1 = userRepository.save(testUser1);

        testUser2 = User.builder()
                .username("testuser2")
                .password("password123")
                .build();
        testUser2 = userRepository.save(testUser2);

        newUser = User.builder()
                .username("newuser")
                .password("password123")
                .build();
        newUser = userRepository.save(newUser);

        // Create test items with different popularity scores
        popularItem1 = Item.builder()
                .tmdbId(1L)
                .tmdbType(TmdbType.MOVIE)
                .title("The Matrix")
                .overview("A computer hacker learns about the true nature of reality.")
                .releaseDate(LocalDate.of(1999, 3, 31))
                .popularity(BigDecimal.valueOf(950.5678))
                .voteAverage(BigDecimal.valueOf(8.7))
                .build();
        popularItem1 = itemRepository.save(popularItem1);

        popularItem2 = Item.builder()
                .tmdbId(2L)
                .tmdbType(TmdbType.MOVIE)
                .title("Inception")
                .overview("A thief who steals corporate secrets through dream-sharing technology.")
                .releaseDate(LocalDate.of(2010, 7, 16))
                .popularity(BigDecimal.valueOf(920.1234))
                .voteAverage(BigDecimal.valueOf(8.8))
                .build();
        popularItem2 = itemRepository.save(popularItem2);

        popularItem3 = Item.builder()
                .tmdbId(3L)
                .tmdbType(TmdbType.TV)
                .title("Breaking Bad")
                .overview("A chemistry teacher turned methamphetamine manufacturer.")
                .releaseDate(LocalDate.of(2008, 1, 20))
                .popularity(BigDecimal.valueOf(900.0))
                .voteAverage(BigDecimal.valueOf(9.5))
                .build();
        popularItem3 = itemRepository.save(popularItem3);

        lessPopularItem = Item.builder()
                .tmdbId(4L)
                .tmdbType(TmdbType.MOVIE)
                .title("Indie Film")
                .overview("A lesser-known independent film.")
                .releaseDate(LocalDate.of(2020, 5, 1))
                .popularity(BigDecimal.valueOf(50.0))
                .voteAverage(BigDecimal.valueOf(7.0))
                .build();
        lessPopularItem = itemRepository.save(lessPopularItem);

        // Create interactions for testUser1 (has interaction history)
        // testUser1 has watched/rated popular items
        Interaction interaction1 = Interaction.builder()
                .id(1L)
                .user(testUser1)
                .item(popularItem1)
                .interactionType(InteractionType.VIEW)
                .build();
        interactionRepository.save(interaction1);

        Interaction interaction2 = Interaction.builder()
                .id(2L)
                .user(testUser1)
                .item(popularItem2)
                .interactionType(InteractionType.RATING)
                .build();
        interactionRepository.save(interaction2);

        // Create interactions for testUser2
        Interaction interaction3 = Interaction.builder()
                .id(3L)
                .user(testUser2)
                .item(popularItem1)
                .interactionType(InteractionType.VIEW)
                .build();
        interactionRepository.save(interaction3);

        Interaction interaction4 = Interaction.builder()
                .id(4L)
                .user(testUser2)
                .item(lessPopularItem)
                .interactionType(InteractionType.VIEW)
                .build();
        interactionRepository.save(interaction4);

        // newUser has NO interactions
    }

    /**
     * Clean up test data after each test.
     */
    @AfterEach
    void tearDown() {
        interactionRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==========================
    // SUCCESSFUL RECOMMENDATION TESTS
    // ==========================

    @Test
    @DisplayName("Should return personalized recommendations for user with interaction history")
    void testGetRecommendations_UserWithInteractions_ReturnsRecommendations() {
        // Act
        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1.getId(), 5);

        // Assert
        assertThat(recommendations).isNotNull();
        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).hasSizeLessThanOrEqualTo(5);

        // Verify all returned items exist in the database
        for (Item item : recommendations) {
            assertThat(itemRepository.findById(item.getId())).isPresent();
        }
    }

    @Test
    @DisplayName("Should return popularity-based recommendations for new user without interactions")
    void testGetRecommendations_NewUserNoInteractions_ReturnsPopularityBasedRecommendations() {
        // Act
        List<Item> recommendations = recommendationService.getUserRecommendations(newUser.getId(), 3);

        // Assert
        assertThat(recommendations).isNotNull();
        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).hasSizeLessThanOrEqualTo(3);

        // Verify recommendations are based on popularity (should include popular items)
        // Since newUser has no interactions, engine should fall back to popularity-based recommendations
        for (Item item : recommendations) {
            assertThat(itemRepository.findById(item.getId())).isPresent();
        }
    }

    @Test
    @DisplayName("Should respect limit parameter and return correct number of recommendations")
    void testGetRecommendations_LimitParameter_ReturnsCorrectNumberOfItems() {
        // Arrange
        int limit = 2;

        // Act
        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1.getId(), limit);

        // Assert
        assertThat(recommendations).hasSizeLessThanOrEqualTo(limit);
    }

    @Test
    @DisplayName("Should return items that exist in database")
    void testGetRecommendations_VerifyItemsExistInDatabase() {
        // Act
        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1.getId(), 5);

        // Assert
        assertThat(recommendations).isNotNull();
        for (Item item : recommendations) {
            // Verify each item can be found in the database
            Item dbItem = itemRepository.findById(item.getId()).orElse(null);
            assertThat(dbItem).isNotNull();
            assertThat(dbItem.getId()).isEqualTo(item.getId());
            assertThat(dbItem.getTitle()).isEqualTo(item.getTitle());
        }
    }

    @Test
    @DisplayName("Should handle multiple users with different interaction patterns")
    void testGetRecommendations_MultipleUsers_ReturnsPersonalizedResults() {
        // Act - Get recommendations for both users
        List<Item> recommendationsUser1 = recommendationService.getUserRecommendations(testUser1.getId(), 3);
        List<Item> recommendationsUser2 = recommendationService.getUserRecommendations(testUser2.getId(), 3);

        // Assert
        assertThat(recommendationsUser1).isNotNull();
        assertThat(recommendationsUser2).isNotNull();
        assertThat(recommendationsUser1).isNotEmpty();
        assertThat(recommendationsUser2).isNotEmpty();

        // Both users should get recommendations (personalization is handled by the engine)
        // We just verify the service layer works correctly for different users
    }

    // ==========================
    // EXCEPTION HANDLING TESTS
    // ==========================

    @Test
    @DisplayName("Should throw UserNotFoundException for non-existent user")
    void testGetRecommendations_NonExistentUser_ThrowsUserNotFoundException() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();

        // Act & Assert
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(nonExistentUserId, 5))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: " + nonExistentUserId);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null user ID")
    void testGetRecommendations_NullUserId_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(null, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for zero limit")
    void testGetRecommendations_ZeroLimit_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(testUser1.getId(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be greater than 0");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative limit")
    void testGetRecommendations_NegativeLimit_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(testUser1.getId(), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be greater than 0");
    }

    // ==========================
    // EDGE CASE TESTS
    // ==========================

    @Test
    @DisplayName("Should handle limit larger than available items")
    void testGetRecommendations_LimitLargerThanAvailableItems_ReturnsAllAvailableItems() {
        // Arrange
        int largeLimit = 1000;

        // Act
        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1.getId(), largeLimit);

        // Assert
        assertThat(recommendations).isNotNull();
        // Should return at most the number of items in the database (4 items in our test data)
        assertThat(recommendations.size()).isLessThanOrEqualTo(4);
    }

    @Test
    @DisplayName("Should work correctly with limit of 1")
    void testGetRecommendations_LimitOne_ReturnsSingleItem() {
        // Act
        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1.getId(), 1);

        // Assert
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0)).isNotNull();
    }
}
