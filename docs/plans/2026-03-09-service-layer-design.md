# Service Layer Design

**Date:** 2026-03-09  
**Status:** Approved  
**Author:** OpenCode AI Assistant

## Overview

This document describes the design of the service layer that connects the REST API to the domain layer (recommendation engine). The service layer provides thin orchestration - fetching user data from repositories, building recommendation contexts, delegating to the domain engine for parallel recommendation generation, and translating results into business response objects.

## Goals

1. Create a thin service layer that orchestrates between API and domain
2. Fetch user data from repositories and build RecommendationContext
3. Delegate parallel execution of recommendation strategies to the domain engine
4. Translate domain exceptions into business result objects with status codes
5. Return comprehensive results (recommendations + status + message) to controllers

## Non-Goals

- Implementing repositories (Spring Data JPA) - separate design/implementation
- Implementing REST controllers - separate design/implementation  
- Adding caching or advanced optimizations - future enhancement
- Implementing additional recommendation workflows beyond user recommendations

## Architecture

### Layer Responsibilities

**Service Layer** (`src/main/java/org/tvl/tvlooker/service/`)
- Thin orchestration layer between REST controllers and domain
- Fetches user data from repositories (interactions, favorites, reviews)
- Builds RecommendationContext for the domain engine
- Invokes HybridRecommendationEngine (which handles parallel strategy execution internally)
- Translates domain exceptions into business result objects

**Key Principle:** The service is a pass-through orchestrator. Complex logic lives in the domain (recommendation algorithms, parallel execution via ParallelStreams) and repositories (data access). The service just coordinates the flow.

### Component Structure

```
service/
├── RecommendationService.java (interface)
├── RecommendationServiceImpl.java
├── dto/
│   ├── RecommendationResult.java
│   └── RecommendationStatus.java (enum)
└── builder/
    └── RecommendationContextBuilder.java (package-private)
```

### Data Flow

```
Controller → Service.getUserRecommendations(userId, limit)
                ↓
         Validate userId
                ↓
         Fetch from Repositories:
           - UserRepository.findById(userId)
           - InteractionRepository.findByUserId(userId)
           - ListFavoriteRepository.findByUserId(userId)
           - ReviewRepository.findByUserId(userId)
                ↓
         ContextBuilder.build(user, interactions, favorites, reviews, limit)
                ↓
         RecommendationEngine.recommend(context)
           [Parallel execution of strategies inside motor via ParallelStreams]
                ↓
         Translate result/exceptions to RecommendationResult
                ↓
         Return RecommendationResult to Controller
```

## Component Details

### RecommendationService Interface

```java
package org.tvl.tvlooker.service;

import org.tvl.tvlooker.service.dto.RecommendationResult;

/**
 * Service for generating personalized recommendations for users.
 */
public interface RecommendationService {
    
    /**
     * Get personalized recommendations for a user.
     *
     * @param userId the ID of the user
     * @param limit maximum number of recommendations to return
     * @return RecommendationResult containing recommendations and status
     */
    RecommendationResult getUserRecommendations(Long userId, int limit);
}
```

Simple contract - takes user ID and limit, returns result object. Additional methods can be added later as needed (e.g., `getRecommendationsByGenre`, `getSimilarItems`).

### RecommendationResult DTO

```java
package org.tvl.tvlooker.service.dto;

import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import lombok.Builder;
import lombok.Value;
import java.util.List;

/**
 * Result object containing recommendations and execution status.
 */
@Value
@Builder
public class RecommendationResult {
    
    /**
     * Status of the recommendation request.
     */
    RecommendationStatus status;
    
    /**
     * List of recommended items with scores.
     * Empty list if status is not SUCCESS.
     */
    @Builder.Default
    List<ScoredItem> recommendations = List.of();
    
    /**
     * Optional human-readable message providing context about the result.
     * Used for error messages or informational text.
     */
    String message;
}
```

**Fields:**
- `status`: SUCCESS, NO_DATA, INSUFFICIENT_DATA, ERROR
- `recommendations`: Empty list on failure, populated on success
- `message`: Human-readable explanation (e.g., "User has no interactions yet", "User not found")

### RecommendationStatus Enum

```java
package org.tvl.tvlooker.service.dto;

/**
 * Status codes for recommendation results.
 */
public enum RecommendationStatus {
    
    /**
     * Recommendations generated successfully.
     */
    SUCCESS,
    
    /**
     * User exists but no recommendations could be generated.
     * Example: User not found, no items available.
     */
    NO_DATA,
    
    /**
     * System unable to generate recommendations due to insufficient data.
     * This is a system error - with popularity fallback, this should rarely occur.
     */
    INSUFFICIENT_DATA,
    
    /**
     * Unexpected error occurred during recommendation generation.
     */
    ERROR
}
```

### RecommendationContextBuilder

Package-private helper class that assembles `RecommendationContext` from repository data:

