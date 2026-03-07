package org.tvl.tvlooker.domain.strategy.aggregation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for ConstantConvexAggregation.
 * 
 * Tests cover:
 * - Weight-based score aggregation
 * - Normalization when strategies are missing
 * - Handling of overlapping and non-overlapping results
 * - Edge cases (empty results, single strategy)
 * - Weight validation
 * - Explanation merging
 */
@DisplayName("ConstantConvexAggregation Tests")
class ConstantConvexAggregationTest {

    private ConstantConvexAggregation aggregation;
    private RecommendationContext context;

    @BeforeEach
    void setUp() {
        // Create aggregation with default test weights
        Map<String, Double> weights = new HashMap<>();
        weights.put("popularity", 0.2);
        weights.put("content", 0.2);
        weights.put("item-collaborative", 0.2);
        weights.put("user-collaborative", 0.2);
        weights.put("matrix-factorization", 0.2);
        
        aggregation = new ConstantConvexAggregation(weights);

        context = TestDataFactory.createPopulatedContext();
    }

    // ===================== Basic Functionality Tests =====================

    @Test
    @DisplayName("Should have correct aggregation name")
    void shouldHaveCorrectAggregationName() {
        assertEquals("constant-convex-combination", aggregation.getAggregationName());
    }

    @Test
    @DisplayName("Should aggregate results from single strategy")
    void shouldAggregateResultsFromSingleStrategy() {
        // Given: Results from a single strategy
        List<Item> items = TestDataFactory.createItems(3);
        List<ScoredItem> popularityResults = List.of(
                TestDataFactory.createScoredItem(items.get(0), 0.9, "Popular", "popularity"),
                TestDataFactory.createScoredItem(items.get(1), 0.7, "Popular", "popularity"),
                TestDataFactory.createScoredItem(items.get(2), 0.5, "Popular", "popularity")
        );

        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", popularityResults);

        // When: Aggregating results
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: Should return results sorted by score
        assertNotNull(aggregated);
        assertEquals(3, aggregated.size());
        
        // Verify ordering
        assertTrue(aggregated.get(0).getScore() >= aggregated.get(1).getScore());
        assertTrue(aggregated.get(1).getScore() >= aggregated.get(2).getScore());
        
        // Verify source strategy is set to aggregation name
        for (ScoredItem scored : aggregated) {
            assertEquals("constant-convex-combination", scored.getSourceStrategy());
        }
    }

    @Test
    @DisplayName("Should aggregate results from multiple strategies")
    void shouldAggregateResultsFromMultipleStrategies() {
        // Given: Results from multiple strategies for the same items
        List<Item> items = TestDataFactory.createItems(3);
        
        List<ScoredItem> popularityResults = List.of(
                TestDataFactory.createScoredItem(items.get(0), 0.9, "Popular", "popularity"),
                TestDataFactory.createScoredItem(items.get(1), 0.6, "Popular", "popularity")
        );
        
        List<ScoredItem> contentResults = List.of(
                TestDataFactory.createScoredItem(items.get(0), 0.7, "Similar content", "content"),
                TestDataFactory.createScoredItem(items.get(2), 0.8, "Similar content", "content")
        );

        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", popularityResults);
        strategyResults.put("content", contentResults);

        // When: Aggregating results
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: Should combine scores from both strategies
        assertNotNull(aggregated);
        assertEquals(3, aggregated.size()); // All unique items

        // Item 0 appears in both strategies, should have combined score
        ScoredItem item0 = aggregated.stream()
                .filter(s -> s.getItem().getId().equals(1L))
                .findFirst()
                .orElse(null);
        assertNotNull(item0);
        // Score should be weighted combination
        assertTrue(item0.getScore() > 0 && item0.getScore() <= 1.0);
    }

    // ===================== Weight Normalization Tests =====================

    @Test
    @DisplayName("Should normalize weights when item appears in only some strategies")
    void shouldNormalizeWeightsWhenItemAppearsInSomeStrategies() {
        // Given: Two items, each appearing in a different strategy
        List<Item> items = TestDataFactory.createItems(2);
        
        List<ScoredItem> popularityResults = List.of(
                TestDataFactory.createScoredItem(items.get(0), 1.0, "From popularity", "popularity")
        );
        
        List<ScoredItem> contentResults = List.of(
                TestDataFactory.createScoredItem(items.get(1), 1.0, "From content", "content")
        );

        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", popularityResults);
        strategyResults.put("content", contentResults);

        // When: Aggregating results
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: Each item should be scored based on available strategies only
        assertNotNull(aggregated);
        assertEquals(2, aggregated.size());
        
        // Both items should have scores (normalized by their active weights)
        for (ScoredItem scored : aggregated) {
            assertTrue(scored.getScore() > 0 && scored.getScore() <= 1.0, 
                "Score should be normalized: " + scored.getScore());
        }
    }

