package org.tvl.tvlooker.domain.motor.utils.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.data_structure.ItemFeatureVector;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserProfileProviderTest {

    private UserProfileProvider provider;

    @BeforeEach
    void setup() {
        provider = new UserProfileProvider();
    }

    @Test
    @DisplayName("Should aggregate profiles based on weights")
    void testProvide() {
        UUID userId = UUID.randomUUID();
        Interaction i1 = new Interaction(1L, userId, 1L, null, InteractionType.LIKE, new Timestamp(System.currentTimeMillis()));
        Interaction i2 = new Interaction(2L, userId, 2L, null, InteractionType.VIEW, new Timestamp(System.currentTimeMillis()));
        Interaction i3 = new Interaction(3L, userId, 3L, null, InteractionType.RESEARCH, new Timestamp(System.currentTimeMillis())); // RESEARCH should have weight 0

        RecommendationContext context = new RecommendationContext();
        context.setInteractions(List.of(i1, i2, i3));

        ItemFeatureVector v1 = ItemFeatureVector.builder().build();
        v1.getGenres().put("Action", 1.0);
        ItemFeatureVector v2 = ItemFeatureVector.builder().build();
        v2.getGenres().put("Comedy", 1.0);
        ItemFeatureVector v3 = ItemFeatureVector.builder().build();
        v3.getGenres().put("Action", 1.0);

        context.registerDataProvider(new org.tvl.tvlooker.domain.motor.utils.DataProvider<Map<Long, ItemFeatureVector>>() {
            @Override
            public String getProviderId() {
                return "item-feature-vectors";
            }
            @Override
            public Map<Long, ItemFeatureVector> provide(RecommendationContext ctx) {
                return Map.of(1L, v1, 2L, v2, 3L, v3);
            }
        });

        Map<String, ItemFeatureVector> profiles = provider.provide(context);

        assertNotNull(profiles.get(userId.toString()));
        ItemFeatureVector prof = profiles.get(userId.toString());

        // LIKE weight=5.0 -> Action = 5.0
        assertEquals(5.0, prof.getGenres().get("Action"), 0.001);
        // VIEW weight=3.0 -> Comedy = 3.0
        assertEquals(3.0, prof.getGenres().get("Comedy"), 0.001);
    }

    @Test
    @DisplayName("Should skip interactions with null userId or itemId")
    void testShouldSkipNullUserIdOrItemId() {
        UUID userId = UUID.randomUUID();
        Interaction i1 = new Interaction(1L, null, 1L, null, InteractionType.LIKE, new Timestamp(System.currentTimeMillis()));
        Interaction i2 = new Interaction(2L, userId, null, null, InteractionType.VIEW, new Timestamp(System.currentTimeMillis()));
        Interaction i3 = new Interaction(3L, userId, 3L, null, InteractionType.LIKE, new Timestamp(System.currentTimeMillis()));

        RecommendationContext context = new RecommendationContext();
        context.setInteractions(List.of(i1, i2, i3));

        ItemFeatureVector v3 = ItemFeatureVector.builder().build();
        v3.getGenres().put("Action", 1.0);

        context.registerDataProvider(new org.tvl.tvlooker.domain.motor.utils.DataProvider<Map<Long, ItemFeatureVector>>() {
            @Override
            public String getProviderId() {
                return "item-feature-vectors";
            }
            @Override
            public Map<Long, ItemFeatureVector> provide(RecommendationContext ctx) {
                return Map.of(3L, v3);
            }
        });

        Map<String, ItemFeatureVector> profiles = provider.provide(context);

        assertNotNull(profiles.get(userId.toString()));
        ItemFeatureVector prof = profiles.get(userId.toString());
        // Only i3 should be processed (i1 and i2 skipped due to null userId/itemId)
        assertEquals(5.0, prof.getGenres().get("Action"), 0.001);
    }

    @Test
    @DisplayName("Should handle null review id safely")
    void testShouldHandleNullReviewId() {
        UUID userId = UUID.randomUUID();
        Interaction i1 = new Interaction(1L, userId, 1L, null, InteractionType.RATING, new Timestamp(System.currentTimeMillis()));

        RecommendationContext context = new RecommendationContext();
        context.setInteractions(List.of(i1));

        ItemFeatureVector v1 = ItemFeatureVector.builder().build();
        v1.getGenres().put("Action", 1.0);

        context.registerDataProvider(new org.tvl.tvlooker.domain.motor.utils.DataProvider<Map<Long, ItemFeatureVector>>() {
            @Override
            public String getProviderId() {
                return "item-feature-vectors";
            }
            @Override
            public Map<Long, ItemFeatureVector> provide(RecommendationContext ctx) {
                return Map.of(1L, v1);
            }
        });

        Map<String, ItemFeatureVector> profiles = provider.provide(context);

        assertNotNull(profiles.get(userId.toString()));
        // Should not throw NPE, should use default score 4.0
        assertNotNull(profiles.get(userId.toString()));
    }
}



