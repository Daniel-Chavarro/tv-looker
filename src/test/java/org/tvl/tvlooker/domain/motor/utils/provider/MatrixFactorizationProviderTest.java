package org.tvl.tvlooker.domain.motor.utils.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.tvl.tvlooker.domain.data_structure.SVDFactors;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MatrixFactorizationProvider Tests")
class MatrixFactorizationProviderTest {

    private MatrixFactorizationProvider provider;
    private SVDMatrixProcessor svdProcessor;

    @BeforeEach
    void setUp() {
        svdProcessor = new SVDMatrixProcessor();
        provider = new MatrixFactorizationProvider(3, svdProcessor);
    }

    @Test
    @DisplayName("Should return correct provider ID")
    void shouldReturnCorrectProviderId() {
        assertEquals("svd-factors", provider.getProviderId());
    }

    @Test
    @DisplayName("Should cache results for 1 week (604800 seconds)")
    void shouldCacheForOneWeek() {
        assertEquals(604800, provider.getCacheExpirationSeconds());
    }

    @Test
    @DisplayName("Should be cacheable")
    void shouldBeCacheable() {
        assertTrue(provider.isCacheable());
    }

    @Test
    @DisplayName("Should return empty factors when no interactions")
    void shouldReturnEmptyFactorsWhenNoInteractions() {
        User user = TestDataFactory.createUser("user1");
        List<Item> items = TestDataFactory.createItems(3);
        RecommendationContext context = TestDataFactory.createContext(
                List.of(user), items, new ArrayList<>());

        SVDFactors factors = provider.provide(context);

        assertNotNull(factors);
        assertEquals(0, factors.getLatentFactors());
        assertTrue(factors.getUserIdToIndex().isEmpty());
        assertTrue(factors.getItemIdToIndex().isEmpty());
    }

