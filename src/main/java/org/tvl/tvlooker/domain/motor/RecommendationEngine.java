package org.tvl.tvlooker.domain.motor;

import java.util.List;

import org.tvl.tvlooker.domain.model.Interaction;
import org.tvl.tvlooker.domain.model.Item;

public interface RecommendationEngine {
    public List<Item> recommend(User user, List<Interaction> interactions, RecommendationContext context);
    public void setRecommendationStrategies(List<RecommendationStrategy> strategies);
    public void setEvalationStrategy(EvaluationStrategy evaluationStrategy);
    
}