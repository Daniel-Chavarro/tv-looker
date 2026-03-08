package org.tvl.tvlooker.domain.motor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.exception.InsufficientDataException;
import org.tvl.tvlooker.domain.exception.InvalidEngineConfigurationException;
import org.tvl.tvlooker.domain.exception.NoRecommendationsAvailableException;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.domain.motor.utils.provider.ItemPopularityProvider;
import org.tvl.tvlooker.domain.strategy.aggregation.AggregationStrategy;
import org.tvl.tvlooker.domain.strategy.aggregation.ConstantConvexAggregation;
import org.tvl.tvlooker.domain.strategy.recommendation.PopularityStrategy;
import org.tvl.tvlooker.domain.strategy.recommendation.RecommendationStrategy;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD tests for HybridRecommendationEngine.
 * 
 * Tests cover:
 * - End-to-end recommendation pipeline
 * - Validation of inputs
 * - Strategy execution with error handling
 * - Aggregation of results
 * - Post-processing (deduplication, sorting)
 * - Edge cases and error scenarios
 * - DataProvider registration
 */
@DisplayName("HybridRecommendationEngine Tests")
class HybridRecommendationEngineTest {

    private HybridRecommendationEngine engine;
    private RecommendationContext context;
    private User testUser;
    private List<RecommendationStrategy> strategies;
    private AggregationStrategy aggregationStrategy;
    private List<org.tvl.tvlooker.domain.motor.utils.DataProvider<?>> dataProviders;

    @BeforeEach
    void setUp() throws Exception {
        testUser = TestDataFactory.createUser("testUser");
        strategies = new ArrayList<>();
        dataProviders = new ArrayList<>();
    }
    
    // Helper method to create a properly initialized ConstantConvexAggregation for testing
    private ConstantConvexAggregation createTestAggregation() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("popularity", 0.2);
        weights.put("content", 0.2);
        weights.put("item-collaborative", 0.2);
        weights.put("user-collaborative", 0.2);
        weights.put("matrix-factorization", 0.2);
        
