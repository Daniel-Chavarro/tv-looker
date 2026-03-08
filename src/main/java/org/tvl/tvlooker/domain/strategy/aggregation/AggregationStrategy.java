package org.tvl.tvlooker.domain.strategy.aggregation;

import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for aggregating results from multiple recommendation strategies.
 */
public interface AggregationStrategy {

    /**
     * Combines results from multiple recommendation strategies into a single ranked list.
     *
     * @param strategyResults Map of strategy name to their scored results
     * @param context The recommendation context (for accessing additional data if needed)
     * @return Final ranked list of recommendations
     */
    List<ScoredItem> aggregate(Map<String, List<ScoredItem>> strategyResults, RecommendationContext context);

    /**
     * Returns unique identifier for this aggregation strategy.
     *
     * @return Aggregation name (e.g., "weighted-average", "borda-count")
     */
    String getAggregationName();
}