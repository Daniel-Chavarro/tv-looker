package org.tvl.tvlooker.config;

import org.springframework.beans.factory.annotation.Value;
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

import java.util.List;

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
    public RecommendationStrategy popularityStrategy() {
        return new PopularityStrategy();
    }

    // AGGREGATION STRATEGIES

    @Bean
    @Primary
    public AggregationStrategy defaultAggregation(
            @Value("${recommendation.aggregation.type:constant}") String aggregationName){
        return switch (aggregationName){
            case "constant" -> constantConvexAggregation();
            default -> constantConvexAggregation();
        };
    }

    @Bean
    public AggregationStrategy constantConvexAggregation() {
        return new ConstantConvexAggregation();
    }

}

