# Recommendation Engine Architecture Design

**Date:** 2026-03-05  
**Status:** Approved  
**Architecture Pattern:** Pipeline Architecture

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Decision](#architecture-decision)
3. [Core Architecture & Responsibilities](#core-architecture--responsibilities)
4. [Interface Contracts](#interface-contracts)
5. [Engine Implementation Details](#engine-implementation-details)
6. [Data Flow & Example Execution](#data-flow--example-execution)
7. [Testing Strategy](#testing-strategy)
8. [Future Extensibility](#future-extensibility)
9. [Edge Cases & Error Handling](#edge-cases--error-handling)
10. [Constraints & Assumptions](#constraints--assumptions)

---

## Overview

This document describes the architecture for an agnostic recommendation engine focused on TV programs and movies. The engine acts as an orchestrator that coordinates multiple recommendation strategies and combines their results through an aggregation strategy.

### Goals

- **Modularity:** Easy to add new recommendation algorithms
- **Resilience:** Graceful handling of strategy failures
- **Testability:** Each component testable in isolation
- **Extensibility:** Foundation for future enhancements (weighted ensembles, metadata-driven execution)

---

## Architecture Decision

After evaluating three approaches:

1. **Pipeline Architecture** ✅ (Selected)
2. Weighted Ensemble with Strategy Metadata
3. Cascade Architecture

**Selected: Pipeline Architecture** because it provides:
- Clear separation of concerns
- Natural fit with existing codebase structure
- Easy to add new strategies without modifying engine
- Resilient to partial failures
- Spring-friendly dependency injection
- Foundation for future evolution to weighted ensemble

---

## Core Architecture & Responsibilities

### Execution Pipeline

```
User + Context → [Pre-filtering] → [Strategy Execution] → [Aggregation] → [Post-processing] → Final Results
```

### Component Responsibilities

#### RecommendationEngine (HybridRecommendationEngine)

**Responsibilities:**
- Orchestrate the recommendation pipeline
- Execute strategies with error handling
- Apply pre/post-filtering
- Coordinate between strategies and aggregation
- Log execution metrics

#### RecommendationStrategy

**Responsibilities:**
- Receive: `User`, `List<Item> candidateItems`, `RecommendationContext`
- Generate: `List<ScoredItem>` with scores (0-1 range) and explanations
- Focus: Single algorithm implementation
- Independent: No knowledge of other strategies

**Key Principle:** Strategies are autonomous and don't communicate with each other.

#### AggregationStrategy

**Responsibilities:**
- Receive: `Map<String, List<ScoredItem>>` (strategy name → results)
- Generate: Final `List<ScoredItem>` (combined and ranked)
- Combine scores using chosen method (weighted average, max, Borda count, etc.)
- Merge or concatenate explanations

**Key Principle:** Only combines scores - doesn't apply business rules or filtering.

#### RecommendationContext

**Responsibilities:**
- Provide data access through `DataProvider` registry
- Store ALL system data: users, items, interactions
- Allow strategies to request computed data structures (similarity matrices, etc.)

**Key Distinction:**
- `context.getItems()` = ALL items in catalog (for computing features, similarities)
- `candidateItems` parameter = Pre-filtered valid candidates for recommendation

---

## Interface Contracts

### RecommendationStrategy Interface

```java
package org.tvl.tvlooker.domain.strategy.recommendation;

import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.List;

/**
 * Strategy interface for generating item recommendations.
 * Each implementation represents a different recommendation algorithm.
 */
public interface RecommendationStrategy {

    /**
     * Generates recommendations for a user.
     *
     * @param user The user to generate recommendations for
     * @param candidateItems Pre-filtered items that are valid candidates for recommendation
     *                       (e.g., not already watched, meet content restrictions)
     * @param context The recommendation context with ALL system data for computing features
     * @return List of scored items (can be empty if strategy has no recommendations)
     */
    List<ScoredItem> recommend(User user, List<Item> candidateItems, RecommendationContext context);

    /**
     * Returns unique identifier for this strategy (used for logging and debugging).
     *
     * @return Strategy name (e.g., "collaborative-filtering", "content-based")
     */
    String getStrategyName();
}
```

### AggregationStrategy Interface

```java
package org.tvl.tvlooker.domain.strategy.aggregation;

import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for aggregating results from multiple recommendation strategies.
 */
public interface AggregationStrategy {

    /**
     * Combines results from multiple recommendation strategies into a single ranked list.
     *
     * @param strategyResults Map of strategy name to their scored results
     * @param context The recommendation context (for accessing additional data if needed)
     * @return Final ranked list of recommendations
     */
    List<ScoredItem> aggregate(Map<String, List<ScoredItem>> strategyResults, RecommendationContext context);

    /**
     * Returns unique identifier for this aggregation strategy.
     *
     * @return Aggregation name (e.g., "weighted-average", "borda-count")
     */
    String getAggregationName();
}
```

### ScoredItem Enhancement

```java
package org.tvl.tvlooker.domain.data_structure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tvl.tvlooker.domain.model.entity.Item;

/**
 * Represents a scored recommendation result.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ScoredItem {
    /** The recommended item */
    private Item item;
    
    /** 
     * The score assigned to the item.
     * Range: 0.0 to 1.0, where higher scores indicate stronger recommendations.
     */
    private double score;
    
    /** 
     * Human-readable explanation for why this item was recommended.
     * Example: "Users similar to you loved this", "Same genre as your favorites"
     */
    private String explanation;
    
    /** 
     * Track which strategy generated this recommendation (optional, for debugging).
     * Set by strategies or during aggregation.
     */
    private String sourceStrategy;
}
```

**Note:** Changed `User` field to `Item` field (bug fix from original implementation).

---

## Engine Implementation Details

### HybridRecommendationEngine.recommend() Flow

```java
@Override
public List<ScoredItem> recommend(User user, RecommendationContext context) {
    // 1. Validation
    validateInputs(user, context);
    
    // 2. Pre-filtering: Get candidate items
    List<Item> candidateItems = filterCandidates(user, context);
    
    // 3. Execute all strategies (with error handling)
    Map<String, List<ScoredItem>> strategyResults = executeStrategies(user, candidateItems, context);
    
    // 4. Aggregate results
    List<ScoredItem> aggregated = aggregationStrategy.aggregate(strategyResults, context);
    
    // 5. Post-processing
    return postProcess(aggregated);
}
```

### Key Private Methods

#### 1. validateInputs(User user, RecommendationContext context)

**Purpose:** Ensure all required data is present

**Validations:**
- User is not null
- Context is not null
- Strategies list is not empty
- AggregationStrategy is not null
- Context has items, users, interactions (not null)

**Throws:** `InvalidEngineConfigurationException` if validation fails

#### 2. filterCandidates(User user, RecommendationContext context)

**Purpose:** Pre-filter items to create valid candidate pool

**Filtering Rules:**
- Remove items user has already interacted with (watched/rated)
- Apply content restrictions (age rating, availability, region)
- Apply business rules (e.g., premium content for premium users)

**Returns:** `List<Item>` of valid candidates

**Special Case:** If no candidates remain, throw `NoCandidatesAvailableException`. 

#### 3. executeStrategies(User user, List<Item> candidates, RecommendationContext context)

**Purpose:** Execute all strategies with graceful error handling

**Implementation:**
```java
private Map<String, List<ScoredItem>> executeStrategies(
        User user, 
        List<Item> candidates, 
        RecommendationContext context) {
    
    Map<String, List<ScoredItem>> results = new HashMap<>();
    
    for (RecommendationStrategy strategy : strategies) {
        try {
            List<ScoredItem> strategyResult = strategy.recommend(user, candidates, context);
            results.put(strategy.getStrategyName(), strategyResult);
            
            logger.info("Strategy {} returned {} recommendations for user {}", 
                strategy.getStrategyName(), strategyResult.size(), user.getId());
                
        } catch (Exception e) {
            logger.warn("Strategy {} failed for user {}: {}", 
                strategy.getStrategyName(), user.getId(), e.getMessage());
            // Continue with other strategies
        }
    }
    
    // If ALL strategies failed, throw exception
    if (results.isEmpty()) {
        throw new NoRecommendationsAvailableException(
            "All recommendation strategies failed for user " + user.getId());
    }
    
    return results;
}
```

**Error Handling:**
- Individual strategy failure: Log warning, continue with others
- All strategies fail: Throw `NoRecommendationsAvailableException`

#### 4. postProcess(List<ScoredItem> aggregated)

**Purpose:** Final cleanup and preparation of results

**Processing Steps:**
1. Deduplicate items (keep highest score if item appears multiple times)
2. Ensure scores are in valid range [0.0, 1.0]
3. Sort by score descending
4. Limit to top N results (default: 20, configurable)

**Returns:** Final `List<ScoredItem>`

---

## Data Flow & Example Execution

### Complete Data Flow Diagram

```
User Request
    ↓
HybridRecommendationEngine.recommend(user, context)
    ↓
[1. Validate inputs]
    ↓
[2. Pre-filter candidates]
    context.getItems() → filter → candidateItems
    ↓
[3. Execute Strategies]
    ├─→ CollaborativeFilteringStrategy.recommend(user, candidateItems, context)
    │       → List<ScoredItem> (scores: 0.8, 0.75, 0.6...)
    │
    ├─→ ContentBasedStrategy.recommend(user, candidateItems, context)
    │       → List<ScoredItem> (scores: 0.9, 0.7, 0.65...)
    │
    └─→ PopularityStrategy.recommend(user, candidateItems, context)
            → List<ScoredItem> (scores: 0.85, 0.8, 0.7...)
    ↓
[4. Collect Results]
    Map: {
        "collaborative": [ScoredItem(item1, 0.8), ...],
        "content-based": [ScoredItem(item1, 0.9), ...],
        "popularity": [ScoredItem(item2, 0.85), ...]
    }
    ↓
[5. Aggregate]
    AggregationStrategy.aggregate(strategyResults, context)
        → Combines scores (e.g., weighted average)
        → Final: [ScoredItem(item1, 0.87), ScoredItem(item2, 0.82), ...]
    ↓
[6. Post-process]
    - Deduplicate
    - Limit to top N
    - Sort by score
    ↓
Return final List<ScoredItem>
```

### Example Scenario Walkthrough

**Given:**
- User: John (id: 123)
- Watched items: [Item A, Item B]
- All items in catalog: [A, B, C, D, E, F]
- Strategies configured: [CollaborativeFiltering, ContentBased]
- Aggregation: WeightedAverage (collaborative: 0.6, content: 0.4)

**Step-by-step Execution:**

#### Step 1: Pre-filtering
```
Input: context.getItems() = [A, B, C, D, E, F]
Filter: Remove watched items [A, B]
Output: candidateItems = [C, D, E, F]
```

#### Step 2: Execute Strategies

**CollaborativeFiltering Strategy:**
```
Input: user=John, candidates=[C, D, E, F]
Output: [
  ScoredItem(C, 0.9, "Users similar to you loved this"),
  ScoredItem(D, 0.7, "Users like you also watched")
]
```

**ContentBased Strategy:**
```
Input: user=John, candidates=[C, D, E, F]
Output: [
  ScoredItem(C, 0.8, "Similar to Item A you watched"),
  ScoredItem(E, 0.75, "Same genre as your favorites"),
  ScoredItem(F, 0.6, "Same director as Item B")
]
```

#### Step 3: Collect Results
```java
Map<String, List<ScoredItem>> strategyResults = {
  "collaborative": [ScoredItem(C, 0.9), ScoredItem(D, 0.7)],
  "content-based": [ScoredItem(C, 0.8), ScoredItem(E, 0.75), ScoredItem(F, 0.6)]
}
```

#### Step 4: Aggregation (Weighted Average: 0.6 collaborative, 0.4 content)
```
Item C: 0.9 * 0.6 + 0.8 * 0.4 = 0.86
Item D: 0.7 * 0.6 + 0.0 * 0.4 = 0.42  (content didn't score it, treat as 0)
Item E: 0.0 * 0.6 + 0.75 * 0.4 = 0.30  (collaborative didn't score it)
Item F: 0.0 * 0.6 + 0.6 * 0.4 = 0.24

Sorted by score: [
  ScoredItem(C, 0.86, "Combined recommendation"),
  ScoredItem(D, 0.42, "Collaborative recommendation"),
  ScoredItem(E, 0.30, "Content-based recommendation"),
  ScoredItem(F, 0.24, "Content-based recommendation")
]
```

#### Step 5: Post-processing
```
Limit to top 3 results:
Final output: [
  ScoredItem(C, 0.86),
  ScoredItem(D, 0.42),
  ScoredItem(E, 0.30)
]
```

---

## Testing Strategy

### Unit Tests

#### Strategy Tests
- Test each strategy independently with mock `RecommendationContext`
- Verify scoring logic with known inputs
- Test edge cases (empty candidates, no user history)

#### Aggregation Tests
- Test aggregation algorithms with known strategy results
- Verify score combination correctness
- Test with missing items in some strategies (treat as score 0)
- Test deduplication logic

#### Engine Tests
- Mock strategies and aggregation strategy
- Verify orchestration flow (correct method calls in order)
- Test error handling (strategy failures)
- Test filtering logic

### Integration Tests

- End-to-end flow with real strategies and aggregation
- Use H2 test database (existing test profile)
- Create fixture data with known user-item interactions
- Verify expected recommendations based on algorithms
- Test complete error scenarios

### Test Coverage Goals

- Unit test coverage: > 90%
- Integration test coverage: Key user flows
- Edge case coverage: All identified edge cases

---

## Future Extensibility

### Easy Additions Without Changing Engine

#### 1. New Recommendation Strategies
Just implement `RecommendationStrategy` interface:
- Collaborative filtering (user-user, item-item)
- Content-based (genre, actors, directors, embeddings)
- Popularity-based
- Trending/time-based
- Context-aware (time of day, device)
- Deep learning models (neural collaborative filtering)

#### 2. New Aggregation Methods
Just implement `AggregationStrategy` interface:
- Weighted average
- Borda count
- Reciprocal rank fusion
- Maximum score
- Cascade (priority-based)
- Machine-learned weights

#### 3. Dynamic Configuration via Spring

```java
@Configuration
public class RecommendationConfig {
    
    @Bean
    public RecommendationEngine recommendationEngine(
            List<RecommendationStrategy> strategies,
            AggregationStrategy aggregation) {
        return new HybridRecommendationEngine(strategies, aggregation);
    }
    
    @Bean
    public RecommendationStrategy collaborativeFiltering() {
        return new CollaborativeFilteringStrategy();
    }
    
    @Bean
    public RecommendationStrategy contentBased() {
        return new ContentBasedStrategy();
    }
    
    @Bean
    public AggregationStrategy weightedAverage() {
        return new WeightedAverageAggregation(
            Map.of(
                "collaborative-filtering", 0.6,
                "content-based", 0.4
            )
        );
    }
}
```

### Evolution Path to Weighted Ensemble (Approach 2)

**Phase 1 (Current):** Pipeline Architecture
- Simple strategy execution
- Basic aggregation

**Phase 2 (Future):** Add Optional Metadata
```java
public interface StrategyMetadata {
    double getConfidence();  // 0.0 to 1.0
    double getDefaultWeight();
    Set<Class<?>> getRequiredData();
}

// Strategies can optionally implement
public class CollaborativeFilteringStrategy 
        implements RecommendationStrategy, StrategyMetadata {
    // ...
}
```

**Phase 3 (Future):** Metadata-Aware Engine
```java
// Engine checks if strategy provides metadata
if (strategy instanceof StrategyMetadata) {
    StrategyMetadata meta = (StrategyMetadata) strategy;
    // Use confidence and weights
}
```

**Benefit:** Backward compatible - old strategies continue to work without modification.

---

## Edge Cases & Error Handling

### Edge Cases

#### 1. Empty Strategy Results
**Scenario:** One strategy returns empty list  
**Handling:** Continue with other strategies, aggregate available results

**Scenario:** All strategies return empty lists  
**Handling:** Throw `NoRecommendationsAvailableException`

#### 2. No Candidate Items
**Scenario:** All items filtered out (user watched everything)  
**Handling:** Return empty list (valid state, not an error)

#### 3. Score Conflicts in Aggregation
**Scenario:** Same item scored differently by strategies  
**Handling:** Aggregation resolves by design (weighted combination)

**Scenario:** Item appears in some strategies but not others  
**Handling:** Treat missing as score 0.0 in aggregation

#### 4. New User (Cold Start)
**Scenario:** No interaction history  
**Handling:**
- Collaborative strategies may fail/return empty
- Content-based or popularity strategies continue
- Graceful degradation with fewer strategies

#### 5. New Item (Cold Start)
**Scenario:** No interaction data for item  
**Handling:**
- Collaborative filtering can't recommend it
- Content-based features should work
- Item may be excluded by pre-filtering if metadata missing

### Custom Exceptions

```java
package org.tvl.tvlooker.domain.exception;

/**
 * Thrown when all recommendation strategies fail to produce any results.
 */
public class NoRecommendationsAvailableException extends RuntimeException {
    public NoRecommendationsAvailableException(String message) {
        super(message);
    }
}

/**
 * Thrown when the engine is misconfigured (no strategies, no aggregation, etc.).
 */
public class InvalidEngineConfigurationException extends RuntimeException {
    public InvalidEngineConfigurationException(String message) {
        super(message);
    }
}

/**
 * Thrown when required data is missing from the context.
 */
public class InsufficientDataException extends RuntimeException {
    public InsufficientDataException(String message) {
        super(message);
    }
}
```

### Error Handling Summary

| Scenario | Action | Exception |
|----------|--------|-----------|
| One strategy fails | Log warning, continue | None |
| All strategies fail | Throw exception | `NoRecommendationsAvailableException` |
| No strategies configured | Throw exception | `InvalidEngineConfigurationException` |
| No aggregation configured | Throw exception | `InvalidEngineConfigurationException` |
| Invalid context | Throw exception | `InvalidEngineConfigurationException` |
| Missing required data | Throw exception | `InsufficientDataException` |
| Aggregation fails | Throw exception | Runtime exception |
| No candidates after filter | Return empty list | None |

---

## Constraints & Assumptions

### Performance Constraints

- **Strategy Execution Time:** Each strategy should complete within 5 seconds
- **Total Pipeline Time:** Target < 10 seconds for complete recommendation generation
- **Future Optimization:** Consider adding timeouts and parallel execution

### Data Constraints

- **Score Range:** All scores normalized to 0.0 to 1.0 range
- **Context Validity:** Context must have non-null users, items, and interactions lists
- **DataProvider Caching:** DataProviders should cache expensive computations (similarity matrices, embeddings)

### Business Rules

- **Mandatory Pre-filtering:** Can't recommend items user already interacted with
- **Deduplication:** Post-processing removes duplicate items (keep highest score)
- **Result Limit:** Default top N = 20 results (configurable via property or constructor)

### Spring Integration

- **Component Scanning:** Engine registered as `@Component`
- **Strategy Discovery:** Strategies auto-discovered via `@Component` scanning
- **Dependency Injection:** Aggregation strategy injected via constructor
- **Configuration:** Use `@Configuration` classes for complex setups

### Logging & Observability

#### INFO Level
- Number of candidates after filtering
- Which strategies executed successfully
- Number of results from each strategy
- Final result count

#### WARN Level
- Strategy failures (with exception message)
- Empty results from all strategies
- Performance warnings (slow strategies)

#### DEBUG Level
- Detailed strategy results before aggregation
- Score calculations during aggregation
- Individual filtering decisions
- DataProvider cache hits/misses

---

## Summary

This Pipeline Architecture provides:

✅ **Clear separation of concerns** - Engine orchestrates, strategies focus on algorithms  
✅ **Resilience** - Graceful handling of partial failures  
✅ **Testability** - Each component tests independently  
✅ **Extensibility** - Easy to add new strategies and aggregations  
✅ **Spring-friendly** - Natural dependency injection  
✅ **Future-proof** - Foundation for metadata-driven weighted ensembles  

The engine acts as a "compiler/executor" as originally envisioned, coordinating the pipeline while strategies and aggregation remain focused and independent.

---

**Next Steps:**
1. Implement updated interface contracts
2. Implement HybridRecommendationEngine with full pipeline
3. Create example strategy implementations (for testing)
4. Create example aggregation implementations
5. Write comprehensive unit and integration tests
6. Document strategy development guidelines
