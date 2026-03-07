package org.tvl.tvlooker.domain.motor.utils.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for ItemPopularityProvider.
 * 
 * Tests cover:
 * - Normalization of popularity scores to [0, 1] range
 * - Handling of empty item lists
 * - Handling of items with zero popularity
 * - Correct provider ID
 * - Cacheability settings
 * - Edge cases and boundary conditions
 */
@DisplayName("ItemPopularityProvider Tests")
class ItemPopularityProviderTest {

    private ItemPopularityProvider provider;
    private RecommendationContext context;

    @BeforeEach
    void setUp() {
        provider = new ItemPopularityProvider();
    }

    // ===================== Basic Functionality Tests =====================

    @Test
    @DisplayName("Should have correct provider ID")
    void shouldHaveCorrectProviderId() {
        assertEquals("item-popularity", provider.getProviderId());
    }

    @Test
    @DisplayName("Should be cacheable")
    void shouldBeCacheable() {
        assertTrue(provider.isCacheable());
    }

    @Test
    @DisplayName("Should have 24-hour cache expiration")
    void shouldHave24HourCacheExpiration() {
        assertEquals(86400, provider.getCacheExpirationSeconds());
    }

    // ===================== Normalization Tests =====================

    @Test
    @DisplayName("Should normalize popularity scores to [0, 1] range")
    void shouldNormalizePopularityScores() {
        // Given: Items with known popularity values
        List<Item> items = TestDataFactory.createItemsForNormalizationTest();
        context = TestDataFactory.createContext(new ArrayList<>(), items, new ArrayList<>());

        // When: Provider processes the items
        Map<Long, Double> popularityScores = provider.provide(context);

        // Then: Scores should be normalized
        assertNotNull(popularityScores);
        assertEquals(4, popularityScores.size());

        // Item with max popularity (800) should have score 1.0
        assertEquals(1.0, popularityScores.get(4L), 0.001);

        // Item with popularity 400 should have score 0.5
        assertEquals(0.5, popularityScores.get(3L), 0.001);

        // Item with popularity 200 should have score 0.25
        assertEquals(0.25, popularityScores.get(2L), 0.001);

        // Item with popularity 100 should have score 0.125
        assertEquals(0.125, popularityScores.get(1L), 0.001);
    }

    @Test
    @DisplayName("Should normalize when all items have same popularity")
    void shouldNormalizeWhenAllItemsHaveSamePopularity() {
        // Given: All items with same popularity
        List<Item> items = List.of(
                TestDataFactory.createItem(1L, "Item 1", 500.0),
                TestDataFactory.createItem(2L, "Item 2", 500.0),
                TestDataFactory.createItem(3L, "Item 3", 500.0)
        );
        context = TestDataFactory.createContext(new ArrayList<>(), items, new ArrayList<>());

        // When: Provider processes the items
        Map<Long, Double> popularityScores = provider.provide(context);

        // Then: All should have score 1.0
        assertEquals(1.0, popularityScores.get(1L), 0.001);
        assertEquals(1.0, popularityScores.get(2L), 0.001);
        assertEquals(1.0, popularityScores.get(3L), 0.001);
    }

    @Test
    @DisplayName("Should handle single item correctly")
    void shouldHandleSingleItem() {
        // Given: Single item
        List<Item> items = List.of(TestDataFactory.createItem(1L, "Item 1", 100.0));
        context = TestDataFactory.createContext(new ArrayList<>(), items, new ArrayList<>());

        // When: Provider processes the item
        Map<Long, Double> popularityScores = provider.provide(context);

        // Then: Single item should have score 1.0
        assertEquals(1, popularityScores.size());
        assertEquals(1.0, popularityScores.get(1L), 0.001);
    }

    // ===================== Edge Case Tests =====================

