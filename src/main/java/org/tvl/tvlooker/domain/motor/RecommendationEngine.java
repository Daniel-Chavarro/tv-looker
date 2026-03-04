package org.tvl.tvlooker.domain.motor;

import java.util.List;

import org.tvl.tvlooker.domain.model.Interaction;
import org.tvl.tvlooker.domain.model.Item;
import org.tvl.tvlooker.domain.model.RecommendationContext;
import org.tvl.tvlooker.domain.model.User;
import org.tvl.tvlooker.domain.strategy.evaluation_strategy.EvaluationStrategy;
import org.tvl.tvlooker.domain.strategy.recommendation_strategy.RecommendationStrategy;

/**
 * The RecommendationEngine interface defines the contract for a recommendation engine that generates item
 * recommendations for users based on their interactions and a given context. It includes methods for recommending
 * items, setting recommendation strategies, and setting an evaluation strategy.
 */
public interface RecommendationEngine {
    List<Item> recommend(User user, List<Interaction> interactions, RecommendationContext context);
    void setRecommendationStrategies(List<RecommendationStrategy> strategies);
    void setEvaluationStrategy(EvaluationStrategy evaluationStrategy);
    
}