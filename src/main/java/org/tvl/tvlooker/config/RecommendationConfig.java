package org.tvl.tvlooker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.tvl.tvlooker.domain.motor.HybridRecommendationEngine;
import org.tvl.tvlooker.domain.motor.RecommendationEngine;
import org.tvl.tvlooker.domain.motor.utils.DataProvider;
import org.tvl.tvlooker.domain.strategy.aggregation.AggregationStrategy;
import org.tvl.tvlooker.domain.strategy.aggregation.ConstantConvexAggregation;
import org.tvl.tvlooker.domain.strategy.recommendation.PopularityStrategy;
import org.tvl.tvlooker.domain.strategy.recommendation.RecommendationStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class RecommendationConfig {

    // RECOMMENDATION ENGINE

    @Bean
    public RecommendationEngine recommendationEngine(
            List<RecommendationStrategy> strategies,
            AggregationStrategy aggregation,
            List<DataProvider<?>> dataProviders) {
        return new HybridRecommendationEngine(strategies, aggregation, dataProviders);
    }

    // RECOMMENDATION STRATEGIES

    @Bean
    @Order(1)
    @ConditionalOnProperty(
            name = "recommendation.strategies.popularity.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public RecommendationStrategy popularityStrategy() {
        return new PopularityStrategy();
    }

    // AGGREGATION STRATEGIES

    @Bean
    @Primary
    public AggregationStrategy aggregationStrategy(
            @Value("${recommendation.aggregation.type:constant}") String aggregationType,
            @Value("${recommendation.weights.popularity:0.15}") double weightPopularity,
            @Value("${recommendation.weights.content:0.25}") double weightContent,
            @Value("${recommendation.weights.item-collaborative:0.25}") double weightItemCollaborative,
            @Value("${recommendation.weights.user-collaborative:0.25}") double weightUserCollaborative,
            @Value("${recommendation.weights.matrix-factorization:0.10}") double weightMatrixFactorization) {
        
        Map<String, Double> weights = new HashMap<>();
        weights.put("popularity", weightPopularity);
        weights.put("content", weightContent);
        weights.put("item-collaborative", weightItemCollaborative);
        weights.put("user-collaborative", weightUserCollaborative);
        weights.put("matrix-factorization", weightMatrixFactorization);
        
        return switch (aggregationType) {
            case "constant" -> new ConstantConvexAggregation(weights);
            // Add more aggregation types here in the future:
            // case "variable" -> new VariableConvexAggregation(weights);
            // case "ranking" -> new RankingBasedAggregation();
            default -> new ConstantConvexAggregation(weights);
        };
    }

}

