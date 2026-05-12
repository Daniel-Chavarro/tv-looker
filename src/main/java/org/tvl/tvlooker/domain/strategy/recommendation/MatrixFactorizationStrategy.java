package org.tvl.tvlooker.domain.strategy.recommendation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.data_structure.SVDFactors;
import org.tvl.tvlooker.domain.exception.NoDataProviderException;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Matrix Factorization recommendation strategy using SVD latent factors.
 *
 * Predicts user preferences by computing the dot product of user and item
 * latent factor vectors obtained from SVD decomposition of the rating matrix.
 *
 * Prediction: score = userVector · itemVector
 * Normalization: score / 5.0 (ratings are 1-5 scale)
 */
public class MatrixFactorizationStrategy implements RecommendationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MatrixFactorizationStrategy.class);
    private static final double MAX_RATING = 5.0;
    private static final String EXPLANATION = "Based on your overall preferences";
    private static final String PROVIDER_ID = "svd-factors";

    @Override
    public List<ScoredItem> recommend(User user, List<Item> candidateItems, RecommendationContext context) {
        if (candidateItems == null || candidateItems.isEmpty()) {
            return List.of();
        }

        SVDFactors factors;
        try {
            factors = context.getData(PROVIDER_ID, SVDFactors.class);
        } catch (NoDataProviderException e) {
            logger.debug("No SVD factors provider available, returning empty recommendations");
            return List.of();
        }

        if (factors == null || factors.getLatentFactors() == 0) {
            return List.of();
        }

        String userUuid = user.getId().toString();

        if (!factors.hasUser(userUuid)) {
            return List.of();
        }

        return candidateItems.stream()
                .filter(item -> item.getId() != null && factors.hasItem(item.getId()))
                .map(item -> {
                    double rawScore = factors.predictScore(userUuid, item.getId());
                    double normalizedScore = normalizeScore(rawScore);
                    return ScoredItem.builder()
                            .item(item)
                            .score(normalizedScore)
                            .explanation(EXPLANATION)
                            .sourceStrategy(getStrategyName())
                            .build();
                })
                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "matrix-factorization";
    }

    /**
     * Normalizes a raw predicted score to [0, 1] range.
     * Raw scores are in the rating scale (1-5), so divide by MAX_RATING.
     * Clamps to [0, 1] to handle edge cases from SVD approximation.
     */
    private double normalizeScore(double rawScore) {
        double normalized = rawScore / MAX_RATING;
        return Math.max(0.0, Math.min(1.0, normalized));
    }
}
