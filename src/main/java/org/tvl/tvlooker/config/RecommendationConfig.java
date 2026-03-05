package org.tvl.tvlooker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tvl.tvlooker.domain.motor.HybridRecommendationEngine;
import org.tvl.tvlooker.domain.motor.RecommendationEngine;
import org.tvl.tvlooker.domain.strategy.aggregation_strategy.AggregationStrategy;
import org.tvl.tvlooker.domain.strategy.recommendation_strategy.RecommendationStrategy;

import java.util.List;

@Configuration
public class RecommendationConfig {

    @Bean
    public RecommendationEngine recommendationEngine(
            List<RecommendationStrategy> strategies,
            AggregationStrategy aggregation) {
        return new HybridRecommendationEngine(strategies, aggregation);
    }
