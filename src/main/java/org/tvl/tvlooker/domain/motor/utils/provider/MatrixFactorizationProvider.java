package org.tvl.tvlooker.domain.motor.utils.provider;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.data_structure.SVDFactors;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.motor.utils.DataProvider;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data provider that computes SVD factors from the user-item rating matrix.
 * Uses Apache Commons Math SingularValueDecomposition.
 *
 * Decomposition: R ≈ U × Σ × V^T
 * After reduction to k factors: userFactors = U_k × Σ_k, itemFactors = V_k × Σ_k
 * Prediction: score = userVector · itemVector
 */
@Component
public class MatrixFactorizationProvider implements DataProvider<SVDFactors> {

    private static final Logger logger = LoggerFactory.getLogger(MatrixFactorizationProvider.class);

    private final int latentFactors;

    public MatrixFactorizationProvider(
            @Value("${recommendation.mf.latent-factors:50}") int latentFactors) {
        this.latentFactors = latentFactors;
    }

    @Override
    public String getProviderId() {
        return "svd-factors";
    }

    @Override
    public SVDFactors provide(RecommendationContext context) {
        List<Interaction> interactions = context.getInteractions();
        if (interactions == null || interactions.isEmpty()) {
            logger.warn("No interactions available for SVD computation");
            return buildEmptyFactors();
        }

        return computeSVDFactors(interactions, context);
    }

    @Override
    public long getCacheExpirationSeconds() {
        return 604800; // 1 week
    }

    /**
     * Builds the user-item rating matrix and performs SVD decomposition.
     */
    private SVDFactors computeSVDFactors(List<Interaction> interactions, RecommendationContext context) {
        IndexMappings mappings = buildIndexMappings(interactions);
        
        if (mappings.isEmpty()) {
            logger.warn("No rating interactions found for SVD computation");
            return buildEmptyFactors();
        }

        logger.info("Building rating matrix: {} users × {} items, k={}", 
                mappings.numUsers(), mappings.numItems(), latentFactors);

        RatingData ratingData = collectAndNormalizeRatings(interactions, context, mappings);
        
        return performSVDAndBuildFactors(ratingData, mappings);
    }

