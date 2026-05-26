package org.tvl.tvlooker.domain.strategy.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.data_structure.ItemFeatureVector;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.motor.utils.DataProvider;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentBasedStrategyTest {

    private ContentBasedStrategy strategy;

    @BeforeEach
    void setup() {
        strategy = new ContentBasedStrategy();
        assertEquals("content-based", strategy.getStrategyName());
    }

    @Test
    @DisplayName("Should recommend items and handle cold start")
    void testRecommend() {
        User u1 = User.builder().id(UUID.randomUUID()).build();
        User coldUser = User.builder().id(UUID.randomUUID()).build();
        Item item1 = Item.builder().id(1L).build();
        Item item2 = Item.builder().id(2L).build();

        RecommendationContext context = new RecommendationContext();

        ItemFeatureVector v1 = ItemFeatureVector.builder().build();
        v1.getGenres().put("Action", 1.0);
        ItemFeatureVector v2 = ItemFeatureVector.builder().build();
        v2.getGenres().put("Comedy", 1.0);

        ItemFeatureVector profile1 = ItemFeatureVector.builder().build();
        profile1.getGenres().put("Action", 2.0);

        context.registerDataProvider(new DataProvider<Map<Long, ItemFeatureVector>>() {
            @Override
            public String getProviderId() {
                return "item-feature-vectors";
            }
            @Override
            public Map<Long, ItemFeatureVector> provide(RecommendationContext ctx) {
                return Map.of(1L, v1, 2L, v2);
            }
        });

        context.registerDataProvider(new DataProvider<Map<String, ItemFeatureVector>>() {
            @Override
            public String getProviderId() {
                return "user-content-profiles";
            }
            @Override
            public Map<String, ItemFeatureVector> provide(RecommendationContext ctx) {
                return Map.of(u1.getId().toString(), profile1);
            }
        });

        // Test normal prediction
        List<ScoredItem> recs1 = strategy.recommend(u1, List.of(item1, item2), context);
        assertEquals(1, recs1.size()); // item2 should have score 0 and get filtered
        assertEquals(1L, recs1.get(0).getItem().getId());
        assertEquals(1.0, recs1.get(0).getScore());
        assertEquals("content-based", recs1.get(0).getSourceStrategy());

        // Test cold start
        List<ScoredItem> coldRecs = strategy.recommend(coldUser, List.of(item1, item2), context);
        assertTrue(coldRecs.isEmpty());
    }
}

