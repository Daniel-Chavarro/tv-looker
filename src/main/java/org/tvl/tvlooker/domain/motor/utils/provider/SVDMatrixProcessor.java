package org.tvl.tvlooker.domain.motor.utils.provider;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.data_structure.SVDFactors;

import java.util.Arrays;
import java.util.Map;

/**
 * Processes SVD decomposition and builds factor matrices.
 * Extracted from MatrixFactorizationProvider to reduce God Class violations.
 */
@Component
public class SVDMatrixProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SVDMatrixProcessor.class);

    /**
     * Performs SVD on the rating matrix and builds factor matrices.
     *
     * @param ratingMatrix the user-item rating matrix
     * @param latentFactors maximum number of latent factors to extract
     * @param userIdToIndex mapping from user UUID to matrix row index
     * @param itemIdToIndex mapping from item ID to matrix column index
     * @param indexToItemId reverse mapping from matrix column index to item ID
     * @return SVDFactors containing decomposed matrices
     */
    public SVDFactors process(double[][] ratingMatrix, int latentFactors,
                               Map<String, Integer> userIdToIndex,
                               Map<Long, Integer> itemIdToIndex,
                               Map<Integer, Long> indexToItemId) {
        int numUsers = ratingMatrix.length;
        int numItems = ratingMatrix[0].length;

        RealMatrix matrix = new Array2DRowRealMatrix(ratingMatrix, false);
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);

        int k = Math.min(latentFactors, Math.min(numUsers, numItems));

        double[][] userFactors = extractUserFactors(svd, numUsers, k);
        double[][] itemFactors = extractItemFactors(svd, numItems, k);
        double[] reducedSingularValues = Arrays.copyOf(svd.getSingularValues(), k);

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
}
