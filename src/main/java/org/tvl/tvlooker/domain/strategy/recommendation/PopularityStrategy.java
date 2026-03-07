package org.tvl.tvlooker.domain.strategy.recommendation;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PopularityStrategy generates recommendations based on the overall popularity of items.
 * It uses pre-computed popularity scores (e.g., from TMDB) to rank candidate items.
 * This strategy is simple but effective for recommending trending content.
 */
@Component
public class PopularityStrategy implements RecommendationStrategy {
    /**
     * Generates recommendations for a user.
     *
     * @param user           The user to generate recommendations for
     * @param candidateItems Pre-filtered items that are valid candidates for recommendation
     *                       (e.g., not already watched, meet content restrictions)
     * @param context        The recommendation context with ALL system data for computing features
     * @return List of scored items (can be empty if strategy has no recommendations)
     */
    @Override
    public List<ScoredItem> recommend(User user, List<Item> candidateItems, RecommendationContext context) {
        Map<Long, Double> popularityScores = context.getData("item-popularity",Map.class);
        return candidateItems.stream()
                .map(item -> ScoredItem.builder()
                        .item(item)
                        .score(popularityScores.getOrDefault(item.getId(), 0d))
                        .explanation("Trending on TMDB")
                        .sourceStrategy(getStrategyName())
                        .build())
                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Returns unique identifier for this strategy (used for logging and debugging).
     *
     * @return Strategy name (e.g., "collaborative-filtering", "content-based")
     */
    @Override
    public String getStrategyName() {
        return "popularity";
    }
}
