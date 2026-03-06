package org.tvl.tvlooker.domain.motor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.exception.NoRecommendationsAvailableException;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.strategy.aggregation_strategy.AggregationStrategy;
import org.tvl.tvlooker.domain.strategy.recommendation_strategy.RecommendationStrategy;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The HybridRecommendationEngine class is a concrete implementation of the RecommendationEngine interface that combines
 * multiple recommendation strategies and an evaluation strategy to generate item recommendations for users. It allows
 * for flexible configuration of strategies and can be extended to include various recommendation algorithms.
 */
@Setter
@Getter
@AllArgsConstructor
public class HybridRecommendationEngine implements RecommendationEngine {
    /** A list of recommendation strategies that the engine will use to generate recommendations. */
    private  List<RecommendationStrategy> strategies;

    /** An evaluation strategy that the engine will use to evaluate the effectiveness of the recommendations. */
    private AggregationStrategy aggregationStrategy;

    /** Logger for logging information and debugging purposes. */
    // TODO: Consider using a general logging framework and configuring it properly for the application.
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(HybridRecommendationEngine.class);

    /** Generates a list of scored items as recommendations for a given user based on the provided recommendation
     * context. This method combines the results from multiple recommendation strategies and applies an aggregation
     * strategy to produce the final list of recommendations. The implementation can be extended to include specific
     * logic for generating recommendations based on user interactions, preferences, and other relevant data.
     *
     * @param user    The user for whom the recommendations are being generated.
     * @param context The recommendation context that contains relevant data such as users, items, interactions, and
     *                registered data providers.
     * @return A list of scored items representing the recommended items for the user, along with their scores and
     *         explanations.
     */
    @Override
    public List<ScoredItem> recommend(User user, RecommendationContext context) {
        // 1. Validation
        validateInputs(user, context);

        // 2. Pre-filtering: Get candidate items
        List<Item> candidateItems = filterCandidates(user, context);

        // 3. Execute all strategies (with error handling)
        Map<String, List<ScoredItem>> strategyResults = executeStrategies(user, candidateItems, context);

        // 4. Aggregate results
        List<ScoredItem> aggregated = aggregationStrategy.aggregate(strategyResults, context);

        // 5. Post-processing
        return postProcess(aggregated);
    }

    /** Validates the inputs to the recommend method, ensuring that the user, context, strategies, and aggregation
     * strategy are all properly set before attempting to generate recommendations. This method throws appropriate
     * exceptions if any of the required inputs are missing or invalid.
     *
     * @param user    The user for whom the recommendations are being generated.
     * @param context The recommendation context that contains relevant data such as users, items, interactions, and
     *                registered data providers.
     * @throws IllegalArgumentException If the user or context is null, or if the context does not contain necessary
     * @throws IllegalStateException If the strategies list is null or empty, or if the aggregation strategy is not set.
     */
    private void validateInputs(User user, RecommendationContext context) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (context == null || context.checkDataNotNull()) {
            throw new IllegalArgumentException("RecommendationContext cannot be null");
        }
        if (strategies == null || strategies.isEmpty()) {
            throw new IllegalStateException("At least one recommendation strategy must be set");
        }
        if (aggregationStrategy == null) {
            throw new IllegalStateException("Aggregation strategy must be set");
        }
    }

    private List<Item> filterCandidates(User user, RecommendationContext context) {
        // For simplicity, we return all items as candidates.
        // In a real implementation, this would filter out items the user has already interacted with,
        // items that don't meet content restrictions, etc.
        return context.getItems();
    }

    /** Executes all configured recommendation strategies for the given user and candidate items, while handling any
     * exceptions that may occur during the execution of each strategy. The method collects the results from each
     * strategy and logs the outcomes, including any failures. If all strategies fail, it throws a
     * NoRecommendationsAvailableException.
     *
     * @param user       The user for whom the recommendations are being generated.
     * @param candidates The list of candidate items that are valid for recommendation.
     * @param context    The recommendation context that contains relevant data such as users, items, interactions, and
     *                   registered data providers.
     * @return A map where the key is the strategy name and the value is the list of scored items returned by that
     *         strategy.
     * @throws NoRecommendationsAvailableException If all recommendation strategies fail to produce results.
     */
    private Map<String, List<ScoredItem>> executeStrategies(
            User user,
            List<Item> candidates,
            RecommendationContext context) {

        Map<String, List<ScoredItem>> results = new HashMap<>();

        for (RecommendationStrategy strategy : strategies) {
            try {
                List<ScoredItem> strategyResult = strategy.recommend(user, candidates, context);
                results.put(strategy.getStrategyName(), strategyResult);

                logger.info("Strategy {} returned {} recommendations for user {}",
                        strategy.getStrategyName(), strategyResult.size(), user.getId());

            } catch (Exception e) {
                logger.warn("Strategy {} failed for user {}: {}",
                        strategy.getStrategyName(), user.getId(), e.getMessage());
                // Continue with other strategies
            }
        }

        // If ALL strategies failed, throw exception
        if (results.isEmpty()) {
            throw new NoRecommendationsAvailableException(
                    "All recommendation strategies failed for user " + user.getId());
        }

        return results;
    }

    /** Post-processes the aggregated list of scored items to ensure that there are no duplicate items and that only the
     * highest-scoring version of each item is retained. This method iterates through the list of scored items, checks
     * for duplicates based on the item, and removes any lower-scoring duplicates while keeping the one with the
     * highest score. The resulting list contains unique items with their best scores.
     *
     * @param items The list of scored items to be post-processed.
     * @return A list of scored items with duplicates removed, retaining only the highest-scoring version of each item.
     */
    private List<ScoredItem> postProcess(List<ScoredItem> items) {
        Map<Item, ScoredItem> seenItems = new HashMap<>();

        for (ScoredItem scoredItem : items) {
            if (scoredItem.getScore() < 0 || scoredItem.getScore() > 1) {
                logger.warn("Scored item {} has invalid score {}. Deleting.",
                        scoredItem.getItem().getId(), scoredItem.getScore());
                items.remove(scoredItem);
                continue;
            }


            Item item = scoredItem.getItem();

            if (seenItems.containsKey(item)) {
                ScoredItem existing = seenItems.get(item);
                if (scoredItem.getScore() > existing.getScore()) {
                    seenItems.put(item, scoredItem);
                    items.remove(existing);
                }
            } else {
                seenItems.put(item, scoredItem);
                items.remove(scoredItem);
            }
        }
        return items;
    }
}