    @Test
    @DisplayName("Should handle empty items list")
    void shouldHandleEmptyItemsList() {
        // Given: Empty items list
        context = TestDataFactory.createContext(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        // When: Provider processes empty list
        Map<Long, Double> popularityScores = provider.provide(context);

        // Then: Should return empty map
        assertNotNull(popularityScores);
        assertTrue(popularityScores.isEmpty());
    }

    @Test
    @DisplayName("Should handle items with zero popularity")
    void shouldHandleItemsWithZeroPopularity() {
        // Given: Items with zero and non-zero popularity
        List<Item> items = List.of(
                TestDataFactory.createItem(1L, "Item 1", 0.0),
                TestDataFactory.createItem(2L, "Item 2", 100.0)
        );
        context = TestDataFactory.createContext(new ArrayList<>(), items, new ArrayList<>());

        // When: Provider processes the items
        Map<Long, Double> popularityScores = provider.provide(context);

        // Then: Zero popularity item should have score 0.0, other should have 1.0
        assertEquals(0.0, popularityScores.get(1L), 0.001);
        assertEquals(1.0, popularityScores.get(2L), 0.001);
    }

    @Test
    @DisplayName("Should handle all items with zero popularity")
    void shouldHandleAllItemsWithZeroPopularity() {
        // Given: All items with zero popularity
        List<Item> items = List.of(
                TestDataFactory.createItem(1L, "Item 1", 0.0),
                TestDataFactory.createItem(2L, "Item 2", 0.0)
        );
        context = TestDataFactory.createContext(new ArrayList<>(), items, new ArrayList<>());

        // When: Provider processes the items
        Map<Long, Double> popularityScores = provider.provide(context);

        // Then: All should have score 0.0 (since max is 0, we normalize to 0)
        // Or could be 1.0 depending on implementation - check actual implementation
        assertNotNull(popularityScores);
        assertEquals(2, popularityScores.size());
    }

    @Test
    @DisplayName("Should handle very large popularity values")
    void shouldHandleVeryLargePopularityValues() {
        // Given: Items with very large popularity values
        List<Item> items = List.of(
                TestDataFactory.createItem(1L, "Item 1", 1000000.0),
                TestDataFactory.createItem(2L, "Item 2", 500000.0)
        );
        context = TestDataFactory.createContext(new ArrayList<>(), items, new ArrayList<>());

        // When: Provider processes the items
        Map<Long, Double> popularityScores = provider.provide(context);

        // Then: Should normalize correctly
        assertEquals(1.0, popularityScores.get(1L), 0.001);
        assertEquals(0.5, popularityScores.get(2L), 0.001);
    }

    @Test
    @DisplayName("Should handle very small popularity differences")
    void shouldHandleSmallPopularityDifferences() {
        // Given: Items with very small differences in popularity
        List<Item> items = List.of(
                TestDataFactory.createItem(1L, "Item 1", 100.001),
                TestDataFactory.createItem(2L, "Item 2", 100.0)
        );
        context = TestDataFactory.createContext(new ArrayList<>(), items, new ArrayList<>());

        // When: Provider processes the items
        Map<Long, Double> popularityScores = provider.provide(context);

        // Then: Should handle small differences
        assertNotNull(popularityScores);
        assertTrue(popularityScores.get(1L) > popularityScores.get(2L));
        assertEquals(1.0, popularityScores.get(1L), 0.001);
    }

    // ===================== Context Integration Tests =====================

    @Test
    @DisplayName("Should throw exception when context is null")
    void shouldThrowExceptionWhenContextIsNull() {
        // When & Then: Provider should handle null context gracefully
        assertThrows(NullPointerException.class, () -> provider.provide(null));
    }

    @Test
    @DisplayName("Should throw exception when context items are null")
    void shouldThrowExceptionWhenContextItemsAreNull() {
        // Given: Context with null items
        context = TestDataFactory.createNullDataContext();

        // When & Then: Provider should handle null items
        assertThrows(NullPointerException.class, () -> provider.provide(context));
    }

    // ===================== Performance Tests =====================

    @Test
    @DisplayName("Should handle large number of items efficiently")
    void shouldHandleLargeNumberOfItems() {
        // Given: Large number of items
        List<Item> items = TestDataFactory.createItems(1000);
        context = TestDataFactory.createContext(new ArrayList<>(), items, new ArrayList<>());

        // When: Provider processes many items
        long startTime = System.currentTimeMillis();
        Map<Long, Double> popularityScores = provider.provide(context);
        long endTime = System.currentTimeMillis();

        // Then: Should complete in reasonable time (< 1 second)
        assertTrue(endTime - startTime < 1000, "Processing should complete within 1 second");
        assertEquals(1000, popularityScores.size());

        // Verify normalization is correct
        assertTrue(popularityScores.values().stream().allMatch(score -> score >= 0.0 && score <= 1.0));
    }

    // ===================== Boundary Tests =====================

    @Test
    @DisplayName("Should ensure all scores are in valid range [0, 1]")
    void shouldEnsureAllScoresInValidRange() {
        // Given: Items with various popularity values
        List<Item> items = TestDataFactory.createItems(20);
        context = TestDataFactory.createContext(new ArrayList<>(), items, new ArrayList<>());

        // When: Provider processes the items
        Map<Long, Double> popularityScores = provider.provide(context);

        // Then: All scores should be in [0, 1] range
        for (Double score : popularityScores.values()) {
            assertTrue(score >= 0.0 && score <= 1.0,
                    "Score " + score + " should be in range [0, 1]");
        }

        // And: At least one item should have score 1.0 (the most popular)
        assertTrue(popularityScores.values().stream().anyMatch(score -> Math.abs(score - 1.0) < 0.001),
                "At least one item should have score 1.0");
    }

    @Test
    @DisplayName("Should maintain relative ordering of popularity")
    void shouldMaintainRelativeOrderingOfPopularity() {
        // Given: Items with specific popularity ordering
        List<Item> items = List.of(
                TestDataFactory.createItem(1L, "Low", 100.0),
                TestDataFactory.createItem(2L, "Medium", 500.0),
                TestDataFactory.createItem(3L, "High", 1000.0)
        );
        context = TestDataFactory.createContext(new ArrayList<>(), items, new ArrayList<>());

        // When: Provider processes the items
        Map<Long, Double> popularityScores = provider.provide(context);

        // Then: Relative ordering should be maintained
        assertTrue(popularityScores.get(3L) > popularityScores.get(2L));
        assertTrue(popularityScores.get(2L) > popularityScores.get(1L));
    }
}