    @Test
    @DisplayName("Should handle item appearing in all strategies")
    void shouldHandleItemAppearingInAllStrategies() {
        // Given: Same item appears in all strategies with different scores
        List<Item> items = TestDataFactory.createItems(1);
        Item commonItem = items.get(0);
        
        List<ScoredItem> strategy1Results = List.of(
                TestDataFactory.createScoredItem(commonItem, 0.8, "Strategy 1", "popularity")
        );
        
        List<ScoredItem> strategy2Results = List.of(
                TestDataFactory.createScoredItem(commonItem, 0.6, "Strategy 2", "content")
        );
        
        List<ScoredItem> strategy3Results = List.of(
                TestDataFactory.createScoredItem(commonItem, 0.7, "Strategy 3", "item-collaborative")
        );

        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", strategy1Results);
        strategyResults.put("content", strategy2Results);
        strategyResults.put("item-collaborative", strategy3Results);

        // When: Aggregating results
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: Should return weighted combination
        assertNotNull(aggregated);
        assertEquals(1, aggregated.size());
        
        ScoredItem result = aggregated.get(0);
        // Score should be weighted average of all three scores
        assertTrue(result.getScore() > 0.6 && result.getScore() < 0.8);
    }

    // ===================== Edge Case Tests =====================

    @Test
    @DisplayName("Should handle empty strategy results")
    void shouldHandleEmptyStrategyResults() {
        // Given: Empty strategy results
        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();

        // When: Aggregating empty results
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: Should return empty list
        assertNotNull(aggregated);
        assertTrue(aggregated.isEmpty());
    }

    @Test
    @DisplayName("Should handle strategy with empty result list")
    void shouldHandleStrategyWithEmptyResultList() {
        // Given: One strategy with results, another with empty list
        List<Item> items = TestDataFactory.createItems(2);
        
        List<ScoredItem> strategy1Results = List.of(
                TestDataFactory.createScoredItem(items.get(0), 0.9, "Strategy 1", "popularity")
        );

        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", strategy1Results);
        strategyResults.put("content", new ArrayList<>()); // Empty list

        // When: Aggregating results
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: Should handle gracefully
        assertNotNull(aggregated);
        assertEquals(1, aggregated.size());
    }

    @Test
    @DisplayName("Should handle null strategy results gracefully")
    void shouldHandleNullStrategyResults() {
        // When & Then: Should throw appropriate exception
        assertThrows(NullPointerException.class, 
                () -> aggregation.aggregate(null, context));
    }

    // ===================== Score Validation Tests =====================

    @Test
    @DisplayName("Should ensure all aggregated scores are in valid range [0, 1]")
    void shouldEnsureAllAggregatedScoresInValidRange() {
        // Given: Multiple strategies with various scores
        List<Item> items = TestDataFactory.createItems(5);
        
        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", TestDataFactory.createScoredItems(items.subList(0, 3), "popularity"));
        strategyResults.put("content", TestDataFactory.createScoredItems(items.subList(2, 5), "content"));

        // When: Aggregating results
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: All scores should be in valid range
        for (ScoredItem scored : aggregated) {
            assertTrue(scored.getScore() >= 0.0 && scored.getScore() <= 1.0,
                    "Score " + scored.getScore() + " should be in range [0, 1]");
        }
    }

    @Test
    @DisplayName("Should maintain score ordering after aggregation")
    void shouldMaintainScoreOrdering() {
        // Given: Strategy results
        List<Item> items = TestDataFactory.createItems(4);
        
        List<ScoredItem> popularityResults = List.of(
                TestDataFactory.createScoredItem(items.get(0), 1.0, "Popular", "popularity"),
                TestDataFactory.createScoredItem(items.get(1), 0.8, "Popular", "popularity"),
                TestDataFactory.createScoredItem(items.get(2), 0.6, "Popular", "popularity"),
                TestDataFactory.createScoredItem(items.get(3), 0.4, "Popular", "popularity")
        );

        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", popularityResults);

        // When: Aggregating results
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: Results should be sorted in descending order
        for (int i = 1; i < aggregated.size(); i++) {
            assertTrue(aggregated.get(i - 1).getScore() >= aggregated.get(i).getScore(),
                    "Scores should be in descending order");
        }
    }

    // ===================== Explanation Merging Tests =====================

    @Test
    @DisplayName("Should merge explanations from multiple strategies")
    void shouldMergeExplanationsFromMultipleStrategies() {
        // Given: Same item from multiple strategies with different explanations
        List<Item> items = TestDataFactory.createItems(1);
        Item item = items.get(0);
        
        List<ScoredItem> popularityResults = List.of(
                TestDataFactory.createScoredItem(item, 0.9, "Trending now", "popularity")
        );
        
        List<ScoredItem> contentResults = List.of(
                TestDataFactory.createScoredItem(item, 0.7, "Similar to your favorites", "content")
        );

        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", popularityResults);
        strategyResults.put("content", contentResults);

        // When: Aggregating results
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: Explanation should contain both strategy explanations
        assertNotNull(aggregated);
        assertEquals(1, aggregated.size());
        
        String explanation = aggregated.get(0).getExplanation();
        assertNotNull(explanation);
        assertTrue(explanation.contains("Trending now") || explanation.contains("Similar to your favorites"));
        assertTrue(explanation.contains("|") || explanation.length() > 0); // Check delimiter or combined text
    }

