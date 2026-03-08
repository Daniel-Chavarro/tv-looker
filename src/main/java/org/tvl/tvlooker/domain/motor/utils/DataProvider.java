package org.tvl.tvlooker.domain.motor.utils;

public interface DataProvider <T>{
    /**
     * Returns unique identifier for this provider
     * Used by strategies to request specific data
     */
    String getProviderId();

    /**
     * Provides data based on the given recommendation context.
     *
     * @param context The context containing user information, interaction history, and other relevant data.
     * @return The data of type T that can be used for generating recommendations.
     */
    T provide(RecommendationContext context);

    /**
     * Whether this provider's result should be cached
     * Default: true
     */
    default boolean isCacheable() {
        return true;
    }

    /**
     * Cache expiration time in seconds (if cacheable)
     * Default: 1 hour
     */
    default long getCacheExpirationSeconds() {
        return 3600;
    }
}
