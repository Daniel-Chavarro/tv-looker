package org.tvl.tvlooker.domain.motor.utils.provider;

import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the user-item rating matrix from interactions.
 * Extracted from MatrixFactorizationProvider to reduce God Class violations.
 */
public class RatingMatrixBuilder {

    private final RecommendationContext context;

    public RatingMatrixBuilder(RecommendationContext context) {
        this.context = context;
    }

    /**
     * Result containing the built rating matrix and index mappings.
     */
    public record RatingMatrixResult(
            double[][] matrix,
            Map<String, Integer> userIdToIndex,
            Map<Long, Integer> itemIdToIndex,
            Map<Integer, Long> indexToItemId) {

        public boolean isEmpty() {
            return userIdToIndex.isEmpty() || itemIdToIndex.isEmpty();
        }

        public int numUsers() {
            return userIdToIndex.size();
        }

        public int numItems() {
            return itemIdToIndex.size();
        }
    }

    /**
     * Builds the rating matrix from rating interactions.
     *
     * @param interactions list of user-item interactions
     * @return RatingMatrixResult containing the matrix and mappings
     */
    public RatingMatrixResult build(List<Interaction> interactions) {
        RatingAccumulator.IndexMappings mappings = buildIndexMappings(interactions);

        if (mappings.userIdToIndex().isEmpty() || mappings.itemIdToIndex().isEmpty()) {
            return new RatingMatrixResult(
                    new double[0][0],
                    mappings.userIdToIndex(),
                    mappings.itemIdToIndex(),
                    mappings.indexToItemId()
            );
        }

        double[][] matrix = fillRatingMatrix(interactions, mappings);

        return new RatingMatrixResult(
                matrix,
                mappings.userIdToIndex(),
                mappings.itemIdToIndex(),
                mappings.indexToItemId()
        );
    }

    private RatingAccumulator.IndexMappings buildIndexMappings(List<Interaction> interactions) {
        Map<String, Integer> userIdToIndex = new HashMap<>();
        Map<Long, Integer> itemIdToIndex = new HashMap<>();
        Map<Integer, Long> indexToItemId = new HashMap<>();

        int userIndex = 0;
        int itemIndex = 0;

        for (Interaction interaction : interactions) {
            if (interaction.getInteractionType() != InteractionType.RATING) {
                continue;
            }
            String userUuid = interaction.getUserId().toString();
            Long itemId = interaction.getItemId();

            userIndex = addUserIfAbsent(userIdToIndex, userUuid, userIndex);
            itemIndex = addItemIfAbsent(itemIdToIndex, indexToItemId, itemId, itemIndex);
        }

        return new RatingAccumulator.IndexMappings(userIdToIndex, itemIdToIndex, indexToItemId);
    }

    private int addUserIfAbsent(Map<String, Integer> userIdToIndex, String userUuid, int currentIndex) {
        if (!userIdToIndex.containsKey(userUuid)) {
            userIdToIndex.put(userUuid, currentIndex);
            return currentIndex + 1;
        }
        return currentIndex;
    }

    private int addItemIfAbsent(Map<Long, Integer> itemIdToIndex, Map<Integer, Long> indexToItemId,
                                 Long itemId, int currentIndex) {
        if (!itemIdToIndex.containsKey(itemId)) {
            itemIdToIndex.put(itemId, currentIndex);
            indexToItemId.put(currentIndex, itemId);
            return currentIndex + 1;
        }
        return currentIndex;
    }

    private double[][] fillRatingMatrix(List<Interaction> interactions, RatingAccumulator.IndexMappings mappings) {
        RatingAccumulator accumulator = new RatingAccumulator();
        accumulator.accumulate(interactions, mappings, context);

        int numUsers = mappings.userIdToIndex().size();
        int numItems = mappings.itemIdToIndex().size();
        double[][] ratingData = new double[numUsers][numItems];

        double globalMean = accumulator.computeGlobalMean();

        for (Map.Entry<String, Integer> userEntry : mappings.userIdToIndex().entrySet()) {
            String userUuid = userEntry.getKey();
            int uIdx = userEntry.getValue();

            Map<Integer, Double> userRatings = accumulator.getUserRatings(userUuid);
            Map<Integer, Integer> userCounts = accumulator.getUserCounts(userUuid);
            double userMean = computeUserMean(userRatings, userCounts, globalMean);

            fillUserRow(ratingData, uIdx, userRatings, userCounts, mappings, userMean);
        }

        return ratingData;
    }

    private double computeUserMean(Map<Integer, Double> userRatings,
                                    Map<Integer, Integer> userCounts, double globalMean) {
        if (userRatings.isEmpty()) {
            return globalMean;
        }
        double sum = userRatings.values().stream().mapToDouble(Double::doubleValue).sum();
        int count = userCounts.values().stream().mapToInt(Integer::intValue).sum();
        return count > 0 ? sum / count : globalMean;
    }

    private void fillUserRow(double[][] ratingData, int uIdx,
                              Map<Integer, Double> userRatings,
                              Map<Integer, Integer> userCounts,
                              RatingAccumulator.IndexMappings mappings, double userMean) {
        for (Map.Entry<Long, Integer> itemEntry : mappings.itemIdToIndex().entrySet()) {
            int iIdx = itemEntry.getValue();
            Double totalRating = userRatings.get(iIdx);
            Integer count = userCounts.get(iIdx);

            if (totalRating != null && count != null) {
                ratingData[uIdx][iIdx] = (totalRating / count) - userMean;
            } else {
                ratingData[uIdx][iIdx] = 0.0;
            }
        }
    }
}