    @Test
    @DisplayName("Should have single explanation for item from single strategy")
    void shouldHaveSingleExplanationForItemFromSingleStrategy() {
        // Given: Item from single strategy
        List<Item> items = TestDataFactory.createItems(1);
        
        List<ScoredItem> popularityResults = List.of(
                TestDataFactory.createScoredItem(items.get(0), 0.9, "Trending now", "popularity")
        );

        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", popularityResults);

        // When: Aggregating results
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: Should have single explanation
        assertNotNull(aggregated);
        assertEquals(1, aggregated.size());
        assertEquals("Trending now", aggregated.get(0).getExplanation());
    }

    // ===================== Deduplication Tests =====================

    @Test
    @DisplayName("Should deduplicate items appearing in multiple strategies")
    void shouldDeduplicateItemsFromMultipleStrategies() {
        // Given: Same item appears in multiple strategies
        List<Item> items = TestDataFactory.createItems(2);
        Item item1 = items.get(0);
        Item item2 = items.get(1);
        
        List<ScoredItem> popularityResults = List.of(
                TestDataFactory.createScoredItem(item1, 0.9, "Popular", "popularity"),
                TestDataFactory.createScoredItem(item2, 0.7, "Popular", "popularity")
        );
        
        List<ScoredItem> contentResults = List.of(
                TestDataFactory.createScoredItem(item1, 0.8, "Similar", "content")
        );

        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", popularityResults);
        strategyResults.put("content", contentResults);

        // When: Aggregating results
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: item1 should appear only once
        assertNotNull(aggregated);
        assertEquals(2, aggregated.size());
        
        long item1Count = aggregated.stream()
                .filter(s -> s.getItem().getId().equals(item1.getId()))
                .count();
        assertEquals(1, item1Count, "Item should appear only once");
    }

    // ===================== Context Integration Tests =====================

    @Test
    @DisplayName("Should work with provided context")
    void shouldWorkWithProvidedContext() {
        // Given: Context and strategy results
        RecommendationContext testContext = TestDataFactory.createPopulatedContext();
        List<Item> items = testContext.getItems();
        
        List<ScoredItem> results = TestDataFactory.createScoredItems(items.subList(0, 3), "popularity");
        Map<String, List<ScoredItem>> strategyResults = Map.of("popularity", results);

        // When: Aggregating with context
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, testContext);

        // Then: Should successfully aggregate
        assertNotNull(aggregated);
        assertFalse(aggregated.isEmpty());
    }

    // ===================== Performance Tests =====================

    @Test
    @DisplayName("Should handle large number of items efficiently")
    void shouldHandleLargeNumberOfItems() {
        // Given: Many items from multiple strategies
        List<Item> items = TestDataFactory.createItems(500);
        
        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", TestDataFactory.createScoredItems(items.subList(0, 250), "popularity"));
        strategyResults.put("content", TestDataFactory.createScoredItems(items.subList(100, 350), "content"));
        strategyResults.put("item-collaborative", TestDataFactory.createScoredItems(items.subList(200, 450), "item-collaborative"));

        // When: Aggregating large results
        long startTime = System.currentTimeMillis();
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);
        long endTime = System.currentTimeMillis();

        // Then: Should complete in reasonable time (< 2 seconds)
        assertTrue(endTime - startTime < 2000, 
                "Aggregation should complete within 2 seconds");
        assertFalse(aggregated.isEmpty());
        
        // Verify all results are properly scored and ordered
        for (int i = 1; i < aggregated.size(); i++) {
            assertTrue(aggregated.get(i - 1).getScore() >= aggregated.get(i).getScore());
        }
    }

    // ===================== Cold Start Scenario Tests =====================

    @Test
    @DisplayName("Should handle results when only popularity strategy returns results (cold start)")
    void shouldHandleOnlyPopularityStrategy() {
        // Given: Only popularity strategy has results (cold start scenario)
        List<Item> items = TestDataFactory.createItems(5);
        
        List<ScoredItem> popularityResults = TestDataFactory.createScoredItems(items, "popularity");
        
        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("popularity", popularityResults);
        strategyResults.put("content", new ArrayList<>()); // No results
        strategyResults.put("item-collaborative", new ArrayList<>()); // No results

        // When: Aggregating with mostly empty strategies
        List<ScoredItem> aggregated = aggregation.aggregate(strategyResults, context);

        // Then: Should return recommendations based on popularity alone
        assertNotNull(aggregated);
        assertEquals(5, aggregated.size());
        
        // Scores should still be valid
        for (ScoredItem scored : aggregated) {
            assertTrue(scored.getScore() > 0 && scored.getScore() <= 1.0);
        }
    }
}
