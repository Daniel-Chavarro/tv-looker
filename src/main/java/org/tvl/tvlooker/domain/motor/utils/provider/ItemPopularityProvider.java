package org.tvl.tvlooker.domain.motor.utils.provider;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.motor.utils.DataProvider;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.HashMap;
import java.util.Map;

@Component
public class ItemPopularityProvider implements DataProvider<Map<Long, Double>> {

    @Override
    public String getProviderId() {
        return "item-popularity";
    }

    @Override
    public Map<Long, Double> provide(RecommendationContext context) {
        Map<Long, Double> popularityScores = new HashMap<>();

        // Find max popularity for normalization
        double maxPopularity = context.getItems().stream()
                .mapToDouble(item -> item.getPopularity().doubleValue())
                .max()
                .orElse(1.0);

        // Normalize all scores to [0, 1]
        for (Item item : context.getItems()) {
            double normalized = item.getPopularity().doubleValue() / maxPopularity;
            popularityScores.put(item.getId(), normalized);
        }

        return popularityScores;
    }

    @Override
    public long getCacheExpirationSeconds() {
        return 86400; // 24 hours - popularity changes slowly
    }
}