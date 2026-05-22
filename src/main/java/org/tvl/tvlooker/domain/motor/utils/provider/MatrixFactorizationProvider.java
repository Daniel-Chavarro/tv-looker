package org.tvl.tvlooker.domain.motor.utils.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.data_structure.SVDFactors;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.motor.utils.DataProvider;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

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
    private final SVDMatrixProcessor svdProcessor;

    public MatrixFactorizationProvider(
            @Value("${recommendation.mf.latent-factors:50}") int latentFactors,
            SVDMatrixProcessor svdProcessor) {
        if (latentFactors < 1) {
            logger.warn("Invalid recommendation.mf.latent-factors={}, clamping to 1", latentFactors);
            this.latentFactors = 1;
        } else {
            this.latentFactors = latentFactors;
        }
        this.svdProcessor = svdProcessor;
    }

    @Override
    public String getProviderId() {
        return "svd-factors";
    }

    @Override
    public SVDFactors provide(RecommendationContext context) {
        List<Interaction> interactions = context.getInteractions();
        if (interactions == null || interactions.isEmpty()) {
            logger.info("No interactions available for SVD computation");
            return buildEmptyFactors();
        }

        return computeSVDFactors(interactions, context);
    }

    @Override
    public long getCacheExpirationSeconds() {
        return 604800; // 1 week
    }

    private SVDFactors computeSVDFactors(List<Interaction> interactions, RecommendationContext context) {
        RatingMatrixBuilder matrixBuilder = new RatingMatrixBuilder(context);
        RatingMatrixBuilder.RatingMatrixResult result = matrixBuilder.build(interactions);

        if (result.matrix().length == 0) {
            logger.info("No rating interactions found for SVD computation");
            return buildEmptyFactors();
        }

        logger.info("Building rating matrix: {} users × {} items, k={}",
                result.numUsers(), result.numItems(), latentFactors);

        return svdProcessor.process(result.matrix(), latentFactors,
                result.userIdToIndex(), result.itemIdToIndex(), result.indexToItemId(), result.userMeans());
    }

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
                .userMeans(Map.of())
                .build();
    }
}