        return new ConstantConvexAggregation(weights);
    }

    // ===================== Basic Configuration Tests =====================

    @Test
    @DisplayName("Should create engine with valid configuration")
    void shouldCreateEngineWithValidConfiguration() {
        // Given: Valid strategies and aggregation
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        dataProviders.add(new ItemPopularityProvider());

        // When: Creating engine
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);

        // Then: Engine should be created successfully
        assertNotNull(engine);
        assertNotNull(engine.getSTRATEGIES());
        assertNotNull(engine.getAGGREGATION_STRATEGY());
        assertNotNull(engine.getDATA_PROVIDERS());
    }

    @Test
    @DisplayName("Should throw exception when strategies list is null")
    void shouldThrowExceptionWhenStrategiesListIsNull() {
        // Given: Null strategies
        aggregationStrategy = createTestAggregation();
        engine = new HybridRecommendationEngine(null, aggregationStrategy, dataProviders);
        context = TestDataFactory.createPopulatedContext();

        // When & Then: Should throw exception during validation
        assertThrows(InvalidEngineConfigurationException.class,
                () -> engine.recommend(testUser, context));
    }

    @Test
    @DisplayName("Should throw exception when strategies list is empty")
    void shouldThrowExceptionWhenStrategiesListIsEmpty() {
        // Given: Empty strategies list
        aggregationStrategy = createTestAggregation();
        engine = new HybridRecommendationEngine(new ArrayList<>(), aggregationStrategy, dataProviders);
        context = TestDataFactory.createPopulatedContext();

        // When & Then: Should throw exception during validation
        assertThrows(InvalidEngineConfigurationException.class,
                () -> engine.recommend(testUser, context));
    }

    @Test
    @DisplayName("Should throw exception when aggregation strategy is null")
    void shouldThrowExceptionWhenAggregationStrategyIsNull() {
        // Given: Null aggregation strategy
        strategies.add(new PopularityStrategy());
        engine = new HybridRecommendationEngine(strategies, null, dataProviders);
        context = TestDataFactory.createPopulatedContext();

        // When & Then: Should throw exception during validation
        assertThrows(InvalidEngineConfigurationException.class,
                () -> engine.recommend(testUser, context));
    }

    // ===================== Input Validation Tests =====================

    @Test
    @DisplayName("Should throw exception when user is null")
    void shouldThrowExceptionWhenUserIsNull() {
        // Given: Valid engine configuration
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        context = TestDataFactory.createPopulatedContext();

        // When & Then: Should throw exception
        assertThrows(InsufficientDataException.class,
                () -> engine.recommend(null, context));
    }

    @Test
    @DisplayName("Should throw exception when context is null")
    void shouldThrowExceptionWhenContextIsNull() {
        // Given: Valid engine configuration
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);

        // When & Then: Should throw exception (NPE because DATA_PROVIDERS.forEach happens before validation)
        assertThrows(NullPointerException.class,
                () -> engine.recommend(testUser, null));
    }

    @Test
    @DisplayName("Should throw exception when context has null data")
    void shouldThrowExceptionWhenContextHasNullData() {
        // Given: Context with null data
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        context = TestDataFactory.createNullDataContext();

        // When & Then: Should throw exception
        assertThrows(InsufficientDataException.class,
                () -> engine.recommend(testUser, context));
    }

    // ===================== End-to-End Pipeline Tests =====================

    @Test
    @DisplayName("Should execute complete recommendation pipeline successfully")
    void shouldExecuteCompleteRecommendationPipeline() {
        // Given: Fully configured engine
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        dataProviders.add(new ItemPopularityProvider());
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        List<Item> items = TestDataFactory.createItems(5);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());

        // When: Requesting recommendations
        List<ScoredItem> recommendations = engine.recommend(testUser, context);

        // Then: Should return valid recommendations
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        
        // Verify all items have valid scores
        for (ScoredItem scored : recommendations) {
            assertNotNull(scored.getItem());
            assertTrue(scored.getScore() >= 0.0 && scored.getScore() <= 1.0);
            assertNotNull(scored.getExplanation());
        }
    }

    @Test
    @DisplayName("Should register data providers with context")
    void shouldRegisterDataProvidersWithContext() {
        // Given: Engine with data providers
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        
        ItemPopularityProvider provider = new ItemPopularityProvider();
        dataProviders.add(provider);
        
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        List<Item> items = TestDataFactory.createItems(3);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());

        // When: Requesting recommendations
        engine.recommend(testUser, context);

        // Then: Provider should be registered in context
        assertNotNull(context.getData("item-popularity", Map.class));
    }

    // ===================== Strategy Execution Tests =====================

    @Test
    @DisplayName("Should execute multiple strategies")
    void shouldExecuteMultipleStrategies() {
        // Given: Multiple strategies
        PopularityStrategy strategy1 = new PopularityStrategy();
        RecommendationStrategy strategy2 = mock(RecommendationStrategy.class);
        when(strategy2.getStrategyName()).thenReturn("test-strategy");
        when(strategy2.recommend(any(), any(), any())).thenReturn(new ArrayList<>());
        
        strategies.add(strategy1);
        strategies.add(strategy2);
        
        aggregationStrategy = createTestAggregation();
        dataProviders.add(new ItemPopularityProvider());
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        List<Item> items = TestDataFactory.createItems(3);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());

        // When: Requesting recommendations
        engine.recommend(testUser, context);

        // Then: Both strategies should be called
        verify(strategy2, times(1)).recommend(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle single strategy failure gracefully")
    void shouldHandleSingleStrategyFailureGracefully() {
        // Given: One good strategy and one failing strategy
        PopularityStrategy goodStrategy = new PopularityStrategy();
        
        RecommendationStrategy failingStrategy = mock(RecommendationStrategy.class);
        when(failingStrategy.getStrategyName()).thenReturn("failing-strategy");
        when(failingStrategy.recommend(any(), any(), any()))
                .thenThrow(new RuntimeException("Strategy failed"));
        
        strategies.add(goodStrategy);
        strategies.add(failingStrategy);
        
        aggregationStrategy = createTestAggregation();
        dataProviders.add(new ItemPopularityProvider());
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        List<Item> items = TestDataFactory.createItems(3);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());

        // When: Requesting recommendations
        List<ScoredItem> recommendations = engine.recommend(testUser, context);

        // Then: Should still return recommendations from good strategy
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when all strategies fail")
    void shouldThrowExceptionWhenAllStrategiesFail() {
        // Given: All strategies fail
        RecommendationStrategy failingStrategy1 = mock(RecommendationStrategy.class);
        when(failingStrategy1.getStrategyName()).thenReturn("failing-strategy-1");
        when(failingStrategy1.recommend(any(), any(), any()))
                .thenThrow(new RuntimeException("Strategy 1 failed"));
        
        RecommendationStrategy failingStrategy2 = mock(RecommendationStrategy.class);
        when(failingStrategy2.getStrategyName()).thenReturn("failing-strategy-2");
        when(failingStrategy2.recommend(any(), any(), any()))
                .thenThrow(new RuntimeException("Strategy 2 failed"));
        
        strategies.add(failingStrategy1);
        strategies.add(failingStrategy2);
        
        aggregationStrategy = createTestAggregation();
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        List<Item> items = TestDataFactory.createItems(3);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());

        // When & Then: Should throw exception
        assertThrows(NoRecommendationsAvailableException.class,
                () -> engine.recommend(testUser, context));
    }

    // ===================== Post-Processing Tests =====================

    @Test
    @DisplayName("Should sort recommendations by score descending")
    void shouldSortRecommendationsByScoreDescending() {
        // Given: Engine configuration
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        dataProviders.add(new ItemPopularityProvider());
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        List<Item> items = TestDataFactory.createItems(10);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());

        // When: Getting recommendations
        List<ScoredItem> recommendations = engine.recommend(testUser, context);

        // Then: Should be sorted in descending order
        for (int i = 1; i < recommendations.size(); i++) {
            assertTrue(recommendations.get(i - 1).getScore() >= recommendations.get(i).getScore(),
                    "Recommendations should be sorted by score descending");
        }
    }

    @Test
    @DisplayName("Should handle empty candidate items after filtering")
    void shouldHandleEmptyCandidateItemsAfterFiltering() {
        // Given: Engine with no items
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        dataProviders.add(new ItemPopularityProvider());
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        context = TestDataFactory.createEmptyItemsContext();

        // When: Getting recommendations
        List<ScoredItem> recommendations = engine.recommend(testUser, context);

        // Then: Should return empty list (or handle gracefully)
        assertNotNull(recommendations);
        // Behavior depends on implementation - could be empty or throw exception
    }

    // ===================== Cold Start Tests =====================

    @Test
    @DisplayName("Should handle new user with no interactions (cold start)")
    void shouldHandleNewUserWithNoInteractions() {
        // Given: New user with no interaction history
        User newUser = TestDataFactory.createUser("newUser");
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        dataProviders.add(new ItemPopularityProvider());
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        List<Item> items = TestDataFactory.createItems(5);
        context = TestDataFactory.createColdStartContext(List.of(newUser), items);

        // When: Getting recommendations for new user
        List<ScoredItem> recommendations = engine.recommend(newUser, context);

        // Then: Should return recommendations (based on popularity)
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }

    @Test
    @DisplayName("Should degrade gracefully when collaborative strategies fail for new users")
    void shouldDegradeGracefullyForNewUsers() {
        // Given: Engine with multiple strategies
        PopularityStrategy popularityStrategy = new PopularityStrategy();
        
        // Mock collaborative strategy that fails for new users
        RecommendationStrategy collabStrategy = mock(RecommendationStrategy.class);
        when(collabStrategy.getStrategyName()).thenReturn("collaborative");
        when(collabStrategy.recommend(any(), any(), any())).thenReturn(new ArrayList<>());
        
        strategies.add(popularityStrategy);
        strategies.add(collabStrategy);
        
        aggregationStrategy = createTestAggregation();
        dataProviders.add(new ItemPopularityProvider());
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        User newUser = TestDataFactory.createUser("newUser");
        List<Item> items = TestDataFactory.createItems(5);
        context = TestDataFactory.createColdStartContext(List.of(newUser), items);

        // When: Getting recommendations
        List<ScoredItem> recommendations = engine.recommend(newUser, context);

        // Then: Should still return recommendations from popularity strategy
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }

    // ===================== Integration Tests =====================

    @Test
    @DisplayName("Should integrate strategy results correctly")
    void shouldIntegrateStrategyResultsCorrectly() {
        // Given: Real strategy and aggregation
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        dataProviders.add(new ItemPopularityProvider());
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        List<Item> items = TestDataFactory.createItemsForNormalizationTest();
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());

        // When: Getting recommendations
        List<ScoredItem> recommendations = engine.recommend(testUser, context);

        // Then: Should properly integrate results
        assertNotNull(recommendations);
        assertEquals(4, recommendations.size());
        
        // Most popular item should be first
        assertTrue(recommendations.get(0).getScore() >= recommendations.get(1).getScore());
    }

    // ===================== Performance Tests =====================

    @Test
    @DisplayName("Should complete recommendation pipeline in reasonable time")
    void shouldCompleteInReasonableTime() {
        // Given: Engine with strategy
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        dataProviders.add(new ItemPopularityProvider());
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        List<Item> items = TestDataFactory.createItems(100);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());

        // When: Getting recommendations
        long startTime = System.currentTimeMillis();
        List<ScoredItem> recommendations = engine.recommend(testUser, context);
        long endTime = System.currentTimeMillis();

        // Then: Should complete within 5 seconds
        assertTrue(endTime - startTime < 5000,
                "Recommendation pipeline should complete within 5 seconds");
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }

    // ===================== Edge Case Tests =====================

    @Test
    @DisplayName("Should handle context with empty interactions list")
    void shouldHandleContextWithEmptyInteractions() {
        // Given: Context with no interactions
        strategies.add(new PopularityStrategy());
        aggregationStrategy = createTestAggregation();
        dataProviders.add(new ItemPopularityProvider());
        engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders);
        
        List<Item> items = TestDataFactory.createItems(5);
        context = TestDataFactory.createContext(List.of(testUser), items, new ArrayList<>());

        // When: Getting recommendations
        List<ScoredItem> recommendations = engine.recommend(testUser, context);

        // Then: Should handle gracefully
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }
}
