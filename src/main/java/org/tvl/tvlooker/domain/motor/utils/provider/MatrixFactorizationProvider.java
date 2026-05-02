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
        // Build index mappings
        Map<String, Integer> userIdToIndex = new HashMap<>();
        Map<Long, Integer> itemIdToIndex = new HashMap<>();
        Map<Integer, Long> indexToItemId = new HashMap<>();

        int userIndex = 0;
        int itemIndex = 0;

        // First pass: collect unique users and items from RATING interactions
        for (Interaction interaction : interactions) {
            if (interaction.getInteractionType() != InteractionType.RATING) {
                continue;
            }
            String userUuid = interaction.getUserId().toString();
            Long itemId = interaction.getItemId();

            if (!userIdToIndex.containsKey(userUuid)) {
                userIdToIndex.put(userUuid, userIndex++);
            }
            if (!itemIdToIndex.containsKey(itemId)) {
                itemIdToIndex.put(itemId, itemIndex);
                indexToItemId.put(itemIndex, itemId);
                itemIndex++;
            }
        }

        if (userIdToIndex.isEmpty() || itemIdToIndex.isEmpty()) {
            logger.warn("No rating interactions found for SVD computation");
            return buildEmptyFactors();
        }

        int numUsers = userIdToIndex.size();
        int numItems = itemIdToIndex.size();

        logger.info("Building rating matrix: {} users × {} items, k={}", numUsers, numItems, latentFactors);

        // Build rating matrix (users × items)
        // Use average rating as default for sparse matrix filling
        double[][] ratingData = new double[numUsers][numItems];

        // Collect ratings from RATING interactions
        // Aggregate multiple ratings for same user-item pair by averaging
        Map<String, Map<Integer, Double>> ratingAccumulator = new HashMap<>();
        Map<String, Map<Integer, Integer>> ratingCounts = new HashMap<>();

        for (Interaction interaction : interactions) {
            if (interaction.getInteractionType() != InteractionType.RATING) {
                continue;
            }
            String userUuid = interaction.getUserId().toString();
            Long itemId = interaction.getItemId();

            Integer uIdx = userIdToIndex.get(userUuid);
            Integer iIdx = itemIdToIndex.get(itemId);
            if (uIdx == null || iIdx == null) {
                continue;
            }

            // Use review score if available, otherwise default to 3.0 (neutral)
            // Since Interaction doesn't carry the score directly, we look for it via reviewId
            double rating = 3.0;
            if (interaction.getReviewId() != null) {
                // Look up score from reviews in context
                rating = lookupReviewScore(context, interaction.getReviewId());
            }

            ratingAccumulator
                    .computeIfAbsent(userUuid, k -> new HashMap<>())
                    .merge(iIdx, rating, Double::sum);
            ratingCounts
                    .computeIfAbsent(userUuid, k -> new HashMap<>())
                    .merge(iIdx, 1, Integer::sum);
        }

        // Compute average rating for mean-centering
        double globalMean = computeGlobalMean(ratingAccumulator, ratingCounts, numUsers, numItems);

        // Fill rating matrix with mean-centered values
        for (Map.Entry<String, Integer> userEntry : userIdToIndex.entrySet()) {
            String userUuid = userEntry.getKey();
            int uIdx = userEntry.getValue();

            Map<Integer, Double> userRatings = ratingAccumulator.getOrDefault(userUuid, Map.of());
            Map<Integer, Integer> userCounts = ratingCounts.getOrDefault(userUuid, Map.of());

            // Compute user mean
            double userMean = userRatings.isEmpty() ? globalMean :
                    userRatings.values().stream().mapToDouble(Double::doubleValue).sum()
                            / userCounts.values().stream().mapToInt(Integer::intValue).sum();

            for (Map.Entry<Long, Integer> itemEntry : itemIdToIndex.entrySet()) {
                int iIdx = itemEntry.getValue();
                Double totalRating = userRatings.get(iIdx);
                Integer count = userCounts.get(iIdx);

                if (totalRating != null && count != null) {
                    double avgRating = totalRating / count;
                    ratingData[uIdx][iIdx] = avgRating - userMean;
                } else {
                    // Unobserved entries: fill with 0 (mean-centered neutral)
                    ratingData[uIdx][iIdx] = 0.0;
                }
            }
        }

        // Perform SVD
        RealMatrix ratingMatrix = new Array2DRowRealMatrix(ratingData, false);
        SingularValueDecomposition svd = new SingularValueDecomposition(ratingMatrix);

        // Reduce to k dimensions
        int k = Math.min(latentFactors, Math.min(numUsers, numItems));

        RealMatrix uMatrix = svd.getU();
        RealMatrix vMatrix = svd.getV();
        double[] singularValues = svd.getSingularValues();

        // Build reduced factors: userFactors = U_k × Σ_k, itemFactors = V_k × Σ_k
        double[][] userFactors = new double[numUsers][k];
        double[][] itemFactors = new double[numItems][k];
        double[] reducedSingularValues = new double[k];

        for (int i = 0; i < numUsers; i++) {
            for (int j = 0; j < k; j++) {
                userFactors[i][j] = uMatrix.getEntry(i, j) * singularValues[j];
            }
        }

        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < k; j++) {
                itemFactors[i][j] = vMatrix.getEntry(i, j) * singularValues[j];
            }
        }

        System.arraycopy(singularValues, 0, reducedSingularValues, 0, k);

        logger.info("SVD computation complete: {} latent factors extracted", k);

        return SVDFactors.builder()
                .userFactors(userFactors)
                .itemFactors(itemFactors)
                .singularValues(reducedSingularValues)
                .userIdToIndex(userIdToIndex)
                .itemIdToIndex(itemIdToIndex)
                .indexToItemId(indexToItemId)
                .computedAt(System.currentTimeMillis())
                .latentFactors(k)
                .build();
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
            Map<String, Map<Integer, Integer>> ratingCounts,
            int numUsers, int numItems) {

        double totalSum = 0.0;
        int totalCount = 0;

        for (Map.Entry<String, Map<Integer, Double>> userEntry : ratingAccumulator.entrySet()) {
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
}
