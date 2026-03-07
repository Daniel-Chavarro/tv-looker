package org.tvl.tvlooker.domain.motor.utils;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tvl.tvlooker.domain.exception.NoDataProviderException;
import org.tvl.tvlooker.domain.model.entity.Interaction;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the context for generating recommendations, containing users, items, interactions, and a registry of data
 * providers. This class allows for dynamic retrieval of data through registered data providers based on the
 * requested data structure.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RecommendationContext {
    /**
     * A map that holds registered data providers, where the key is the class type of the data structure and the
     * value  is the corresponding data provider.
     */
    private Map<String, DataProvider<?>> dataProviders = new HashMap<>();
    /**
     * A list of users in the recommendation context.
     */
    private List<User> users;
    /**
     * A list of items in the recommendation context.
     */
    private List<Item> items;
    /**
     * A list of interactions in the recommendation context.
     */
    private List<Interaction> interactions;

    private Map<String, CachedData> dataCache = new HashMap<>();

    /**
     * Get computed data from a provider.
     * Strategies call this to get the data they need.
     *
     * @param providerId Unique identifier of the provider
     * @param dataType Expected return type
     * @return Computed data structure
     * @throws NoDataProviderException if provider not registered
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String providerId, Class<T> dataType) {

        CachedData cached = dataCache.get(providerId);
        if (cached != null && !cached.isExpired()) {
            return (T) cached.getData();
        }


        DataProvider<?> provider = dataProviders.get(providerId);
        if (provider == null) {
            throw new NoDataProviderException(
                    "No provider registered with ID: " + providerId);
        }

        T data = (T) provider.provide(this);

        if (provider.isCacheable()) {
            long expirationTime = System.currentTimeMillis() +
                    (provider.getCacheExpirationSeconds() * 1000);
            dataCache.put(providerId, new CachedData(data, expirationTime));
        }

        return data;
    }

    /**
     * Clear all cached data
     */
    public void clearCache() {
        dataCache.clear();
    }

    /**
     * Checks if the users or items data in the context is not null.
     *
     * @return true if either users or items is not null, false otherwise.
     */
    public boolean checkDataNotNull(){
        return users != null || items != null;
    }

    /**
     * Registers a new data provider in the context.
     *
     * @param provider The data provider to be registered.
     */
    public void registerDataProvider(DataProvider<?> provider) {
        dataProviders.put(provider.getProviderId(), provider);
    }

    /**
     * Internal class to hold cached data along with its expiration time.
     */
    @AllArgsConstructor
    @Getter
    private static class CachedData {
        private Object data;
        private long expirationTime;

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }



}
