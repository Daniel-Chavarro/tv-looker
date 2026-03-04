package org.tvl.tvlooker.domain.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tvl.tvlooker.domain.data_provider.DataProvider;
import org.tvl.tvlooker.domain.exception.NoDataProviderException;

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
    private final Map<Class<?>, DataProvider<?>> DATA_PROVIDERS = new HashMap<>();
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

    /**
     * Retrieves data for a specific data structure using the registered data providers.
     *
     * @param structure The class type of the data structure to retrieve.
     * @param <T>       The type of the data structure.
     * @return The data corresponding to the specified structure.
     * @throws NoDataProviderException If no data provider is found for the specified structure.
     */
    public <T> T getData(Class<T> structure){
        DataProvider<?> dataProvider = DATA_PROVIDERS.get(structure);

        if(dataProvider == null){
            throw new NoDataProviderException("No data provider found for " + structure.getSimpleName());
        }

        Object data = dataProvider.provide(this);

        return structure.cast(data);
    }

    /**
     * Registers a data provider for a specific data structure.
     *
     * @param dataProvider The data provider to register.
     * @param structure    The class type of the data structure that the provider will supply.
     */
    public void registerDataProvider(DataProvider<?> dataProvider, Class<?> structure){
        DATA_PROVIDERS.put(structure, dataProvider);
    }
}
