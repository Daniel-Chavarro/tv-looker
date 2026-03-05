package org.tvl.tvlooker.domain.motor;

import java.util.List;

import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.strategy.aggregation_strategy.AggregationStrategy;
import org.tvl.tvlooker.domain.strategy.recommendation_strategy.RecommendationStrategy;

/**
 * The RecommendationEngine interface defines the contract for a recommendation engine that generates item
 * recommendations for users based on their interactions and a given context. It includes methods for recommending
 * items, setting recommendation strategies, and setting an evaluation strategy.
 */
public interface RecommendationEngine {
    List<ScoredItem> recommend(User user, RecommendationContext context);
    void setStrategies(List<RecommendationStrategy> strategies);
    void setAggregationStrategy(AggregationStrategy aggregationStrategy);
}