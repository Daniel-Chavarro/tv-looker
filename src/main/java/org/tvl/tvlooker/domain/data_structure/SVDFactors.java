package org.tvl.tvlooker.domain.data_structure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Holds the result of SVD decomposition for the user-item rating matrix.
 * R ≈ U × Σ × V^T
 *
 * After dimensionality reduction to k factors:
 * - userFactors: U_k × Σ_k (users × k)
 * - itemFactors: V_k × Σ_k (items × k)
 * - singularValues: Σ_k diagonal (k)
 */
@AllArgsConstructor
@Builder
@Getter
public class SVDFactors {

    /** User latent factor matrix (numUsers × k). Each row is a user vector. */
    private final double[][] userFactors;

    /** Item latent factor matrix (numItems × k). Each row is an item vector. */
    private final double[][] itemFactors;

    /** Singular values from SVD (k values, descending order). */
    private final double[] singularValues;

    /** Mapping from user UUID (as string) to row index in userFactors. */
    private final Map<String, Integer> userIdToIndex;

    /** Mapping from item ID to row index in itemFactors. */
    private final Map<Long, Integer> itemIdToIndex;

    /** Reverse mapping from row index to item ID. */
    private final Map<Integer, Long> indexToItemId;

    /** Timestamp when these factors were computed. */
    private final long computedAt;

    /** Number of latent factors (k). */
    private final int latentFactors;

    /**
     * User mean ratings for reconstructing predictions.
     */
    private final Map<String, Double> userMeans;

    /**
     * Checks if a user exists in the factor mapping.
     */
    public boolean hasUser(String userUuid) {
        return userIdToIndex != null && userIdToIndex.containsKey(userUuid);
    }

    public boolean hasItem(Long itemId) {
        return itemIdToIndex != null && itemIdToIndex.containsKey(itemId);
    }

    /**
     * Gets the user factor vector for a given user UUID.
     * Returns null if user not found.
     */
    public double[] getUserVector(String userUuid) {
        if (userIdToIndex == null || userFactors == null) return null;
        Integer index = userIdToIndex.get(userUuid);
        if (index == null || index >= userFactors.length) {
            return null;
        }
        return userFactors[index];
    }

    public double[] getItemVector(Long itemId) {
        if (itemIdToIndex == null || itemFactors == null) return null;
        Integer index = itemIdToIndex.get(itemId);
        if (index == null || index >= itemFactors.length) {
            return null;
        }
        return itemFactors[index];
    }

    /**
     * Computes the predicted score for a user-item pair via dot product.
     * score = userVector · itemVector
     * 
     * @return predicted score, or 0.0 if user or item not found
     */
    public double predictScore(String userUuid, Long itemId) {
        double[] userVec = getUserVector(userUuid);
        double[] itemVec = getItemVector(itemId);
        if (userVec == null || itemVec == null) {
            return 0.0;
        }
        return dotProduct(userVec, itemVec);
    }
    
    /**
     * Computes the predicted raw score adding the user's mean to the base prediction.
     */
    public double predictRealScore(String userUuid, Long itemId) {
        double deviation = predictScore(userUuid, itemId);
        double mean = (userMeans != null) ? userMeans.getOrDefault(userUuid, 3.0) : 3.0;
        return deviation + mean;
    }

    private double dotProduct(double[] a, double[] b) {
        double sum = 0.0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }
}