```java
package org.tvl.tvlooker.service.builder;

import org.tvl.tvlooker.domain.model.entity.*;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import java.util.List;

/**
 * Helper class to build RecommendationContext from repository entities.
 * Package-private - only used by RecommendationServiceImpl.
 */
class RecommendationContextBuilder {
    
    /**
     * Build a RecommendationContext from user data.
     *
     * @param user the target user
     * @param interactions user's interaction history
     * @param favorites user's favorite items
     * @param reviews user's reviews
     * @param limit maximum number of recommendations
     * @return assembled RecommendationContext
     */
    RecommendationContext build(
        User user,
        List<Interaction> interactions,
        List<ListFavorite> favorites,
        List<Review> reviews,
        int limit
    ) {
        // Transform repository data into RecommendationContext
        // Set user ID, limit, interaction data, preferences, etc.
        // Implementation details to be defined during implementation phase
    }
}
```

**Responsibilities:**
- Transform JPA entities into domain context objects
- Set recommendation parameters (limit, filters)
- Handle empty data gracefully (e.g., new users with no interactions)
- Keep assembly logic testable and separated from service orchestration

### RecommendationServiceImpl

```java
package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.motor.RecommendationEngine;
import org.tvl.tvlooker.persistence.repository.*;
import org.tvl.tvlooker.service.builder.RecommendationContextBuilder;
import org.tvl.tvlooker.service.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {
    
    private final RecommendationEngine recommendationEngine;
    private final UserRepository userRepository;
    private final InteractionRepository interactionRepository;
    private final ListFavoriteRepository listFavoriteRepository;
    private final ReviewRepository reviewRepository;
    private final RecommendationContextBuilder contextBuilder;
    
    @Override
    @Transactional(readOnly = true)
    public RecommendationResult getUserRecommendations(Long userId, int limit) {
        // Implementation flow (pseudocode):
        // 1. Validate userId (not null, positive)
        // 2. Fetch user from UserRepository
        //    - If not found: return NO_DATA result
        // 3. Fetch user data from repositories:
        //    - InteractionRepository.findByUserId(userId)
        //    - ListFavoriteRepository.findByUserId(userId)
        //    - ReviewRepository.findByUserId(userId)
        // 4. Build context: contextBuilder.build(user, interactions, favorites, reviews, limit)
        // 5. Call engine: recommendationEngine.recommend(context)
        // 6. Catch exceptions and translate to RecommendationResult
        // 7. Return SUCCESS result with recommendations
    }
}
```

**Main Responsibilities:**
1. Validate input (userId, limit)
2. Fetch user entity from UserRepository
3. Fetch user data (interactions, favorites, reviews) from respective repositories
4. Use ContextBuilder to create RecommendationContext
5. Call `recommendationEngine.recommend(context)` - engine handles parallel execution
6. Catch domain exceptions and translate to RecommendationResult with appropriate status
7. Return result to controller

## Error Handling Strategy

The service catches domain exceptions and translates them into business result objects. No exceptions bubble up to controllers - all errors become `RecommendationResult` with appropriate status codes.

### Exception Translation Table

| Domain Exception | RecommendationStatus | Message Example |
|-----------------|---------------------|-----------------|
| `NoRecommendationsAvailableException` | NO_DATA | "No recommendations available at this time" |
| `InsufficientDataException` | ERROR | "System unable to generate recommendations" |
| `NoDataProviderException` | ERROR | "System configuration error" |
| `InvalidEngineConfigurationException` | ERROR | "Recommendation engine misconfigured" |
| User not found (Optional.empty) | NO_DATA | "User not found" |
| Any other runtime exception | ERROR | "An error occurred generating recommendations" |

### Key Insight: Popularity Fallback

With the **popularity strategy** as a fallback in the recommendation engine, the system should always produce results for valid users. New users with no interaction history will receive popularity-based recommendations.

Therefore:
- `InsufficientDataException` becomes a **system error** (STATUS: ERROR), not a normal business case
- Normal operation: new users → SUCCESS with popularity recommendations
- `NO_DATA` is reserved for cases like user not found or no items available in the catalog

## Dependencies

### Service Dependencies (Constructor Injection)

```java
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {
    
    // Domain dependency - orchestrates recommendation strategies
    private final RecommendationEngine recommendationEngine;
    
    // Persistence dependencies - fetch user data
    private final UserRepository userRepository;
    private final InteractionRepository interactionRepository;
    private final ListFavoriteRepository listFavoriteRepository;
    private final ReviewRepository reviewRepository;
    
    // Helper - assembles RecommendationContext
    private final RecommendationContextBuilder contextBuilder;
}
```

**Notes:**
- All dependencies managed by Spring (constructor injection via Lombok `@RequiredArgsConstructor`)
- Repository interfaces need to be created (Spring Data JPA) - separate implementation task
- `RecommendationEngine` is already configured as a Spring bean in `RecommendationConfig.java`

### Repository Methods Needed

Each repository will need a method to fetch data by user ID:

- `UserRepository.findById(Long userId)` - returns `Optional<User>`
- `InteractionRepository.findByUserId(Long userId)` - returns `List<Interaction>`
- `ListFavoriteRepository.findByUserId(Long userId)` - returns `List<ListFavorite>`
- `ReviewRepository.findByUserId(Long userId)` - returns `List<Review>`

