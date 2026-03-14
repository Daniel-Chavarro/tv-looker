# Service Layer Design (Revised)

**Date:** 2026-03-09  
**Last Updated:** 2026-03-14  
**Status:** Approved  
**Author:** OpenCode AI Assistant

## Overview

This document describes the design of the service layer that connects the REST API to the domain layer (recommendation engine). The service layer follows a **service composition pattern** where each entity has its own service, and RecommendationService orchestrates these services to provide recommendations.

## Goals

1. Create entity services - one service per domain entity with CRUD + domain-specific queries
2. Build RecommendationService that composes entity services to orchestrate recommendation workflow
3. Load all system data (users, items, interactions) needed for collaborative filtering algorithms
4. Delegate to the recommendation engine for strategy execution
5. Use exception-based error handling - controllers translate exceptions to HTTP responses
6. Return items directly (scores hidden from users for now)

## Non-Goals

- Implementing repositories (Spring Data JPA) - separate design/implementation
- Implementing REST controllers - separate design/implementation  
- Adding caching or advanced optimizations - future enhancement
- Implementing additional recommendation workflows beyond user recommendations
- Returning recommendation scores to users (may be added later)

## Architecture

### Layer Responsibilities

**Entity Services** (`src/main/java/org/tvl/tvlooker/service/`)
- One service per domain entity (User, Item, Interaction)
- Each service wraps its corresponding repository
- Provides CRUD operations + domain-specific queries
- Public API - can be reused across the application
- Throws domain exceptions for error cases (e.g., `UserNotFoundException`)
- Stateless, Spring-managed beans
- No `@Transactional` annotations - transactions managed at orchestration level

**RecommendationService** (`src/main/java/org/tvl/tvlooker/service/`)
- Orchestrates the recommendation workflow
- Composes entity services via constructor injection
- Loads ALL system data (users, items, interactions) needed for collaborative filtering
- Builds RecommendationContext directly (no separate builder class)
- Invokes HybridRecommendationEngine (handles parallel strategy execution internally)
- Throws domain exceptions (no custom result wrapper objects)
- Single transaction boundary at this level (`@Transactional(readOnly = true)`)
- Returns `List<Item>` - scores not exposed to users

**Controllers** (future implementation)
- Handle HTTP requests/responses
- Catch domain exceptions from services
- Translate exceptions to appropriate HTTP status codes and error messages
- Return JSON responses to clients

**Key Principles:**
- Service composition over direct repository access
- Exceptions for error handling (no custom result wrappers like RecommendationResult)
- Single responsibility - each service owns one entity's operations
- Transaction boundaries at the orchestration level (RecommendationService)
- Load all data upfront for collaborative filtering algorithms

### Component Structure

```
service/
├── UserService.java
├── ItemService.java
├── InteractionService.java
└── RecommendationService.java
```

**Removed from original design:**
- `dto/RecommendationResult.java` - deleted (controllers handle errors)
- `dto/RecommendationStatus.java` - deleted (controllers handle errors)
- `builder/RecommendationContextBuilder.java` - deleted (RecommendationService builds context inline)
- `RecommendationServiceImpl.java` - no interface/impl separation needed initially
- `ReviewService`, `ListFavoriteService`, `GenreService`, `ActorService`, `DirectorService` - not needed for recommendations

### Data Flow

```
Controller → RecommendationService.getUserRecommendations(userId, limit)
                      ↓
               Validate userId and limit
                      ↓
               Verify user exists via UserService
                 [throws UserNotFoundException if not found]
                      ↓
               Load ALL system data from entity services:
                 - UserService.getAllUsers()
                 - ItemService.getAllItems()
                 - InteractionService.getAllInteractions()
                      ↓
               Build RecommendationContext directly:
                 [context = new RecommendationContext(targetUserId, allUsers, allItems, allInteractions, limit)]
                      ↓
               RecommendationEngine.recommend(context)
                 [Parallel execution of strategies via ParallelStreams]
                 [Returns List<ScoredItem>]
                      ↓
               Extract items from ScoredItem results
                 [recommendations.stream().map(ScoredItem::getItem).toList()]
                      ↓
               Return List<Item>
                      ↓
               Controller receives result
                      ↓
               [If exception thrown during flow]
                      ↓
               Controller catches exception
                      ↓
               Controller returns appropriate HTTP error response
```

