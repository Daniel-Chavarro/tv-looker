package org.tvl.tvlooker.domain.strategy.recommendation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tvl.tvlooker.domain.data_structure.ItemFeatureVector;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.exception.NoDataProviderException;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Content-Based recommendation strategy using TF-IDF and cosine similarity.
 */
public class ContentBasedStrategy implements RecommendationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ContentBasedStrategy.class);
    private static final String EXPLANATION = "Consistent with the type of content you usually enjoy";

    @Override
    public List<ScoredItem> recommend(User user, List<Item> candidateItems, RecommendationContext context) {
        if (user == null || candidateItems == null || candidateItems.isEmpty()) {
            return List.of();
        }

        Map<String, ItemFeatureVector> userProfiles;
        Map<Long, ItemFeatureVector> itemVectors;

        try {
            userProfiles = (Map<String, ItemFeatureVector>) context.getData("user-content-profiles", Map.class);
            itemVectors = (Map<Long, ItemFeatureVector>) context.getData("item-feature-vectors", Map.class);
        } catch (NoDataProviderException e) {
            logger.debug("Content providers not available, returning empty recommendations", e);
            return List.of();
        } catch (ClassCastException e) {
            logger.warn("Invalid content recommendation data in recommendation context, returning empty recommendations", e);
            return List.of();
        } catch (Exception e) {
            logger.warn("Unexpected error while loading content recommendation data, returning empty recommendations", e);
            return List.of();
        }

        if (userProfiles == null || itemVectors == null) {
            return List.of();
        }

        String userUuid = user.getId().toString();
        ItemFeatureVector userProfile = userProfiles.get(userUuid);

        // Cold start users (no history) -> return empty list
        if (userProfile == null) {
            return List.of();
        }

        return candidateItems.stream()
                .filter(item -> item.getId() != null && itemVectors.containsKey(item.getId()))
                .map(item -> {
                    ItemFeatureVector itemVector = itemVectors.get(item.getId());
                    double similarity = userProfile.cosineSimilarity(itemVector);

                    // Normalize to [0, 1]. Cosine similarity is [-1, 1], so we map it to [0, 1].
                    // Or since features are all non-negative (TF-IDF), cosine similarity will be [0, 1] naturally.
                    // Clamp similarity to [0, 1] for scoring.
                    // Negative similarities (for example from penalized user-profile weights) are treated as 0,
                    // rather than being remapped from [-1, 1] to [0, 1].
                    double normalizedScore = Math.max(0.0, Math.min(1.0, similarity));

                    return ScoredItem.builder()
                            .item(item)
                            .score(normalizedScore)
                            .explanation(EXPLANATION)
                            .sourceStrategy(getStrategyName())
                            .build();
                })
                .filter(scoredItem -> scoredItem.getScore() > 0)
                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "content-based";
    }
}