These will be implemented when the repository layer is created.

### Transaction Management

Service methods annotated with `@Transactional(readOnly = true)` since they only read data. No write operations occur in the recommendation service.

## Parallel Execution Strategy

### Where Parallelization Happens

**Inside the Domain Layer (Recommendation Engine):**
- `HybridRecommendationEngine` coordinates parallel execution of multiple strategies using `CompletableFuture` or similar
- Each `RecommendationStrategy` uses **ParallelStreams** for data processing (e.g., calculating similarities, filtering items)
- `AggregationStrategy` may also use parallel processing when combining strategy results

**Service Layer Remains Synchronous:**
- Service methods are synchronous - controller calls service, waits for result
- Service makes a single call: `recommendationEngine.recommend(context)`
- Engine handles all parallelization internally
- Service receives fully computed results and returns them

### Why This Design?

1. **Thin service layer** - orchestration logic stays simple
2. **Domain encapsulation** - parallelization strategy is a domain concern, not a service concern
3. **Flexibility** - motor can evolve its parallelization approach without changing service interface
4. **Testability** - service tests mock a single engine call, not complex parallel coordination

## Testing Strategy

### Unit Tests

**RecommendationServiceImplTest:**
- Mock all dependencies (repositories, engine, contextBuilder)
- Test scenarios:
  - Happy path: valid user with interactions returns SUCCESS with recommendations
  - New user with no interactions returns SUCCESS (popularity fallback)
  - User not found returns NO_DATA with message
  - Each domain exception maps to correct RecommendationStatus
  - Context builder is called with correct data from repositories
  - Engine is called with built context and correct limit
- Use Mockito for mocking, JUnit 5 for assertions
- Target: 90%+ line coverage

**RecommendationContextBuilderTest:**
- Test context building with various data combinations:
  - User with interactions, favorites, and reviews
  - User with only interactions (no favorites/reviews)
  - User with no data (empty lists)
- Verify limit is set correctly
- Verify data transformation from entities to context
- Test edge cases (null handling, empty collections)

### Integration Tests

**RecommendationServiceIntegrationTest:**
- Use `@SpringBootTest` with test database (H2)
- Seed test data using SQL scripts or programmatic setup:
  - Multiple users with different interaction patterns
  - Items (movies/TV shows) with genres, actors, directors
  - Interactions, favorites, reviews
- Test full flow: service → repositories → engine → result
- Verify actual recommendations are returned (not mocked)
- Test scenarios:
  - User with rich interaction history gets personalized recommendations
  - New user gets popularity-based recommendations
  - Non-existent user returns NO_DATA
- Use `@Sql` annotation for test data setup/teardown

### Test Data Setup

- Reuse existing test fixtures from domain tests where applicable
- Create service-specific test builders for entities:
  - `UserTestBuilder`
  - `InteractionTestBuilder`
  - `ItemTestBuilder`
  - etc.
- Use builder pattern for readable test data construction

### Coverage Goals

- Service layer: 90%+ line coverage
- All exception paths tested
- All status codes verified
- Integration tests cover realistic end-to-end scenarios

## Implementation Plan

### Phase 1: DTOs and Interfaces
1. Create `RecommendationStatus` enum
2. Create `RecommendationResult` DTO
3. Create `RecommendationService` interface
4. Write unit tests for DTOs (if applicable)

### Phase 2: Context Builder
1. Create `RecommendationContextBuilder` class
2. Implement `build()` method (transform entities to context)
3. Write unit tests for context builder

### Phase 3: Service Implementation
1. Create `RecommendationServiceImpl` class
2. Implement `getUserRecommendations()` method:
   - Input validation
   - Repository calls
   - Context building
   - Engine invocation
   - Exception translation
3. Write comprehensive unit tests

### Phase 4: Integration Testing
1. Set up integration test infrastructure
2. Create test data fixtures
3. Write integration tests for full flow
4. Verify end-to-end functionality

### Phase 5: Documentation & Review
1. Add JavaDoc to all public interfaces and classes
2. Update README if needed
3. Code review and refinement

## Future Enhancements

These are explicitly out of scope for the initial implementation but documented for future consideration:

1. **Caching:** Add caching layer (e.g., Redis) to store computed recommendations
2. **Async API:** Expose async endpoints for long-running recommendation jobs
3. **Batch recommendations:** Pre-compute recommendations for all users in background jobs
4. **Additional workflows:** Genre-based recommendations, similar items, trending content
5. **Monitoring:** Add metrics and logging for recommendation performance
6. **A/B testing:** Support for testing different recommendation strategies
7. **Personalization parameters:** Allow users to adjust recommendation preferences (e.g., prefer recent, prefer highly-rated)

## Open Questions

None at this time. Design is approved and ready for implementation.

## References

- [Recommendation Engine Design (2026-03-05)](./2026-03-05-recommendation-engine-design.md)
- [Recommendation Strategies Design (2026-03-06)](./2026-03-06-recommendation-strategies-and-aggregations-design.md)
- Existing codebase: `src/main/java/org/tvl/tvlooker/domain/motor/`