**Transaction Scope:** The entire flow runs in a single read-only transaction started by `@Transactional(readOnly = true)` on `RecommendationService.getUserRecommendations()`. Entity service methods are NOT annotated with `@Transactional`.

## Component Details

### UserService

```java
package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Service for User entity operations.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * Get user by ID.
     * 
     * @param userId the user ID
     * @return the user
     * @throws UserNotFoundException if user not found
     */
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }
    
    /**
     * Get all users in the system.
     * Used by recommendation engine for collaborative filtering.
     * 
     * @return list of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
```

### ItemService

```java
package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Service for Item entity operations.
 */
@Service
@RequiredArgsConstructor
public class ItemService {
    
    private final ItemRepository itemRepository;
    
    /**
     * Get all items in the system.
     * Used by recommendation engine as candidate items and for similarity computation.
     * 
     * @return list of all items
     */
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }
}
```

### InteractionService

```java
package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.model.entity.Interaction;
import org.tvl.tvlooker.persistence.repository.InteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Service for Interaction entity operations.
 */
@Service
@RequiredArgsConstructor
public class InteractionService {
    
    private final InteractionRepository interactionRepository;
    
    /**
     * Get all interactions in the system.
     * Used by recommendation engine for collaborative filtering and matrix factorization.
     * 
     * @return list of all interactions
     */
    public List<Interaction> getAllInteractions() {
        return interactionRepository.findAll();
    }
}
```

**Key Points:**
- Services throw domain exceptions (e.g., `UserNotFoundException`) instead of returning null/Optional
- Methods return entities directly or lists
- No `@Transactional` on entity services - transaction managed at RecommendationService level
- Public visibility - can be used by other features (admin panel, analytics, etc.)

### RecommendationService

```java
package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.motor.RecommendationEngine;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.entity.*;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for generating personalized recommendations for users.
 * Orchestrates entity services and the recommendation engine.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    // Entity services
    private final UserService userService;
    private final ItemService itemService;
    private final InteractionService interactionService;

    // Recommendation engine
    private final RecommendationEngine recommendationEngine;

    /**
     * Get personalized recommendations for a user.
     *
     * @param userId the ID of the user
     * @param limit maximum number of recommendations to return
     * @return list of recommended items
     * @throws UserNotFoundException if user doesn't exist
     * @throws InsufficientDataException if engine can't generate recommendations
     * @throws IllegalArgumentException if userId is null or limit is invalid
     */
    @Transactional(readOnly = true)
    public List<Item> getUserRecommendations(UUID userId, int limit) {
        // 1. Validate input
        validateInput(userId, limit);

        // 2. Verify user exists (throws UserNotFoundException if not found)
        userService.getById(userId);

        // 3. Load ALL system data for recommendation algorithms
        // Note: Collaborative filtering and matrix factorization need complete data
        List<User> allUsers = userService.getAll();
        List<Item> allItems = itemService.getAllItems();
        List<Interaction> allInteractions = interactionService.getAllInteractions();

        // 4. Build RecommendationContext with all system data
        RecommendationContext context = buildContext(
                userId,
                allUsers,
                allItems,
                allInteractions,
                limit
        );

        // 5. Get recommendations from engine
        // Engine returns List<ScoredItem> internally
        List<ScoredItem> scoredRecommendations = recommendationEngine.recommend(context);

        // 6. Extract items from ScoredItem results
        // We don't expose scores to the user (for now)
        List<Item> recommendations = scoredRecommendations.stream()
                .map(ScoredItem::getItem)
                .toList();

        return recommendations;
    }

    private void validateInput(UUID userId, int limit) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }

    private RecommendationContext buildContext(
            UUID targetUserId,
            List<User> allUsers,
            List<Item> allItems,
            List<Interaction> allInteractions,
            int limit) {

        return RecommendationContext.builder()
                .targetUserId(targetUserId)         // The user we're generating recommendations for
                .users(allUsers)                    // All users in system (for collaborative filtering)
                .items(allItems)                    // All items in system (candidates + similarity computation)
                .interactions(allInteractions)      // All interactions (for collaborative filtering)
                .limit(limit)
                .build();
    }
}
```

