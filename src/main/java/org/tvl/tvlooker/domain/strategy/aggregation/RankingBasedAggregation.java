package org.tvl.tvlooker.domain.strategy.aggregation;

import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregation strategy based on Borda Count rank fusion.
 */
public class RankingBasedAggregation implements AggregationStrategy {

    @Override
    public List<ScoredItem> aggregate(
            Map<String, List<ScoredItem>> strategyResults,
            RecommendationContext context) {
        Objects.requireNonNull(strategyResults, "strategyResults cannot be null");

        if (strategyResults.isEmpty()) {
            return List.of();
        }

        int maxPossiblePoints = strategyResults.values().stream()
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();

        if (maxPossiblePoints == 0) {
            return List.of();
        }

        Map<Long, Item> itemsById = new HashMap<>();
        Map<Long, Integer> totalPointsByItemId = new HashMap<>();
        Map<Long, Set<String>> contributingStrategiesByItemId = new HashMap<>();

        for (Map.Entry<String, List<ScoredItem>> entry : strategyResults.entrySet()) {
            String strategyName = entry.getKey();
            List<ScoredItem> rankedItems = entry.getValue();

            if (rankedItems == null || rankedItems.isEmpty()) {
                continue;
            }

            int listSize = rankedItems.size();

            for (int rankIndex = 0; rankIndex < rankedItems.size(); rankIndex++) {
                ScoredItem scoredItem = rankedItems.get(rankIndex);
                if (scoredItem == null || scoredItem.getItem() == null || scoredItem.getItem().getId() == null) {
                    continue;
                }

                Item item = scoredItem.getItem();
                Long itemId = item.getId();

                // Borda formula with 1-based rank: points = N - rank + 1.
                // With 0-based index this becomes: points = N - rankIndex.
                int points = listSize - rankIndex;

                itemsById.putIfAbsent(itemId, item);
                totalPointsByItemId.merge(itemId, points, Integer::sum);
                contributingStrategiesByItemId
                        .computeIfAbsent(itemId, ignored -> new HashSet<>())
                        .add(strategyName);
            }
        }

        List<ScoredItem> result = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : totalPointsByItemId.entrySet()) {
            Long itemId = entry.getKey();
            int totalPoints = entry.getValue();
            int contributingStrategies = contributingStrategiesByItemId
                    .getOrDefault(itemId, Set.of())
                    .size();

            result.add(ScoredItem.builder()
                    .item(itemsById.get(itemId))
                    .score((double) totalPoints / maxPossiblePoints)
                    .explanation("Consensus from " + contributingStrategies + " strategies")
                    .sourceStrategy(getAggregationName())
                    .build());
        }

        return result.stream()
                .sorted(Comparator
                        .comparing(ScoredItem::getScore).reversed()
                        .thenComparing(scored -> scored.getItem().getId()))
                .toList();
    }

    @Override
    public String getAggregationName() {
        return "ranking-based-borda-count";
    }
}

