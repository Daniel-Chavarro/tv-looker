package org.tvl.tvlooker.domain.strategy.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.data_structure.SVDFactors;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.domain.motor.utils.provider.MatrixFactorizationProvider;
import org.tvl.tvlooker.domain.motor.utils.provider.SVDMatrixProcessor;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MatrixFactorizationStrategy Tests")
class MatrixFactorizationStrategyTest {

    private MatrixFactorizationStrategy strategy;
    private MatrixFactorizationProvider provider;
    private SVDMatrixProcessor svdProcessor;

    @BeforeEach
    void setUp() {
        strategy = new MatrixFactorizationStrategy();
        svdProcessor = new SVDMatrixProcessor();
        provider = new MatrixFactorizationProvider(3, svdProcessor); // Small k for tests
    }

    @Test
    @DisplayName("Should return correct strategy name")
    void shouldReturnCorrectStrategyName() {
        assertEquals("matrix-factorization", strategy.getStrategyName());
    }

    @Test
    @DisplayName("Should return empty list when no SVD factors available")
    void shouldReturnEmptyListWhenNoSVDFactors() {
        User user = TestDataFactory.createUser("testuser");
        List<Item> items = TestDataFactory.createItems(3);
        RecommendationContext context = TestDataFactory.createContext(
                List.of(user), items, new ArrayList<>());

        // No provider registered - strategy returns empty list
        List<ScoredItem> result = strategy.recommend(user, items, context);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when user has no latent factors")
    void shouldReturnEmptyListWhenUserHasNoLatentFactors() {
        User user = TestDataFactory.createUser("coldUser");
        List<Item> items = TestDataFactory.createItems(3);

        // Create context with interactions from OTHER users only
        User otherUser = TestDataFactory.createUser("otherUser");
        List<Interaction> interactions = List.of(
                createRatingInteraction(1L, otherUser.getId(), items.get(0).getId()),
                createRatingInteraction(2L, otherUser.getId(), items.get(1).getId())
        );

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user, otherUser), items, interactions);
        context.registerDataProvider(provider);

        List<ScoredItem> result = strategy.recommend(user, items, context);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should generate recommendations for user with rating history")
    void shouldGenerateRecommendationsForUserWithHistory() {
        User user1 = TestDataFactory.createUser("user1");
        User user2 = TestDataFactory.createUser("user2");
        List<Item> items = TestDataFactory.createItems(5);

        // Create rating interactions for both users
        List<Interaction> interactions = new ArrayList<>();
        interactions.add(createRatingInteraction(1L, user1.getId(), items.get(0).getId()));
        interactions.add(createRatingInteraction(2L, user1.getId(), items.get(1).getId()));
        interactions.add(createRatingInteraction(3L, user1.getId(), items.get(2).getId()));
        interactions.add(createRatingInteraction(4L, user2.getId(), items.get(0).getId()));
        interactions.add(createRatingInteraction(5L, user2.getId(), items.get(1).getId()));
        interactions.add(createRatingInteraction(6L, user2.getId(), items.get(3).getId()));

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1, user2), items, interactions);
        context.registerDataProvider(provider);

        List<ScoredItem> result = strategy.recommend(user1, items, context);

        assertFalse(result.isEmpty());
        for (ScoredItem scored : result) {
            assertNotNull(scored.getItem());
            assertTrue(scored.getScore() >= 0.0 && scored.getScore() <= 1.0,
                    "Score should be in [0,1] but was " + scored.getScore());
            assertEquals("Based on your overall preferences", scored.getExplanation());
            assertEquals("matrix-factorization", scored.getSourceStrategy());
        }
    }

    @Test
    @DisplayName("Should normalize scores to [0, 1] range")
    void shouldNormalizeScoresToValidRange() {
        User user1 = TestDataFactory.createUser("user1");
        User user2 = TestDataFactory.createUser("user2");
        List<Item> items = TestDataFactory.createItems(10);

        List<Interaction> interactions = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            interactions.add(createRatingInteraction((long) (i + 1), user1.getId(), items.get(i).getId()));
        }
        for (int i = 0; i < 4; i++) {
            interactions.add(createRatingInteraction((long) (i + 10), user2.getId(), items.get(i).getId()));
        }

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1, user2), items, interactions);
        context.registerDataProvider(provider);

        List<ScoredItem> result = strategy.recommend(user1, items, context);

        for (ScoredItem scored : result) {
            assertTrue(scored.getScore() >= 0.0,
                    "Score should be >= 0 but was " + scored.getScore());
            assertTrue(scored.getScore() <= 1.0,
                    "Score should be <= 1 but was " + scored.getScore());
        }
    }

    @Test
    @DisplayName("Should return results sorted by score descending")
    void shouldReturnResultsSortedByScoreDescending() {
        User user1 = TestDataFactory.createUser("user1");
        User user2 = TestDataFactory.createUser("user2");
        List<Item> items = TestDataFactory.createItems(5);

        List<Interaction> interactions = new ArrayList<>();
        interactions.add(createRatingInteraction(1L, user1.getId(), items.get(0).getId()));
        interactions.add(createRatingInteraction(2L, user1.getId(), items.get(1).getId()));
        interactions.add(createRatingInteraction(3L, user1.getId(), items.get(2).getId()));
        interactions.add(createRatingInteraction(4L, user2.getId(), items.get(0).getId()));
        interactions.add(createRatingInteraction(5L, user2.getId(), items.get(1).getId()));

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1, user2), items, interactions);
        context.registerDataProvider(provider);

        List<ScoredItem> result = strategy.recommend(user1, items, context);

        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).getScore() >= result.get(i).getScore(),
                    "Results should be sorted by score descending");
        }
    }

    @Test
    @DisplayName("Should handle empty candidate items list")
    void shouldHandleEmptyCandidateItems() {
        User user1 = TestDataFactory.createUser("user1");
        User user2 = TestDataFactory.createUser("user2");
        List<Item> items = TestDataFactory.createItems(3);

        List<Interaction> interactions = List.of(
                createRatingInteraction(1L, user1.getId(), items.get(0).getId()),
                createRatingInteraction(2L, user2.getId(), items.get(1).getId())
        );

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1, user2), items, interactions);
        context.registerDataProvider(provider);

        List<ScoredItem> result = strategy.recommend(user1, List.of(), context);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should only recommend items present in candidate list")
    void shouldOnlyRecommendCandidateItems() {
        User user1 = TestDataFactory.createUser("user1");
        User user2 = TestDataFactory.createUser("user2");
        List<Item> allItems = TestDataFactory.createItems(5);

        List<Interaction> interactions = new ArrayList<>();
        interactions.add(createRatingInteraction(1L, user1.getId(), allItems.get(0).getId()));
        interactions.add(createRatingInteraction(2L, user1.getId(), allItems.get(1).getId()));
        interactions.add(createRatingInteraction(3L, user2.getId(), allItems.get(0).getId()));
        interactions.add(createRatingInteraction(4L, user2.getId(), allItems.get(2).getId()));

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1, user2), allItems, interactions);
        context.registerDataProvider(provider);

        // Only pass a subset as candidates
        List<Item> candidates = List.of(allItems.get(2), allItems.get(3));
        List<ScoredItem> result = strategy.recommend(user1, candidates, context);

        for (ScoredItem scored : result) {
            assertTrue(candidates.contains(scored.getItem()),
                    "Recommended item should be in candidate list");
        }
    }

    private Interaction createRatingInteraction(Long id, UUID userId, Long itemId) {
        return Interaction.builder()
                .id(id)
                .userId(userId)
                .itemId(itemId)
                .interactionType(InteractionType.RATING)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();
    }
}