**Key Characteristics:**
- Composes 3 entity services + recommendation engine
- Single `@Transactional(readOnly = true)` boundary
- Loads ALL system data needed for collaborative filtering algorithms
- Builds RecommendationContext inline (no separate builder class)
- Returns `List<Item>` directly - scores not exposed to users
- Throws exceptions for all error cases - controller handles translation to HTTP responses
- Thin orchestration - fetches data, builds context, delegates to engine

## Error Handling Strategy

### Exception-Based Error Handling

The service layer throws domain exceptions; controllers catch and translate them to HTTP responses. No custom result wrapper objects.

### Domain Exceptions

**New exception to create:**

```java
package org.tvl.tvlooker.domain.exception;

/**
 * Thrown when a requested user is not found.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
```

**Existing domain exceptions** (from recommendation engine design):
- `InsufficientDataException` - Engine cannot generate recommendations
- `NoRecommendationsAvailableException` - No suitable items found
- `NoDataProviderException` - Missing required data provider
- `InvalidEngineConfigurationException` - Engine misconfigured

### Exception Translation (Controller Layer)

Controllers will use `@ExceptionHandler` or `@ControllerAdvice` to catch exceptions and return appropriate HTTP responses:

| Exception | HTTP Status | Response Message |
|-----------|-------------|------------------|
| `UserNotFoundException` | 404 Not Found | "User not found: {userId}" |
| `IllegalArgumentException` | 400 Bad Request | "Invalid input: {message}" |
| `NoRecommendationsAvailableException` | 200 OK (empty list) | "No recommendations available" |
| `InsufficientDataException` | 500 Internal Server Error | "Unable to generate recommendations" |
| `NoDataProviderException` | 500 Internal Server Error | "System configuration error" |
| `InvalidEngineConfigurationException` | 500 Internal Server Error | "Recommendation engine misconfigured" |
| Any other `RuntimeException` | 500 Internal Server Error | "An error occurred" |

### Key Insights

**With Popularity Fallback:**
- New users (no interactions) → SUCCESS with popularity-based recommendations
- `InsufficientDataException` becomes a **system error** (should rarely occur with popularity fallback)
- Normal flow always returns recommendations for valid users

**No Custom Result Wrappers:**
- Services return data directly (`List<Item>`) or throw exceptions
- Controllers handle HTTP response construction
- Cleaner service layer, simpler testing
- Follows Spring best practices

## Dependencies

### Service Dependencies (Constructor Injection)

```java
@Service
@RequiredArgsConstructor
public class RecommendationService {
    
    // Entity services - provide data access
    private final UserService userService;
    private final ItemService itemService;
    private final InteractionService interactionService;
    
    // Domain dependency - orchestrates recommendation strategies
    private final RecommendationEngine recommendationEngine;
}
```

**Notes:**
- All dependencies managed by Spring (constructor injection via Lombok `@RequiredArgsConstructor`)
- Entity services wrap Spring Data JPA repositories
- `RecommendationEngine` is already configured as a Spring bean in `RecommendationConfig.java`

### Repository Methods Needed

Each repository needs methods for bulk data access:

- `UserRepository.findById(UUID userId)` - returns `Optional<User>` (already exists)
- `UserRepository.findAll()` - returns `List<User>` (Spring Data JPA provides)
- `ItemRepository.findAll()` - returns `List<Item>` (Spring Data JPA provides)
- `InteractionRepository.findAll()` - returns `List<Interaction>` (Spring Data JPA provides)

