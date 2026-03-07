package org.tvl.tvlooker.domain.strategy.aggregation;

import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConstantConvexAggregation implements AggregationStrategy {

    private final Map<String, Double> strategyWeights;

    public ConstantConvexAggregation(Map<String, Double> strategyWeights) {
        this.strategyWeights = strategyWeights;
        
        // Validate weights sum to 1.0
        double totalWeight = strategyWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(totalWeight - 1.0) > 0.001) {
            throw new IllegalArgumentException("Weights must sum to 1. Current sum: " + totalWeight);
        }
    }

    /**
     * Combines results from multiple recommendation strategies into a single ranked list.
     *
     * @param strategyResults Map of strategy name to their scored results
     * @param context         The recommendation context (for accessing additional data if needed)
     * @return Final ranked list of recommendations
     */
    @Override
    public List<ScoredItem> aggregate(
            Map<String, List<ScoredItem>> strategyResults,
            RecommendationContext context) {

        // Collect all unique items
        Map<Long, Item> allItems = new HashMap<>();
        strategyResults.values().forEach(results ->
                results.forEach(scored ->
                        allItems.put(scored.getItem().getId(), scored.getItem())));

        // Compute weighted score for each item
        Map<Long, Double> itemScores = new HashMap<>();
        Map<Long, List<String>> itemExplanations = new HashMap<>();

        for (Long itemId : allItems.keySet()) {
            double finalScore = 0.0;
            List<String> explanations = new ArrayList<>();
            double activeWeightSum = 0.0;

            for (Map.Entry<String, List<ScoredItem>> entry : strategyResults.entrySet()) {
                String strategyName = entry.getKey();
                List<ScoredItem> results = entry.getValue();

                Optional<ScoredItem> scoredOpt = results.stream()
                        .filter(s -> s.getItem().getId().equals(itemId))
                        .findFirst();

                if (scoredOpt.isPresent()) {
                    ScoredItem scored = scoredOpt.get();
                    double weight = strategyWeights.getOrDefault(strategyName, 0.0);

                    finalScore += weight * scored.getScore();
                    activeWeightSum += weight;
                    explanations.add(scored.getExplanation());
                }
            }

            // Normalize by active weights
            if (activeWeightSum > 0) {
                finalScore = finalScore / activeWeightSum;
            }

            itemScores.put(itemId, finalScore);
            itemExplanations.put(itemId, explanations);
        }

        // Create final scored items
        return allItems.entrySet().stream()
                .map(entry -> {
                    Long itemId = entry.getKey();
                    Item item = entry.getValue();

                    return ScoredItem.builder()
                            .item(item)
                            .score(itemScores.get(itemId))
                            .explanation(String.join(" | ", itemExplanations.get(itemId)))
                            .sourceStrategy(getAggregationName())
                            .build();
                })
                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Returns unique identifier for this aggregation strategy.
     *
     * @return Aggregation name (e.g., "weighted-average", "borda-count")
     */
    @Override
    public String getAggregationName() {
        return "constant-convex-combination";
    }
}