    /**
     * Builds index mappings from user UUIDs and item IDs to matrix indices.
     */
    private IndexMappings buildIndexMappings(List<Interaction> interactions) {
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

        return new IndexMappings(userIdToIndex, itemIdToIndex, indexToItemId);
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

    /**
     * Collects ratings from interactions and normalizes them.
     */
    private RatingData collectAndNormalizeRatings(List<Interaction> interactions, 
                                                   RecommendationContext context,
                                                   IndexMappings mappings) {
        Map<String, Map<Integer, Double>> ratingAccumulator = new HashMap<>();
        Map<String, Map<Integer, Integer>> ratingCounts = new HashMap<>();

        accumulateRatings(interactions, context, mappings, ratingAccumulator, ratingCounts);

        double globalMean = computeGlobalMean(ratingAccumulator, ratingCounts);
        double[][] ratingMatrix = fillRatingMatrix(ratingAccumulator, ratingCounts, mappings, globalMean);

        return new RatingData(ratingMatrix, globalMean);
    }

    private void accumulateRatings(List<Interaction> interactions, RecommendationContext context,
                                      IndexMappings mappings,
                                      Map<String, Map<Integer, Double>> ratingAccumulator,
                                      Map<String, Map<Integer, Integer>> ratingCounts) {
        for (Interaction interaction : interactions) {
            if (interaction.getInteractionType() != InteractionType.RATING) {
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

            ratingAccumulator
                    .computeIfAbsent(userUuid, k -> new HashMap<>())
                    .merge(iIdx, rating, Double::sum);
            ratingCounts
                    .computeIfAbsent(userUuid, k -> new HashMap<>())
                    .merge(iIdx, 1, Integer::sum);
        }
    }

    private double extractRating(Interaction interaction, RecommendationContext context) {
        if (interaction.getReviewId() != null) {
            return lookupReviewScore(context, interaction.getReviewId());
        }
        return 3.0;
    }

    private double[][] fillRatingMatrix(Map<String, Map<Integer, Double>> ratingAccumulator,
                                         Map<String, Map<Integer, Integer>> ratingCounts,
                                         IndexMappings mappings, double globalMean) {
        double[][] ratingData = new double[mappings.numUsers()][mappings.numItems()];

        for (Map.Entry<String, Integer> userEntry : mappings.userIdToIndex().entrySet()) {
            String userUuid = userEntry.getKey();
            int uIdx = userEntry.getValue();

            Map<Integer, Double> userRatings = ratingAccumulator.getOrDefault(userUuid, Map.of());
            Map<Integer, Integer> userCounts = ratingCounts.getOrDefault(userUuid, Map.of());
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
                              IndexMappings mappings, double userMean) {
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

    /**
     * Performs SVD and builds the factor matrices.
     */
    private SVDFactors performSVDAndBuildFactors(RatingData ratingData, IndexMappings mappings) {
        RealMatrix ratingMatrix = new Array2DRowRealMatrix(ratingData.matrix(), false);
        SingularValueDecomposition svd = new SingularValueDecomposition(ratingMatrix);

        int k = Math.min(latentFactors, Math.min(mappings.numUsers(), mappings.numItems()));

        double[][] userFactors = extractUserFactors(svd, mappings.numUsers(), k);
        double[][] itemFactors = extractItemFactors(svd, mappings.numItems(), k);
        double[] reducedSingularValues = Arrays.copyOf(svd.getSingularValues(), k);

        logger.info("SVD computation complete: {} latent factors extracted", k);

        return SVDFactors.builder()
                .userFactors(userFactors)
                .itemFactors(itemFactors)
                .singularValues(reducedSingularValues)
                .userIdToIndex(mappings.userIdToIndex())
                .itemIdToIndex(mappings.itemIdToIndex())
                .indexToItemId(mappings.indexToItemId())
                .computedAt(System.currentTimeMillis())
                .latentFactors(k)
                .build();
    }

    private double[][] extractUserFactors(SingularValueDecomposition svd, int numUsers, int k) {
        double[][] userFactors = new double[numUsers][k];
        RealMatrix uMatrix = svd.getU();
        double[] singularValues = svd.getSingularValues();

        for (int i = 0; i < numUsers; i++) {
            for (int j = 0; j < k; j++) {
                userFactors[i][j] = uMatrix.getEntry(i, j) * singularValues[j];
            }
        }
        return userFactors;
    }

    private double[][] extractItemFactors(SingularValueDecomposition svd, int numItems, int k) {
        double[][] itemFactors = new double[numItems][k];
        RealMatrix vMatrix = svd.getV();
        double[] singularValues = svd.getSingularValues();

        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < k; j++) {
                itemFactors[i][j] = vMatrix.getEntry(i, j) * singularValues[j];
            }
        }
        return itemFactors;
    }

    /**
     * Looks up the review score from the context.
     * Falls back to 3.0 (neutral) if not found.
     */
    private double lookupReviewScore(RecommendationContext context, Long reviewId) {
        // Since reviews aren't directly in the context, we return a neutral default.
        // In a production system, this would query the review repository.
        return 3.0;
    }

    /**
     * Computes the global mean rating across all accumulated ratings.
     */
    private double computeGlobalMean(
            Map<String, Map<Integer, Double>> ratingAccumulator,
            Map<String, Map<Integer, Integer>> ratingCounts) {

        double totalSum = 0.0;
        int totalCount = 0;

        for (Map.Entry<String, Map<Integer, Double>> userEntry : ratingAccumulator.entrySet()) {
            String userUuid = userEntry.getKey();
            Map<Integer, Integer> counts = ratingCounts.getOrDefault(userUuid, Map.of());
            double[] userSums = accumulateUserSums(userEntry.getValue(), counts);
            totalSum += userSums[0];
            totalCount += (int) userSums[1];
        }

        return totalCount > 0 ? totalSum / totalCount : 3.0;
    }

    private double[] accumulateUserSums(Map<Integer, Double> ratings, Map<Integer, Integer> counts) {
        double sum = 0.0;
        int count = 0;
        for (Map.Entry<Integer, Double> ratingEntry : ratings.entrySet()) {
            Integer itemCount = counts.get(ratingEntry.getKey());
            if (itemCount != null) {
                sum += ratingEntry.getValue();
                count += itemCount;
            }
        }
        return new double[]{sum, count};
    }

    /**
     * Builds an empty SVDFactors instance for cases with no data.
     */
    private SVDFactors buildEmptyFactors() {
        return SVDFactors.builder()
                .userFactors(new double[0][0])
                .itemFactors(new double[0][0])
                .singularValues(new double[0])
                .userIdToIndex(Map.of())
                .itemIdToIndex(Map.of())
                .indexToItemId(Map.of())
                .computedAt(System.currentTimeMillis())
                .latentFactors(0)
                .build();
    }

    /**
     * Record holding index mappings for users and items.
     */
    private record IndexMappings(
            Map<String, Integer> userIdToIndex,
            Map<Long, Integer> itemIdToIndex,
            Map<Integer, Long> indexToItemId) {

        boolean isEmpty() {
            return userIdToIndex.isEmpty() || itemIdToIndex.isEmpty();
        }

        int numUsers() {
            return userIdToIndex.size();
        }

        int numItems() {
            return itemIdToIndex.size();
        }
    }

    /**
     * Record holding the rating matrix data.
     */
    private record RatingData(double[][] matrix, double globalMean) {
    }
}
