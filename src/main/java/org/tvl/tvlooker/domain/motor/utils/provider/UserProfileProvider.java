package org.tvl.tvlooker.domain.motor.utils.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.data_structure.ItemFeatureVector;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.dto.Review;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.motor.utils.DataProvider;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes aggregated user content profiles.
 *
 * Provider ID: "user-content-profiles"
 * Cache: 1h
 */
@Component
public class UserProfileProvider implements DataProvider<Map<String, ItemFeatureVector>> {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileProvider.class);

    @Override
    public String getProviderId() {
        return "user-content-profiles";
    }

    @Override
    public long getCacheExpirationSeconds() {
        return 3600; // 1 hour
    }

    @Override
    public Map<String, ItemFeatureVector> provide(RecommendationContext context) {
        Map<String, ItemFeatureVector> userProfiles = new HashMap<>();

        Map<Long, ItemFeatureVector> itemVectors;
        try {
            // Unchecked cast handling via Java types
            Object obj = context.getData("item-feature-vectors", Map.class);
            itemVectors = (Map<Long, ItemFeatureVector>) obj;
        } catch (org.tvl.tvlooker.domain.exception.NoDataProviderException e) {
            itemVectors = new HashMap<>();
        } catch (Exception e) {
            // Log unexpected exceptions and fallback to an empty map
            logger.error("Unexpected exception fetching item vectors", e);
            itemVectors = new HashMap<>();
        }

        if (itemVectors == null || itemVectors.isEmpty() || context.getInteractions() == null) {
            return userProfiles;
        }

        Map<Long, Double> reviewScoreMap = buildReviewScoreMap(context);
        Map<String, Map<Long, Double>> userItemWeights = new HashMap<>();

        for (Interaction interaction : context.getInteractions()) {
            if (interaction.getUserId() == null || interaction.getItemId() == null) {
                continue;
            }

            String userUuid = interaction.getUserId().toString();
            Long itemId = interaction.getItemId();

            double weight = computeWeight(interaction, reviewScoreMap);

            if (weight != 0.0) {
                userItemWeights.computeIfAbsent(userUuid, k -> new HashMap<>())
                        .merge(itemId, weight, Double::sum);
            }
        }

        for (Map.Entry<String, Map<Long, Double>> entry : userItemWeights.entrySet()) {
            String userUuid = entry.getKey();
            ItemFeatureVector profile = ItemFeatureVector.builder().build();

            for (Map.Entry<Long, Double> itemWeight : entry.getValue().entrySet()) {
                Long itemId = itemWeight.getKey();
                Double weight = itemWeight.getValue();

                ItemFeatureVector itemVector = itemVectors.get(itemId);
                if (itemVector != null) {
                    profile.addVectorWithWeight(itemVector, weight);
                }
            }

            userProfiles.put(userUuid, profile);
        }

        return userProfiles;
    }

    private Map<Long, Double> buildReviewScoreMap(RecommendationContext context) {
        if (context.getReviews() == null) {
            return Map.of();
        }
        Map<Long, Double> map = new HashMap<>();
        for (Review review : context.getReviews()) {
            if (review.getId() != null) {
                map.put(review.getId(), review.getScore() != null ? review.getScore() : 4.0);
            }
        }
        return map;
    }

    private double computeWeight(Interaction interaction,
                                  Map<Long, Double> reviewScoreMap) {
        if (interaction.getInteractionType() == InteractionType.LIKE) {
            return 5.0;
        } else if (interaction.getInteractionType() == InteractionType.VIEW) {
            return 3.0;
        } else if (interaction.getInteractionType() == InteractionType.RATING) {
            double score = 4.0;
            if (interaction.getReviewId() != null && reviewScoreMap != null) {
                Double mappedScore = reviewScoreMap.get(interaction.getReviewId());
                if (mappedScore != null) {
                    score = mappedScore;
                }
            }

            if (score >= 4.0) {
                return score;
            } else if (score <= 2.0) {
                return -score;
            }
            return 0.0;
        }
        return 0.0;
    }
}
