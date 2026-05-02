package org.tvl.tvlooker.domain.strategy.aggregation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RankingBasedAggregation Tests")
class RankingBasedAggregationTest {

    private final RankingBasedAggregation aggregation = new RankingBasedAggregation();
    private final RecommendationContext context = TestDataFactory.createPopulatedContext();

    @Test
    @DisplayName("Should have correct aggregation name")
    void shouldHaveCorrectAggregationName() {
        assertEquals("ranking-based-borda-count", aggregation.getAggregationName());
    }

    @Test
    @DisplayName("Should calculate Borda points and normalize scores with known ranking example")
    void shouldCalculateBordaPointsAndNormalizeScores() {
        List<Item> items = TestDataFactory.createItems(4);
        Item item1 = items.get(0);
        Item item2 = items.get(1);
        Item item3 = items.get(2);
        Item item4 = items.get(3);

        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("strategy-a", List.of(
                scored(item1, "A1", "strategy-a"),
                scored(item2, "A2", "strategy-a"),
                scored(item3, "A3", "strategy-a")
        ));
        strategyResults.put("strategy-b", List.of(
                scored(item2, "B1", "strategy-b"),
                scored(item1, "B2", "strategy-b"),
                scored(item4, "B3", "strategy-b")
        ));

        List<ScoredItem> result = aggregation.aggregate(strategyResults, context);

        // maxPossiblePoints = 3 + 3 = 6
        assertEquals(4, result.size());
        assertScoreById(result, item1.getId(), 5.0 / 6.0);
        assertScoreById(result, item2.getId(), 5.0 / 6.0);
        assertScoreById(result, item3.getId(), 1.0 / 6.0);
        assertScoreById(result, item4.getId(), 1.0 / 6.0);

        // Deterministic tie-breaking by item id ascending when scores are equal
        assertEquals(item1.getId(), result.get(0).getItem().getId());
        assertEquals(item2.getId(), result.get(1).getItem().getId());
        assertEquals("Consensus from 2 strategies", result.get(0).getExplanation());
        assertEquals("ranking-based-borda-count", result.get(0).getSourceStrategy());
    }

    @Test
    @DisplayName("Should handle lists with different lengths")
    void shouldHandleDifferentListLengths() {
        List<Item> items = TestDataFactory.createItems(5);
        Item item1 = items.get(0);
        Item item2 = items.get(1);
        Item item3 = items.get(2);
        Item item4 = items.get(3);
        Item item5 = items.get(4);

        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("strategy-a", List.of(
                scored(item1, "A1", "strategy-a"),
                scored(item2, "A2", "strategy-a"),
                scored(item3, "A3", "strategy-a"),
                scored(item4, "A4", "strategy-a")
        ));
        strategyResults.put("strategy-b", List.of(
                scored(item2, "B1", "strategy-b"),
                scored(item5, "B2", "strategy-b")
        ));

        List<ScoredItem> result = aggregation.aggregate(strategyResults, context);

        // maxPossiblePoints = 4 + 2 = 6
        assertEquals(5, result.size());
        assertScoreById(result, item1.getId(), 4.0 / 6.0);
        assertScoreById(result, item2.getId(), 5.0 / 6.0);
        assertScoreById(result, item3.getId(), 2.0 / 6.0);
        assertScoreById(result, item4.getId(), 1.0 / 6.0);
        assertScoreById(result, item5.getId(), 1.0 / 6.0);
    }

    @Test
    @DisplayName("Should return empty list when no ranked items are available")
    void shouldReturnEmptyListWhenNoRankedItems() {
        Map<String, List<ScoredItem>> strategyResults = new HashMap<>();
        strategyResults.put("strategy-a", List.of());
        strategyResults.put("strategy-b", null);

        List<ScoredItem> result = aggregation.aggregate(strategyResults, context);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should keep scores in range [0,1]")
    void shouldKeepScoresInValidRange() {
        List<Item> items = TestDataFactory.createItems(3);
        Map<String, List<ScoredItem>> strategyResults = Map.of(
                "strategy-a", List.of(scored(items.get(0), "x", "strategy-a"), scored(items.get(1), "y", "strategy-a")),
                "strategy-b", List.of(scored(items.get(1), "z", "strategy-b"), scored(items.get(2), "w", "strategy-b"))
        );

        List<ScoredItem> result = aggregation.aggregate(strategyResults, context);

        for (ScoredItem scoredItem : result) {
            assertTrue(scoredItem.getScore() >= 0.0);
            assertTrue(scoredItem.getScore() <= 1.0);
        }
    }

    private ScoredItem scored(Item item, String explanation, String strategyName) {
        return ScoredItem.builder()
                .item(item)
                .score(0.5)
                .explanation(explanation)
                .sourceStrategy(strategyName)
                .build();
    }

    private void assertScoreById(List<ScoredItem> results, Long itemId, double expected) {
        double score = results.stream()
                .filter(it -> it.getItem().getId().equals(itemId))
                .findFirst()
                .orElseThrow()
                .getScore();

        assertEquals(expected, score, 0.000001);
    }
}