    @Test
    @DisplayName("Should return empty factors when no RATING interactions")
    void shouldReturnEmptyFactorsWhenNoRatingInteractions() {
        User user = TestDataFactory.createUser("user1");
        List<Item> items = TestDataFactory.createItems(3);

        // Only VIEW interactions, no RATING
        List<Interaction> interactions = List.of(
                Interaction.builder()
                        .id(1L)
                        .userId(user.getId())
                        .itemId(items.get(0).getId())
                        .interactionType(InteractionType.VIEW)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build()
        );

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user), items, interactions);

        SVDFactors factors = provider.provide(context);

        assertNotNull(factors);
        assertEquals(0, factors.getLatentFactors());
    }

    @Test
    @DisplayName("Should build user-item matrix from RATING interactions")
    void shouldBuildUserItemMatrixFromRatings() {
        User user1 = TestDataFactory.createUser("user1");
        User user2 = TestDataFactory.createUser("user2");
        List<Item> items = TestDataFactory.createItems(4);

        List<Interaction> interactions = List.of(
                createRatingInteraction(1L, user1.getId(), items.get(0).getId()),
                createRatingInteraction(2L, user1.getId(), items.get(1).getId()),
                createRatingInteraction(3L, user2.getId(), items.get(0).getId()),
                createRatingInteraction(4L, user2.getId(), items.get(2).getId())
        );

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1, user2), items, interactions);

        SVDFactors factors = provider.provide(context);

        assertNotNull(factors);
        assertTrue(factors.getLatentFactors() > 0);
        assertEquals(2, factors.getUserIdToIndex().size());
        assertEquals(3, factors.getItemIdToIndex().size());
        assertTrue(factors.hasUser(user1.getId().toString()));
        assertTrue(factors.hasUser(user2.getId().toString()));
        assertTrue(factors.hasItem(items.get(0).getId()));
        assertTrue(factors.hasItem(items.get(1).getId()));
        assertTrue(factors.hasItem(items.get(2).getId()));
    }

    @Test
    @DisplayName("Should compute SVD with correct matrix dimensions")
    void shouldComputeSVDWithCorrectDimensions() {
        User user1 = TestDataFactory.createUser("user1");
        User user2 = TestDataFactory.createUser("user2");
        List<Item> items = TestDataFactory.createItems(5);

        List<Interaction> interactions = new ArrayList<>();
        interactions.add(createRatingInteraction(1L, user1.getId(), items.get(0).getId()));
        interactions.add(createRatingInteraction(2L, user1.getId(), items.get(1).getId()));
        interactions.add(createRatingInteraction(3L, user1.getId(), items.get(2).getId()));
        interactions.add(createRatingInteraction(4L, user2.getId(), items.get(0).getId()));
        interactions.add(createRatingInteraction(5L, user2.getId(), items.get(3).getId()));
        interactions.add(createRatingInteraction(6L, user2.getId(), items.get(4).getId()));

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1, user2), items, interactions);

        SVDFactors factors = provider.provide(context);

        int k = factors.getLatentFactors();
        assertEquals(2, factors.getUserFactors().length); // 2 users
        assertEquals(5, factors.getItemFactors().length); // 5 unique items rated
        assertEquals(k, factors.getUserFactors()[0].length); // k columns
        assertEquals(k, factors.getItemFactors()[0].length); // k columns
        assertEquals(k, factors.getSingularValues().length);
    }

    @Test
    @DisplayName("Should reduce to k latent factors")
    void shouldReduceToKLatentFactors() {
        int k = 2;
        SVDMatrixProcessor processorK2 = new SVDMatrixProcessor();
        MatrixFactorizationProvider providerK2 = new MatrixFactorizationProvider(k, processorK2);

        User user1 = TestDataFactory.createUser("user1");
        User user2 = TestDataFactory.createUser("user2");
        User user3 = TestDataFactory.createUser("user3");
        List<Item> items = TestDataFactory.createItems(5);

        List<Interaction> interactions = new ArrayList<>();
        interactions.add(createRatingInteraction(1L, user1.getId(), items.get(0).getId()));
        interactions.add(createRatingInteraction(2L, user1.getId(), items.get(1).getId()));
        interactions.add(createRatingInteraction(3L, user2.getId(), items.get(0).getId()));
        interactions.add(createRatingInteraction(4L, user2.getId(), items.get(2).getId()));
        interactions.add(createRatingInteraction(5L, user3.getId(), items.get(1).getId()));
        interactions.add(createRatingInteraction(6L, user3.getId(), items.get(3).getId()));

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1, user2, user3), items, interactions);

        SVDFactors factors = providerK2.provide(context);

        assertEquals(k, factors.getLatentFactors());
        assertEquals(k, factors.getUserFactors()[0].length);
        assertEquals(k, factors.getItemFactors()[0].length);
    }

    @Test
    @DisplayName("Should compute valid predictions via dot product")
    void shouldComputeValidPredictions() {
        User user1 = TestDataFactory.createUser("user1");
        User user2 = TestDataFactory.createUser("user2");
        List<Item> items = TestDataFactory.createItems(5);

        List<Interaction> interactions = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            interactions.add(createRatingInteraction((long) (i + 1), user1.getId(), items.get(i).getId()));
        }
        for (int i = 0; i < 3; i++) {
            interactions.add(createRatingInteraction((long) (i + 10), user2.getId(), items.get(i).getId()));
        }

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1, user2), items, interactions);

        SVDFactors factors = provider.provide(context);

        // User1 should have predictions for items they rated
        double score = factors.predictScore(user1.getId().toString(), items.get(0).getId());
        assertFalse(Double.isNaN(score), "Predicted score should not be NaN");
        assertFalse(Double.isInfinite(score), "Predicted score should not be infinite");
    }

    @Test
    @DisplayName("Should return 0 score for unknown user")
    void shouldReturnZeroScoreForUnknownUser() {
        User user1 = TestDataFactory.createUser("user1");
        User unknownUser = TestDataFactory.createUser("unknown");
        List<Item> items = TestDataFactory.createItems(3);

        List<Interaction> interactions = List.of(
                createRatingInteraction(1L, user1.getId(), items.get(0).getId()),
                createRatingInteraction(2L, user1.getId(), items.get(1).getId())
        );

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1), items, interactions);

        SVDFactors factors = provider.provide(context);

        assertEquals(0.0, factors.predictScore(unknownUser.getId().toString(), items.get(0).getId()));
    }

    @Test
    @DisplayName("Should return 0 score for unknown item")
    void shouldReturnZeroScoreForUnknownItem() {
        User user1 = TestDataFactory.createUser("user1");
        List<Item> items = TestDataFactory.createItems(3);

        List<Interaction> interactions = List.of(
                createRatingInteraction(1L, user1.getId(), items.get(0).getId()),
                createRatingInteraction(2L, user1.getId(), items.get(1).getId())
        );

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1), items, interactions);

        SVDFactors factors = provider.provide(context);

        assertEquals(0.0, factors.predictScore(user1.getId().toString(), 9999L));
    }

    @Test
    @DisplayName("Should set computedAt timestamp")
    void shouldSetComputedAtTimestamp() {
        User user1 = TestDataFactory.createUser("user1");
        List<Item> items = TestDataFactory.createItems(3);

        List<Interaction> interactions = List.of(
                createRatingInteraction(1L, user1.getId(), items.get(0).getId()),
                createRatingInteraction(2L, user1.getId(), items.get(1).getId())
        );

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1), items, interactions);

        long before = System.currentTimeMillis();
        SVDFactors factors = provider.provide(context);
        long after = System.currentTimeMillis();

        assertTrue(factors.getComputedAt() >= before && factors.getComputedAt() <= after);
    }

    @Test
    @Timeout(5)
    @DisplayName("Should handle large sparse matrix efficiently")
    void shouldHandleLargeSparseMatrix() {
        int numUsers = 20;
        int numItems = 30;

        List<User> users = TestDataFactory.createUsers(numUsers);
        List<Item> items = TestDataFactory.createItems(numItems);

        // Sparse: each user rates only 3-5 items
        List<Interaction> interactions = new ArrayList<>();
        long id = 1L;
        for (User user : users) {
            for (int i = 0; i < 5; i++) {
                interactions.add(createRatingInteraction(id++, user.getId(), items.get(i).getId()));
            }
        }

        RecommendationContext context = TestDataFactory.createContext(users, items, interactions);

        SVDFactors factors = provider.provide(context);

        assertNotNull(factors);
        assertTrue(factors.getLatentFactors() > 0);
    }

    @Test
    @DisplayName("Should build reverse index mapping from index to itemId")
    void shouldBuildReverseIndexMapping() {
        User user1 = TestDataFactory.createUser("user1");
        List<Item> items = TestDataFactory.createItems(3);

        List<Interaction> interactions = List.of(
                createRatingInteraction(1L, user1.getId(), items.get(0).getId()),
                createRatingInteraction(2L, user1.getId(), items.get(1).getId())
        );

        RecommendationContext context = TestDataFactory.createContext(
                List.of(user1), items, interactions);

        SVDFactors factors = provider.provide(context);

        // Verify reverse mapping consistency
        for (Map.Entry<Long, Integer> entry : factors.getItemIdToIndex().entrySet()) {
            Long itemId = entry.getKey();
            Integer index = entry.getValue();
            assertEquals(itemId, factors.getIndexToItemId().get(index));
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
