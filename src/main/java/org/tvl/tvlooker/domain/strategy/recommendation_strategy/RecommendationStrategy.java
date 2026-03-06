package org.tvl.tvlooker.domain.strategy.recommendation_strategy;

import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.List;

/**
 * Strategy interface for generating item recommendations.
 * Each implementation represents a different recommendation algorithm.
 */
public interface RecommendationStrategy {

    /**
     * Generates recommendations for a user.
     *
     * @param user The user to generate recommendations for
     * @param candidateItems Pre-filtered items that are valid candidates for recommendation
     *                       (e.g., not already watched, meet content restrictions)
     * @param context The recommendation context with ALL system data for computing features
     * @return List of scored items (can be empty if strategy has no recommendations)
     */
    List<ScoredItem> recommend(User user, List<Item> candidateItems, RecommendationContext context);

    /**
     * Returns unique identifier for this strategy (used for logging and debugging).
     *
     * @return Strategy name (e.g., "collaborative-filtering", "content-based")
     */
    String getStrategyName();
}