package org.tvl.tvlooker.domain.motor.utils.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.data_structure.ItemFeatureVector;
import org.tvl.tvlooker.domain.model.dto.Director;
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

        // Total items N = 3. Action DF = 2. Comedy DF = 1. Drama DF = 1.
        // Idf action = Math.log((N+1) / (df+1)) = Math.log(4 / 3)
        // Idf comedy = Math.log((N+1) / (df+1)) = Math.log(4 / 2)
        double actionIdf = Math.log(4.0 / 3.0);
        double comedyIdf = Math.log(4.0 / 2.0);

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
    @DisplayName("Should use flat weights when disabled")
    void testDisabled() {
        ItemFeatureVectorProvider localProvider = new ItemFeatureVectorProvider(false);
        Item item1 = Item.builder().id(1L).genres(Set.of(new Genre(1L, 100L, "Action"))).build();
        RecommendationContext context = RecommendationContext.builder().items(List.of(item1)).build();

        Map<Long, ItemFeatureVector> result = localProvider.provide(context);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(1.0, result.get(1L).getGenres().get("Action"), 0.001);
    }

    @Test
    @DisplayName("Should skip items with null genre names")
    void testShouldSkipNullGenreNames() {
        Genre nullNameGenre = new Genre(1L, 100L, null);
        Genre blankNameGenre = new Genre(2L, 200L, "");
        Genre validGenre = new Genre(3L, 300L, "Action");

        Item item1 = Item.builder().id(1L).genres(Set.of(nullNameGenre, blankNameGenre, validGenre)).build();
        RecommendationContext context = RecommendationContext.builder().items(List.of(item1)).build();

        Map<Long, ItemFeatureVector> result = provider.provide(context);

        assertNotNull(result);
        assertEquals(1, result.size());
        // Only valid genre should be in the vector
        assertTrue(result.get(1L).getGenres().containsKey("Action"));
        assertFalse(result.get(1L).getGenres().containsKey(null));
        assertFalse(result.get(1L).getGenres().containsKey(""));
    }

    @Test
    @DisplayName("Should skip items with null director names")
    void testShouldSkipNullDirectorNames() {
        Director nullDirector = new Director(1L, 100L, null);
        Director blankDirector = new Director(2L, 200L, "");
        Director validDirector = new Director(3L, 300L, "Nolan");

        Item item1 = Item.builder().id(1L).directors(Set.of(nullDirector, blankDirector, validDirector)).build();
        RecommendationContext context = RecommendationContext.builder().items(List.of(item1)).build();

        Map<Long, ItemFeatureVector> result = provider.provide(context);

        assertNotNull(result);
        assertEquals(1, result.size());
        // Only valid director should be in the vector
        assertTrue(result.get(1L).getDirectors().containsKey("Nolan"));
        assertFalse(result.get(1L).getDirectors().containsKey(null));
        assertFalse(result.get(1L).getDirectors().containsKey(""));
    }

    @Test
    @DisplayName("Should skip items with null id")
    void testShouldSkipNullItemId() {
        Genre validGenre = new Genre(1L, 100L, "Action");
        Item item1 = Item.builder().id(null).genres(Set.of(validGenre)).build();
        Item item2 = Item.builder().id(2L).genres(Set.of(validGenre)).build();

        RecommendationContext context = RecommendationContext.builder().items(List.of(item1, item2)).build();

        Map<Long, ItemFeatureVector> result = provider.provide(context);

        assertNotNull(result);
        assertEquals(1, result.size());
        // Only item with valid id should be in the result
        assertTrue(result.containsKey(2L));
        assertFalse(result.containsKey(null));
    }
}

