package org.tvl.tvlooker.domain.data_provider;

import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

public interface DataProvider <T>{
    T provide(RecommendationContext context);
}