### Transaction Management

- Transaction boundary at RecommendationService level: `@Transactional(readOnly = true)`
- Entity services are NOT annotated with `@Transactional`
- Single read-only transaction covers entire recommendation flow
- No write operations occur in recommendation workflow

## Data Loading Strategy

### Why Load All Data?

The recommendation algorithms require different data scopes:

| Algorithm | Data Requirements |
|-----------|------------------|
| **Popularity** | All items (to rank by popularity) |
| **Content-Based** | All items (to find similar items to user's history) |
| **User-Based Collaborative Filtering** | All users + all items + all interactions (to compute user similarity matrix) |
| **Item-Based Collaborative Filtering** | All users + all items + all interactions (to compute item similarity matrix) |
| **Matrix Factorization (SVD)** | All users + all items + all interactions (to build rating matrix) |

**Strategy:** Load all data upfront and pass to RecommendationContext. The context and DataProviders handle caching and lazy computation of derived structures (similarity matrices, feature vectors, etc.).

### Performance Considerations

- **Current scope:** This design assumes a reasonable dataset size (thousands of users/items)
- **Future optimization:** If data grows large, consider:
  - Caching computed data structures (similarity matrices) in Redis
  - Pre-computing recommendations in background jobs
  - Sampling strategies for very large datasets
  - Lazy loading via DataProviders

## Parallel Execution Strategy

### Where Parallelization Happens

**Inside the Domain Layer (Recommendation Engine):**
- `HybridRecommendationEngine` coordinates parallel execution of multiple strategies using `CompletableFuture` or ParallelStreams
- Each `RecommendationStrategy` uses **ParallelStreams** for data processing (calculating similarities, filtering items)
- `AggregationStrategy` may use parallel processing when combining strategy results

**Service Layer Remains Synchronous:**
- Service methods are synchronous - controller calls service, waits for result
- Service makes a single call: `recommendationEngine.recommend(context)`
- Engine handles all parallelization internally
- Service receives fully computed results and returns them

### Why This Design?

1. **Thin service layer** - orchestration logic stays simple
2. **Domain encapsulation** - parallelization strategy is a domain concern, not a service concern
3. **Flexibility** - engine can evolve its parallelization approach without changing service interface
4. **Testability** - service tests mock a single engine call, not complex parallel coordination

## Testing Strategy

### Unit Tests

**Entity Service Tests (Example: UserServiceTest)**
- Mock repository
- Test scenarios:
  - `getUserById()` with existing user returns user
  - `getUserById()` with non-existent user throws `UserNotFoundException`
  - `getAllUsers()` returns all users
- Similar tests for ItemService, InteractionService
- Target: 90%+ line coverage per service

**RecommendationServiceTest**
- Mock all dependencies (UserService, ItemService, InteractionService, RecommendationEngine)
- Test scenarios:
  - **Happy path**: Valid user returns list of items
  - **User not found**: UserService throws `UserNotFoundException`, exception propagates
  - **Null userId**: throws `IllegalArgumentException`
  - **Invalid limit** (≤0): throws `IllegalArgumentException`
  - **Engine throws InsufficientDataException**: exception propagates to controller
  - **Verify service composition**: All entity services called correctly
  - **Verify context building**: Context contains target userId + all system data
  - **Verify item extraction**: ScoredItems converted to Items correctly
- Use Mockito for mocking, JUnit 5 for assertions
- Target: 90%+ line coverage

### Integration Tests

**RecommendationServiceIntegrationTest**
- Use `@SpringBootTest` with test database (H2)
- Seed test data:
  - Multiple users with different interaction patterns
  - Items (movies/TV shows) with metadata
  - Interactions (ratings, watches)
- Test full flow: RecommendationService → Entity Services → Repositories → Engine → Result
- Test scenarios:
  - User with rich interaction history gets personalized recommendations
  - New user (no interactions) gets popularity-based recommendations
  - Non-existent user throws `UserNotFoundException`
  - Verify actual recommendations returned (not mocked)
  - Verify items exist in database
- Use `@Sql` for test data setup/teardown

### Test Data Builders

Create test builders for entities:
- `UserTestBuilder`
- `ItemTestBuilder`
- `InteractionTestBuilder`
- Use builder pattern for readable test construction

### Coverage Goals

- Service layer: 90%+ line coverage
- All exception paths tested
- Integration tests cover realistic end-to-end scenarios

## Implementation Plan

### Phase 1: Entity Services Foundation
1. Create/update entity services with required methods:
   - **UserService**: `getUserById(UUID)`, `getAllUsers()`
   - **ItemService**: `getAllItems()`
   - **InteractionService**: `getAllInteractions()`
2. Create `UserNotFoundException` domain exception
3. Update repositories if needed (ensure `findAll()` methods exist)
4. Write unit tests for each entity service

### Phase 2: RecommendationService Implementation
1. Update `RecommendationService`:
   - Inject required entity services + recommendation engine
   - Implement `getUserRecommendations(UUID, int)` method
   - Build RecommendationContext with all system data
   - Extract items from ScoredItem results
2. Add `@Transactional(readOnly = true)` at service method level
3. Write comprehensive unit tests (mock all dependencies)

### Phase 3: Cleanup Obsolete Code
1. Delete `dto/RecommendationResult.java` (if exists)
2. Delete `dto/RecommendationStatus.java` (if exists)
3. Delete `builder/RecommendationContextBuilder.java` (if exists)
4. Delete `RecommendationServiceImpl.java` (if exists)
5. Remove obsolete services: `ReviewService`, `ListFavoriteService`, `GenreService`, `ActorService`, `DirectorService`
6. Update any references to deleted classes

### Phase 4: Integration Testing
1. Set up integration test infrastructure
2. Create test data fixtures and builders
3. Write integration tests for full recommendation flow
4. Verify end-to-end functionality with real database

### Phase 5: Documentation & Review
1. Add JavaDoc to all public interfaces and classes
2. Update design document with final implementation notes
3. Code review and refinement

## Future Enhancements

These are explicitly out of scope for the initial implementation but documented for future consideration:

1. **Caching:** Add caching layer (e.g., Redis) to store computed recommendations and data structures
2. **Async API:** Expose async endpoints for long-running recommendation jobs
3. **Batch recommendations:** Pre-compute recommendations for all users in background jobs
4. **Additional workflows:** Genre-based recommendations, similar items, trending content
5. **Monitoring:** Add metrics and logging for recommendation performance
6. **A/B testing:** Support for testing different recommendation strategies
7. **Personalization parameters:** Allow users to adjust preferences (prefer recent, highly-rated, etc.)
8. **Expose scores:** Return `List<ScoredItem>` to show users why items were recommended
9. **Pagination:** Support for large result sets
10. **Filtering:** Allow users to filter recommendations by genre, year, etc.

## Open Questions

None at this time. Design is approved and ready for implementation.

## References

- [Recommendation Engine Design (2026-03-05)](./2026-03-05-recommendation-engine-design.md)
- [Recommendation Strategies Design (2026-03-06)](./2026-03-06-recommendation-strategies-and-aggregations-design.md)
- Existing codebase: `src/main/java/org/tvl/tvlooker/domain/motor/`

## Changelog

**2026-03-14:**
- Revised architecture to use service composition pattern (one service per entity)
- Removed RecommendationResult/RecommendationStatus DTOs (controllers handle exceptions)
- Removed RecommendationContextBuilder (context built inline in RecommendationService)
- Changed to load ALL system data (users, items, interactions) for collaborative filtering
- Changed return type to `List<Item>` (scores hidden from users for now)
- Simplified to 3 core entity services: UserService, ItemService, InteractionService
- Added exception-based error handling strategy
- Updated all component details and implementation plan
