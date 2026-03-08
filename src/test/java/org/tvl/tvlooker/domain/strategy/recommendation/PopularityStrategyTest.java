package org.tvl.tvlooker.domain.strategy.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.domain.motor.utils.provider.ItemPopularityProvider;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for PopularityStrategy.
 * 
 * Tests cover:
 * - Basic recommendation generation
 * - Integration with ItemPopularityProvider
 * - Score ordering (most popular first)
 * - Cold start scenarios (new users with no history)
 * - Edge cases (empty candidates, null inputs)
 * - Strategy name and metadata
 */
@DisplayName("PopularityStrategy Tests")
class PopularityStrategyTest {

    private PopularityStrategy strategy;
    private RecommendationContext context;
    private User testUser;

    @BeforeEach
    void setUp() {
        strategy = new PopularityStrategy();
        testUser = TestDataFactory.createUser("testUser");
    }

    // ===================== Basic Functionality Tests =====================

    @Test
    @DisplayName("Should have correct strategy name")
    void shouldHaveCorrectStrategyName() {
        assertEquals("popularity", strategy.getStrategyName());
    }

    @Test
    @DisplayName("Should generate recommendations based on popularity")
    void shouldGenerateRecommendationsBasedOnPopularity() {
        // Given: Items with different popularity and a registered provider
        List<Item> items = TestDataFactory.createItemsForNormalizationTest();
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy generates recommendations
        List<ScoredItem> recommendations = strategy.recommend(testUser, items, context);

        // Then: Should return recommendations for all items
        assertNotNull(recommendations);
        assertEquals(4, recommendations.size());

        // And: All recommendations should have proper metadata
        for (ScoredItem scored : recommendations) {
            assertNotNull(scored.getItem());
            assertTrue(scored.getScore() >= 0.0 && scored.getScore() <= 1.0);
            assertNotNull(scored.getExplanation());
            assertEquals("popularity", scored.getSourceStrategy());
        }
    }

    @Test
    @DisplayName("Should sort recommendations by popularity descending")
    void shouldSortRecommendationsByPopularityDescending() {
        // Given: Items with known popularity values
        List<Item> items = TestDataFactory.createItemsForNormalizationTest();
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy generates recommendations
        List<ScoredItem> recommendations = strategy.recommend(testUser, items, context);

        // Then: Most popular item should be first
        assertEquals(4L, recommendations.get(0).getItem().getId()); // Item with popularity 800
        assertEquals(1.0, recommendations.get(0).getScore(), 0.001);

        // And: Scores should be in descending order
        for (int i = 1; i < recommendations.size(); i++) {
            assertTrue(recommendations.get(i - 1).getScore() >= recommendations.get(i).getScore(),
                    "Scores should be in descending order");
        }
    }

    @Test
    @DisplayName("Should set appropriate explanation")
    void shouldSetAppropriateExplanation() {
        // Given: Items and context
        List<Item> items = TestDataFactory.createItems(3);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy generates recommendations
        List<ScoredItem> recommendations = strategy.recommend(testUser, items, context);

        // Then: All should have explanation mentioning trending/popularity
        for (ScoredItem scored : recommendations) {
            assertNotNull(scored.getExplanation());
            assertTrue(scored.getExplanation().toLowerCase().contains("trending")
                    || scored.getExplanation().toLowerCase().contains("popular"));
        }
    }

    // ===================== Cold Start Tests =====================

    @Test
    @DisplayName("Should work for new users with no interaction history (cold start)")
    void shouldWorkForNewUsersWithNoHistory() {
        // Given: New user with no interactions
        User newUser = TestDataFactory.createUser("newUser");
        List<Item> items = TestDataFactory.createItems(5);
        context = TestDataFactory.createColdStartContext(List.of(newUser), items);
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy generates recommendations for new user
        List<ScoredItem> recommendations = strategy.recommend(newUser, items, context);

        // Then: Should still generate recommendations
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        assertEquals(5, recommendations.size());

        // And: Recommendations should be based purely on popularity
        // Most popular item should be first
        assertTrue(recommendations.get(0).getScore() >= recommendations.get(1).getScore());
    }

