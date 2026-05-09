package org.tvl.tvlooker.domain.motor.utils.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.data_structure.ItemFeatureVector;
import org.tvl.tvlooker.domain.model.dto.Genre;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ItemFeatureVectorProviderTest {

    private ItemFeatureVectorProvider provider;

    @BeforeEach
    void setup() {
        provider = new ItemFeatureVectorProvider(true);
    }

    @Test
    @DisplayName("Should extract and calculate TF-IDF correctly")
    void testComputeTfIdf() {
        Item item1 = Item.builder().id(1L).genres(Set.of(new Genre(1L, 100L, "Action"))).build();
        Item item2 = Item.builder().id(2L).genres(Set.of(new Genre(1L, 100L, "Action"), new Genre(2L, 200L, "Comedy"))).build();
        Item item3 = Item.builder().id(3L).genres(Set.of(new Genre(3L, 300L, "Drama"))).build();

        RecommendationContext context = RecommendationContext.builder().items(List.of(item1, item2, item3)).build();
        Map<Long, ItemFeatureVector> result = provider.provide(context);

        assertNotNull(result);
        assertEquals(3, result.size());

        // Total items = 3. Action DF = 2. Comedy DF = 1. Drama DF = 1.
        // Idf action = Math.log(3 / (1+2)) = Math.log(1) = 0.0
        // Idf comedy = Math.log(3 / (1+1)) = Math.log(1.5)
        double actionIdf = Math.log(3.0 / 3.0);
        double comedyIdf = Math.log(3.0 / 2.0);

        assertEquals(actionIdf, result.get(1L).getGenres().get("Action"), 0.001);
        assertEquals(actionIdf, result.get(2L).getGenres().get("Action"), 0.001);
        assertEquals(comedyIdf, result.get(2L).getGenres().get("Comedy"), 0.001);
    }

    @Test
    @DisplayName("Should respect cache configuration")
    void testCache() {
        assertEquals("item-feature-vectors", provider.getProviderId());
        assertEquals(86400, provider.getCacheExpirationSeconds());
    }

    @Test
    @DisplayName("Should handle disabled correctly")
    void testDisabled() {
        ItemFeatureVectorProvider localProvider = new ItemFeatureVectorProvider(false);
        Item item1 = Item.builder().id(1L).genres(Set.of(new Genre(1L, 100L, "Action"))).build();
        RecommendationContext context = RecommendationContext.builder().items(List.of(item1)).build();

        assertTrue(localProvider.provide(context).isEmpty());
    }
}

