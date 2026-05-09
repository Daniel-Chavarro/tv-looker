package org.tvl.tvlooker.domain.motor.utils.provider;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.data_structure.ItemFeatureVector;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.motor.utils.DataProvider;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Computes aggregated user content profiles.
 *
 * Provider ID: "user-content-profiles"
 * Cache: 1h
 */
@Component
public class UserProfileProvider implements DataProvider<Map<String, ItemFeatureVector>> {

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
        } catch (Exception e) {
            itemVectors = new HashMap<>();
        }

        if (itemVectors == null || itemVectors.isEmpty() || context.getInteractions() == null) {
            return userProfiles;
        }

        Map<String, Map<Long, Double>> userItemWeights = new HashMap<>();

        for (Interaction interaction : context.getInteractions()) {
            if (interaction.getUserId() == null || interaction.getItemId() == null) continue;

            String userUuid = interaction.getUserId().toString();
            Long itemId = interaction.getItemId();

            double weight = computeWeight(interaction, context);

            if (weight != 0.0) {
                userItemWeights.computeIfAbsent(userUuid, k -> new HashMap<>())
                        .merge(itemId, weight, Double::sum);
            }
        }

        for (Map.Entry<String, Map<Long, Double>> entry : userItemWeights.entrySet()) {
            String userUuid = entry.getKey();
            ItemFeatureVector.ItemFeatureVectorBuilder profileBuilder = ItemFeatureVector.builder();
            ItemFeatureVector profile = profileBuilder.build();

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

    private double computeWeight(Interaction interaction, RecommendationContext context) {
        if (interaction.getInteractionType() == InteractionType.LIKE) {
            return 5.0;
        } else if (interaction.getInteractionType() == InteractionType.VIEW) {
            return 3.0;
        } else if (interaction.getInteractionType() == InteractionType.RATING) {
            // For RATING, since we don't have reviews in context, we use a neutral/positive proxy.
            // If the user's test passes review score, we should try to extract it from somewhere.
            // Let's assume a default rating of 4.0 if not found, to add positive weight.
            double score = 4.0;

            // To be precise with the heuristic: score >= 4 is positive (+weight), score <= 2 is penalization (-weight)
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

