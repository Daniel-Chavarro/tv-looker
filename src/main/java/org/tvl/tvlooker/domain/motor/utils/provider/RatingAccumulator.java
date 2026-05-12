package org.tvl.tvlooker.domain.motor.utils.provider;

import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.dto.Review;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accumulates and aggregates ratings from user interactions.
 * Handles multiple ratings for the same user-item pair by averaging.
 */
public class RatingAccumulator {

    private final Map<String, Map<Integer, Double>> ratingSums = new HashMap<>();
    private final Map<String, Map<Integer, Integer>> ratingCounts = new HashMap<>();

    /**
     * Index mappings for matrix construction.
     */
    public record IndexMappings(
            Map<String, Integer> userIdToIndex,
            Map<Long, Integer> itemIdToIndex,
            Map<Integer, Long> indexToItemId) {
    }

    /**
     * Accumulates ratings from interactions using the provided index mappings.
     *
     * @param interactions list of user-item interactions
     * @param mappings index mappings for users and items
     * @param context recommendation context for looking up review scores
     */
    public void accumulate(List<Interaction> interactions,
                           IndexMappings mappings,
                           RecommendationContext context) {
        for (Interaction interaction : interactions) {
            if (interaction.getInteractionType() != InteractionType.RATING) {
                continue;
            }
            if (interaction.getUserId() == null || interaction.getItemId() == null) {
                continue;
            }
            String userUuid = interaction.getUserId().toString();
            Long itemId = interaction.getItemId();

            Integer uIdx = mappings.userIdToIndex().get(userUuid);
            Integer iIdx = mappings.itemIdToIndex().get(itemId);
            if (uIdx == null || iIdx == null) {
                continue;
            }

            double rating = extractRating(interaction, context);

            ratingSums.computeIfAbsent(userUuid, k -> new HashMap<>())
                    .merge(iIdx, rating, Double::sum);
            ratingCounts.computeIfAbsent(userUuid, k -> new HashMap<>())
                    .merge(iIdx, 1, Integer::sum);
        }
    }

    private double extractRating(Interaction interaction, RecommendationContext context) {
        if (interaction.getReviewId() != null) {
            return lookupReviewScore(context, interaction.getReviewId());
        }
        return 3.0;
    }

    private double lookupReviewScore(RecommendationContext context, Long reviewId) {
        if (context.getReviews() != null && reviewId != null) {
            for (Review review : context.getReviews()) {
                if (java.util.Objects.equals(review.getId(), reviewId)) {
                    return review.getScore() != null ? review.getScore() : 3.0;
                }
            }
        }
        return 3.0;
    }

    /**
     * Computes the global mean rating across all accumulated ratings.
     *
     * @return global mean rating, or 3.0 if no ratings
     */
    public double computeGlobalMean() {
        double totalSum = 0.0;
        int totalCount = 0;

        for (Map.Entry<String, Map<Integer, Double>> userEntry : ratingSums.entrySet()) {
            String userUuid = userEntry.getKey();
            Map<Integer, Integer> counts = ratingCounts.getOrDefault(userUuid, Map.of());

            for (Map.Entry<Integer, Double> ratingEntry : userEntry.getValue().entrySet()) {
                Integer count = counts.get(ratingEntry.getKey());
                if (count != null) {
                    totalSum += ratingEntry.getValue();
                    totalCount += count;
                }
            }
        }

        return totalCount > 0 ? totalSum / totalCount : 3.0;
    }

    public Map<Integer, Double> getUserRatings(String userUuid) {
        return ratingSums.getOrDefault(userUuid, Map.of());
    }

    public Map<Integer, Integer> getUserCounts(String userUuid) {
        return ratingCounts.getOrDefault(userUuid, Map.of());
    }
}
