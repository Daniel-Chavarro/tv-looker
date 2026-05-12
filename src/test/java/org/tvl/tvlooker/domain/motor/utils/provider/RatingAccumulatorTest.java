package org.tvl.tvlooker.domain.motor.utils.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RatingAccumulator Tests")
class RatingAccumulatorTest {

    private RatingAccumulator accumulator;

    @BeforeEach
    void setUp() {
        accumulator = new RatingAccumulator();
    }

    @Test
    @DisplayName("Should skip interactions with null userId or itemId")
    void testShouldSkipNullUserIdOrItemId() {
        UUID userId = UUID.randomUUID();
        
        RatingAccumulator.IndexMappings mappings = new RatingAccumulator.IndexMappings(
            Map.of(userId.toString(), 0),
            Map.of(1L, 0),
            Map.of(0, 1L)
        );
        
        Interaction i1 = Interaction.builder()
            .id(1L)
            .userId(null)
            .itemId(1L)
            .interactionType(InteractionType.RATING)
            .createdAt(new Timestamp(System.currentTimeMillis()))
            .build();
        
        Interaction i2 = Interaction.builder()
            .id(2L)
            .userId(userId)
            .itemId(null)
            .interactionType(InteractionType.RATING)
            .createdAt(new Timestamp(System.currentTimeMillis()))
            .build();
        
        Interaction i3 = Interaction.builder()
            .id(3L)
            .userId(userId)
            .itemId(1L)
            .interactionType(InteractionType.RATING)
            .createdAt(new Timestamp(System.currentTimeMillis()))
            .build();
        
        RecommendationContext context = new RecommendationContext();
        
        accumulator.accumulate(List.of(i1, i2, i3), mappings, context);
        
        // Only i3 should be accumulated
        Map<Integer, Double> ratings = accumulator.getUserRatings(userId.toString());
        Map<Integer, Integer> counts = accumulator.getUserCounts(userId.toString());
        
        assertFalse(ratings.isEmpty());
        assertFalse(counts.isEmpty());
        assertEquals(1, ratings.size());
        assertEquals(1, counts.size());
    }

    @Test
    @DisplayName("Should handle null reviewId safely")
    void testShouldHandleNullReviewId() {
        UUID userId = UUID.randomUUID();
        
        RatingAccumulator.IndexMappings mappings = new RatingAccumulator.IndexMappings(
            Map.of(userId.toString(), 0),
            Map.of(1L, 0),
            Map.of(0, 1L)
        );
        
        Interaction i1 = Interaction.builder()
            .id(1L)
            .userId(userId)
            .itemId(1L)
            .reviewId(null)
            .interactionType(InteractionType.RATING)
            .createdAt(new Timestamp(System.currentTimeMillis()))
            .build();
        
        RecommendationContext context = new RecommendationContext();
        
        // Should not throw NPE
        accumulator.accumulate(List.of(i1), mappings, context);
        
        Map<Integer, Double> ratings = accumulator.getUserRatings(userId.toString());
        assertFalse(ratings.isEmpty());
    }

    @Test
    @DisplayName("Should use Objects.equals for null-safe review comparison")
    void testShouldUseObjectsEqualsForReviewComparison() {
        UUID userId = UUID.randomUUID();
        
        RatingAccumulator.IndexMappings mappings = new RatingAccumulator.IndexMappings(
            Map.of(userId.toString(), 0),
            Map.of(1L, 0),
            Map.of(0, 1L)
        );
        
        org.tvl.tvlooker.domain.model.dto.Review review1 = new org.tvl.tvlooker.domain.model.dto.Review(
            1L, userId, 1L, 5, "Great", new Timestamp(System.currentTimeMillis())
        );
        
        Interaction i1 = Interaction.builder()
            .id(1L)
            .userId(userId)
            .itemId(1L)
            .reviewId(1L)
            .interactionType(InteractionType.RATING)
            .createdAt(new Timestamp(System.currentTimeMillis()))
            .build();
        
        RecommendationContext context = new RecommendationContext();
        context.setReviews(List.of(review1));
        
        // Should not throw NPE
        accumulator.accumulate(List.of(i1), mappings, context);
        
        Map<Integer, Double> ratings = accumulator.getUserRatings(userId.toString());
        assertFalse(ratings.isEmpty());
    }
}