    @Test
    @DisplayName("Should work when user has interactions (still uses popularity only)")
    void shouldWorkWhenUserHasInteractions() {
        // Given: User with interactions
        List<Item> items = TestDataFactory.createItems(10);
        List<Item> watchedItems = items.subList(0, 5);
        List<Item> candidateItems = items.subList(5, 10);
        
        var interactions = TestDataFactory.createInteractionsForUser(testUser, watchedItems);
        context = TestDataFactory.createContext(List.of(testUser), items, interactions);
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy generates recommendations (on candidates only)
        List<ScoredItem> recommendations = strategy.recommend(testUser, candidateItems, context);

        // Then: Should generate recommendations for candidate items
        assertNotNull(recommendations);
        assertEquals(5, recommendations.size());

        // And: Recommendations should be sorted by popularity (not by user history)
        for (int i = 1; i < recommendations.size(); i++) {
            assertTrue(recommendations.get(i - 1).getScore() >= recommendations.get(i).getScore());
        }
    }

    // ===================== Edge Case Tests =====================

    @Test
    @DisplayName("Should handle empty candidate items list")
    void shouldHandleEmptyCandidateItems() {
        // Given: Empty candidate items
        List<Item> items = TestDataFactory.createItems(5);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy is called with empty candidates
        List<ScoredItem> recommendations = strategy.recommend(testUser, new ArrayList<>(), context);

        // Then: Should return empty list
        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty());
    }

    @Test
    @DisplayName("Should handle single candidate item")
    void shouldHandleSingleCandidateItem() {
        // Given: Single candidate item
        List<Item> items = TestDataFactory.createItems(5);
        Item singleItem = items.get(0);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy recommends single item
        List<ScoredItem> recommendations = strategy.recommend(testUser, List.of(singleItem), context);

        // Then: Should return single recommendation
        assertNotNull(recommendations);
        assertEquals(1, recommendations.size());
        assertEquals(singleItem.getId(), recommendations.get(0).getItem().getId());
    }

    @Test
    @DisplayName("Should handle items with same popularity")
    void shouldHandleItemsWithSamePopularity() {
        // Given: All items with same popularity
        List<Item> items = List.of(
                TestDataFactory.createItem(1L, "Item 1", 500.0),
                TestDataFactory.createItem(2L, "Item 2", 500.0),
                TestDataFactory.createItem(3L, "Item 3", 500.0)
        );
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy generates recommendations
        List<ScoredItem> recommendations = strategy.recommend(testUser, items, context);

        // Then: All should have same score (1.0 since they're all equally popular)
        assertNotNull(recommendations);
        assertEquals(3, recommendations.size());
        for (ScoredItem scored : recommendations) {
            assertEquals(1.0, scored.getScore(), 0.001);
        }
    }

    @Test
    @DisplayName("Should not throw exception when user is null (strategy doesn't use user)")
    void shouldNotThrowExceptionWhenUserIsNull() {
        // Given: Context with items
        List<Item> items = TestDataFactory.createItems(3);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Calling with null user (PopularityStrategy doesn't use user parameter)
        List<ScoredItem> results = strategy.recommend(null, items, context);
        
        // Then: Should return results (popularity doesn't depend on user)
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when context is null")
    void shouldThrowExceptionWhenContextIsNull() {
        // Given: Items but null context
        List<Item> items = TestDataFactory.createItems(3);

        // When & Then: Should throw exception
        assertThrows(NullPointerException.class, 
                () -> strategy.recommend(testUser, items, null));
    }

    @Test
    @DisplayName("Should throw exception when candidate items is null")
    void shouldThrowExceptionWhenCandidateItemsIsNull() {
        // Given: Valid context
        List<Item> items = TestDataFactory.createItems(3);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When & Then: Should throw exception
        assertThrows(NullPointerException.class, 
                () -> strategy.recommend(testUser, null, context));
    }

    // ===================== Provider Integration Tests =====================

    @Test
    @DisplayName("Should use ItemPopularityProvider from context")
    void shouldUseItemPopularityProviderFromContext() {
        // Given: Context with registered provider
        List<Item> items = TestDataFactory.createItemsForNormalizationTest();
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        
        ItemPopularityProvider provider = new ItemPopularityProvider();
        context.registerDataProvider(provider);

        // When: Strategy generates recommendations
        List<ScoredItem> recommendations = strategy.recommend(testUser, items, context);

        // Then: Should successfully retrieve and use popularity data
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        
        // Verify that scores match what the provider would generate
        Map<Long, Double> expectedScores = provider.provide(context);
        for (ScoredItem scored : recommendations) {
            Long itemId = scored.getItem().getId();
            assertEquals(expectedScores.get(itemId), scored.getScore(), 0.001);
        }
    }

    @Test
    @DisplayName("Should respect provider caching")
    void shouldRespectProviderCaching() {
        // Given: Context with provider
        List<Item> items = TestDataFactory.createItems(5);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy is called multiple times
        List<ScoredItem> recommendations1 = strategy.recommend(testUser, items, context);
        List<ScoredItem> recommendations2 = strategy.recommend(testUser, items, context);

        // Then: Both should return same results (showing cache is working)
        assertEquals(recommendations1.size(), recommendations2.size());
        for (int i = 0; i < recommendations1.size(); i++) {
            assertEquals(recommendations1.get(i).getScore(), 
                        recommendations2.get(i).getScore(), 
                        0.001);
        }
    }

    // ===================== Score Validation Tests =====================

    @Test
    @DisplayName("Should ensure all scores are in valid range [0, 1]")
    void shouldEnsureAllScoresInValidRange() {
        // Given: Items with various popularity values
        List<Item> items = TestDataFactory.createItems(20);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy generates recommendations
        List<ScoredItem> recommendations = strategy.recommend(testUser, items, context);

        // Then: All scores should be in [0, 1] range
        for (ScoredItem scored : recommendations) {
            assertTrue(scored.getScore() >= 0.0 && scored.getScore() <= 1.0,
                    "Score " + scored.getScore() + " should be in range [0, 1]");
        }
    }

    @Test
    @DisplayName("Should handle items with zero popularity")
    void shouldHandleItemsWithZeroPopularity() {
        // Given: Items including some with zero popularity
        List<Item> items = List.of(
                TestDataFactory.createItem(1L, "Item 1", 0.0),
                TestDataFactory.createItem(2L, "Item 2", 100.0)
        );
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy generates recommendations
        List<ScoredItem> recommendations = strategy.recommend(testUser, items, context);

        // Then: Should handle gracefully
        assertNotNull(recommendations);
        assertEquals(2, recommendations.size());
    }

    // ===================== Performance Tests =====================

    @Test
    @DisplayName("Should handle large number of candidate items efficiently")
    void shouldHandleLargeNumberOfCandidateItems() {
        // Given: Large number of items
        List<Item> items = TestDataFactory.createItems(1000);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy processes many items
        long startTime = System.currentTimeMillis();
        List<ScoredItem> recommendations = strategy.recommend(testUser, items, context);
        long endTime = System.currentTimeMillis();

        // Then: Should complete in reasonable time (< 2 seconds)
        assertTrue(endTime - startTime < 2000, 
                "Processing should complete within 2 seconds");
        assertEquals(1000, recommendations.size());
    }

    // ===================== User Independence Tests =====================

    @Test
    @DisplayName("Should generate same recommendations for different users (popularity-based)")
    void shouldGenerateSameRecommendationsForDifferentUsers() {
        // Given: Two different users
        User user1 = TestDataFactory.createUser("user1");
        User user2 = TestDataFactory.createUser("user2");
        List<Item> items = TestDataFactory.createItems(5);
        context = TestDataFactory.createContext(List.of(user1, user2), items, new ArrayList<>());
        context.registerDataProvider(new ItemPopularityProvider());

        // When: Strategy generates recommendations for both users
        List<ScoredItem> recommendations1 = strategy.recommend(user1, items, context);
        List<ScoredItem> recommendations2 = strategy.recommend(user2, items, context);

        // Then: Recommendations should be identical (since popularity is user-independent)
        assertEquals(recommendations1.size(), recommendations2.size());
        for (int i = 0; i < recommendations1.size(); i++) {
            assertEquals(recommendations1.get(i).getItem().getId(),
                        recommendations2.get(i).getItem().getId());
            assertEquals(recommendations1.get(i).getScore(),
                        recommendations2.get(i).getScore(),
                        0.001);
        }
    }
}
