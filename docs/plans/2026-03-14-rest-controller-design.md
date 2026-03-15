# REST Controller Layer Design

**Date:** 2026-03-14  
**Status:** Approved  
**Author:** OpenCode AI Assistant

## Overview

This document describes the design of the REST controller layer for the TV Looker application. The design introduces a three-layer model architecture to properly decouple JPA entities from business logic and API contracts.

## Goals

1. Create RESTful API with versioning (`/api/v1/*`)
2. Decouple JPA entities from business logic (3-layer model: Entity → Domain → DTO)
3. Implement proper exception handling with RFC 7807 error responses
4. Support full CRUD for user-generated entities (User, Interaction, Review, ListFavorite)
5. Support read-only for reference data (Item, Genre, Director, Actor)
6. Expose recommendation endpoint (`GET /users/{id}/recommendations`)
7. Use Jakarta Bean Validation for request validation
8. Follow separation of concerns (Controller → Service → Repository → Database)
9. Fix architectural issues (Item as Map key, entity coupling)

## Non-Goals

1. Pagination - Not implemented in this design (future enhancement)
2. Authentication/Authorization - No security layer (separate design needed)
3. Rate Limiting - Not included (future enhancement)
4. Caching - No caching strategy (future enhancement)
5. API Documentation - No Swagger/OpenAPI specs (separate task)
6. Filtering/Sorting - Basic list endpoints only (future enhancement)
7. Batch Operations - Single resource operations only
8. HATEOAS - No hypermedia links in responses
9. Versioning Strategy Beyond v1 - Only v1 defined
10. GraphQL Alternative - REST only
11. WebSocket/SSE - Synchronous HTTP only
12. File Uploads - Not applicable to current domain

## Architecture

### Three-Layer Model Strategy

The design introduces three distinct model layers with strict boundaries:

#### 1. JPA Entities (`domain.model.entity.*`) - Persistence Layer ONLY

- `User`, `Item`, `Interaction`, `Review`, `ListFavorite`, `Genre`, `Director`, `Actor`
- Annotated with `@Entity`, `@Table`, JPA relationships
- Used ONLY within `persistence.repository.*` package
- Never leave the repository layer - repositories convert to domain models at boundary
- Private to persistence infrastructure

#### 2. Domain Models (`domain.model.*`) - Service & Business Logic Layer

- `UserData`, `ItemData`, `InteractionData`, `ReviewData`, `ListFavoriteData`, etc.
- Used by ALL services (UserService, RecommendationService, etc.)
- Used by RecommendationEngine, strategies, aggregation logic
- No JPA annotations, no persistence concerns, no API concerns
- Minimal POJOs with only business-relevant fields
- Immutable where possible (final fields, builders, no setters)
- This is the "source of truth" for business logic

#### 3. API DTOs (`api.dto.*`) - Presentation Layer

- Request DTOs: `CreateUserRequest`, `UpdateUserRequest`, etc.
- Response DTOs: `UserResponse`, `ItemResponse`, `RecommendationResponse`, etc.
- Annotated with `@Valid`, `@NotNull`, `@Size` for validation
- Tailored to API client needs (may omit sensitive fields, add computed fields)
- RFC 7807 error responses
- Controllers convert between domain models and DTOs

### Layer Boundaries & Responsibilities

```
┌─────────────────────────────────────────────────┐
│         API Layer (Controllers)                 │
│  - Handle HTTP requests/responses               │
│  - Use API DTOs (Request/Response)              │
│  - Convert: API DTOs ↔ Domain Models            │
│  - Catch exceptions, return RFC 7807 errors     │
└────────────────┬────────────────────────────────┘
                 ↓ Domain Models
┌─────────────────────────────────────────────────┐
│         Service Layer                           │
│  - Business logic orchestration                 │
│  - Use Domain Models exclusively                │
│  - No knowledge of JPA entities or API DTOs     │
│  - RecommendationService uses Domain Models     │
└────────────────┬────────────────────────────────┘
                 ↓ Domain Models
┌─────────────────────────────────────────────────┐
│         Domain Motor Layer                      │
│  - RecommendationEngine, Strategies             │
│  - Use Domain Models exclusively                │
│  - Zero persistence or API knowledge            │
└─────────────────────────────────────────────────┘
                 ↑ Domain Models
┌─────────────────────────────────────────────────┐
│         Service Layer (continued)               │
│  - Calls repositories for data access           │
│  - Receives Domain Models from repositories     │
└────────────────┬────────────────────────────────┘
                 ↓ Domain Models ↔ JPA Entities
┌─────────────────────────────────────────────────┐
│         Persistence Layer (Repositories)        │
│  - Spring Data JPA repositories                 │
│  - Use JPA Entities internally                  │
│  - Convert: JPA Entities ↔ Domain Models        │
│  - Public API returns/accepts Domain Models     │
└─────────────────────────────────────────────────┘
```

### Data Flow Example (Recommendations)

```
1. Client sends: GET /api/v1/users/{userId}/recommendations
2. Controller receives request
3. Controller extracts userId from path
4. Controller calls: RecommendationService.getUserRecommendations(userId, limit)
   ↓ (Domain Models flow through service layer)
5. RecommendationService calls repositories (returns Domain Models):
   - UserRepository.findById(id) → Optional<UserData>
   - ItemRepository.findAll() → List<ItemData>
   - InteractionRepository.findAll() → List<InteractionData>
   ↓ (Repository converts JPA Entity → Domain Model internally)
6. RecommendationService builds RecommendationContext with Domain Models
7. RecommendationEngine.recommend(userData, context) → List<ScoredItem>
   ↓ (All motor logic uses Domain Models)
8. Service returns List<ItemData> to controller
9. Controller converts: List<ItemData> → List<ItemResponse>
10. Controller returns JSON response
```

### Key Principles

- **JPA Entities are Private**: Never escape the repository implementation
- **Domain Models are Universal**: Used by repositories, services, and motor layer
- **API DTOs are External**: Only at the controller boundary for HTTP contracts
- **Single Source of Truth**: Domain models define business concepts
- **Repository Responsibility**: Repositories handle all JPA ↔ Domain conversion
- **Service Purity**: Services know nothing about persistence or HTTP

## API Endpoints

### Base URL

`/api/v1`

### Endpoint Overview

| Entity | Endpoints | Methods | Description |
|--------|-----------|---------|-------------|
| **User** | `/users` | GET, POST | List all, Create |
| | `/users/{id}` | GET, PUT, DELETE | Get, Update, Delete |
| | `/users/{id}/recommendations` | GET | Get recommendations |
| **Item** | `/items` | GET | List all (read-only) |
| | `/items/{id}` | GET | Get by id (read-only) |
| **Interaction** | `/interactions` | GET, POST | List all, Create |
| | `/interactions/{id}` | GET, PUT, DELETE | Get, Update, Delete |
| **Review** | `/reviews` | GET, POST | List all, Create |
| | `/reviews/{id}` | GET, PUT, DELETE | Get, Update, Delete |
| **ListFavorite** | `/lists` | GET, POST | List all, Create |
| | `/lists/{id}` | GET, PUT, DELETE | Get, Update, Delete |
| **Genre** | `/genres` | GET | List all (read-only) |
| | `/genres/{id}` | GET | Get by id (read-only) |
| **Director** | `/directors` | GET | List all (read-only) |
| | `/directors/{id}` | GET | Get by id (read-only) |
| **Actor** | `/actors` | GET | List all (read-only) |
| | `/actors/{id}` | GET | Get by id (read-only) |

### Detailed Endpoint Specifications

#### 1. Recommendations

```
GET /api/v1/users/{userId}/recommendations?limit=10
```

- **Description:** Get personalized recommendations for a user
- **Path Params:** `userId` (UUID)
- **Query Params:** `limit` (int, optional, default=10, max=100)
- **Success Response:** `200 OK` + `RecommendationResponse`
- **Error Responses:**
  - `400 Bad Request` - Invalid userId or limit
  - `404 Not Found` - User not found
  - `500 Internal Server Error` - Recommendation engine failure

#### 2. User CRUD

```
GET    /api/v1/users           → List all users
POST   /api/v1/users           → Create user
GET    /api/v1/users/{id}      → Get user by id
PUT    /api/v1/users/{id}      → Update user
DELETE /api/v1/users/{id}      → Delete user
```

- **ID Type:** UUID
- **Create/Update:** Validates email format, required fields

#### 3. Item (Read-Only)

```
GET /api/v1/items           → List all items
GET /api/v1/items/{id}      → Get item by id
```

- **ID Type:** Long
- **Read-Only:** Items populated from TMDB, no create/update/delete

#### 4. Interaction CRUD

```
GET    /api/v1/interactions           → List all interactions
POST   /api/v1/interactions           → Create interaction
GET    /api/v1/interactions/{id}      → Get interaction by id
PUT    /api/v1/interactions/{id}      → Update interaction
DELETE /api/v1/interactions/{id}      → Delete interaction
```

- **ID Type:** Long
- **Validation:** userId and itemId must exist, interactionType required

#### 5. Review CRUD

```
GET    /api/v1/reviews           → List all reviews
POST   /api/v1/reviews           → Create review
GET    /api/v1/reviews/{id}      → Get review by id
PUT    /api/v1/reviews/{id}      → Update review
DELETE /api/v1/reviews/{id}      → Delete review
```

- **ID Type:** Long
- **Validation:** Rating 1-10, comment max 1000 chars

#### 6. ListFavorite CRUD

```
GET    /api/v1/lists           → List all favorite lists
POST   /api/v1/lists           → Create favorite list
GET    /api/v1/lists/{id}      → Get list by id
PUT    /api/v1/lists/{id}      → Update list
DELETE /api/v1/lists/{id}      → Delete list
```

- **ID Type:** Long

#### 7. Reference Data (Read-Only)

```
GET /api/v1/genres           → List all genres
GET /api/v1/genres/{id}      → Get genre by id
GET /api/v1/directors        → List all directors
GET /api/v1/directors/{id}   → Get director by id
GET /api/v1/actors           → List all actors
GET /api/v1/actors/{id}      → Get actor by id
```

- **ID Type:** Long
- **Read-Only:** Reference data from TMDB

### HTTP Status Codes

| Code | Usage |
|------|-------|
| `200 OK` | Successful GET, PUT |
| `201 Created` | Successful POST, includes Location header |
| `204 No Content` | Successful DELETE |
| `400 Bad Request` | Validation errors, invalid parameters |
| `404 Not Found` | Resource not found (user, item, etc.) |
| `500 Internal Server Error` | Unexpected errors, engine failures |

## Component Details

### Domain Models

Domain models are minimal POJOs used by services and the recommendation motor.

#### UserData

```java
package org.tvl.tvlooker.domain.model;

@Builder
@Getter
@AllArgsConstructor
public class UserData {
    private final UUID id;
    private final String username;
    private final String email;
    private final String name;
    private final Timestamp createdAt;
}
```

#### ItemData

```java
package org.tvl.tvlooker.domain.model;

@Builder
@Getter
@AllArgsConstructor
public class ItemData {
    private final Long id;
    private final String title;
    private final String overview;
    private final LocalDate releaseDate;
    private final BigDecimal popularity;
    private final BigDecimal voteAverage;
    private final TmdbType tmdbType;
    private final Long tmdbId;
}
```

#### InteractionData

```java
package org.tvl.tvlooker.domain.model;

@Builder
@Getter
@AllArgsConstructor
public class InteractionData {
    private final Long id;
    private final UUID userId;
    private final Long itemId;
    private final InteractionType interactionType;
    private final Timestamp timestamp;
}
```

#### Additional Domain Models

- `ReviewData` - id, userId, itemId, rating, comment, timestamp
- `ListFavoriteData` - id, userId, itemId, listName, timestamp
- `GenreData` - id, tmdbId, name
- `DirectorData` - id, tmdbId, name
- `ActorData` - id, tmdbId, name

#### ScoredItem (REFACTORED)

```java
package org.tvl.tvlooker.domain.data_structure;

// REFACTOR: Change Item item → ItemData item
@Builder
@Getter
@AllArgsConstructor
public class ScoredItem {
    private final ItemData item;  // ← CHANGED from Item to ItemData
    private final double score;
    private final String explanation;
    private final String sourceStrategy;
}
```

### API DTOs

#### Request DTOs

**CreateUserRequest**

```java
package org.tvl.tvlooker.api.dto.request;

@Getter
@Setter
public class CreateUserRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;
}
```

**UpdateUserRequest**

```java
package org.tvl.tvlooker.api.dto.request;

@Getter
@Setter
public class UpdateUserRequest {
    @Size(min = 3, max = 50)
    private String username;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Size(max = 100)
    private String name;
}
```

**CreateInteractionRequest**

```java
package org.tvl.tvlooker.api.dto.request;

@Getter
@Setter
public class CreateInteractionRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Item ID is required")
    private Long itemId;
    
    @NotNull(message = "Interaction type is required")
    private InteractionType interactionType;
}
```

**CreateReviewRequest**

```java
package org.tvl.tvlooker.api.dto.request;

@Getter
@Setter
public class CreateReviewRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Item ID is required")
    private Long itemId;
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 10")
    @Max(value = 10, message = "Rating must be between 1 and 10")
    private Integer rating;
    
    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;
}
```

**CreateListFavoriteRequest**

```java
package org.tvl.tvlooker.api.dto.request;

@Getter
@Setter
public class CreateListFavoriteRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Item ID is required")
    private Long itemId;
    
    @NotBlank(message = "List name is required")
    @Size(max = 100)
    private String listName;
}
```

#### Response DTOs

**UserResponse**

```java
package org.tvl.tvlooker.api.dto.response;

@Builder
@Getter
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String name;
    private Timestamp createdAt;
}
```

**ItemResponse**

```java
package org.tvl.tvlooker.api.dto.response;

@Builder
@Getter
@AllArgsConstructor
public class ItemResponse {
    private Long id;
    private String title;
    private String overview;
    private LocalDate releaseDate;
    private BigDecimal popularity;
    private BigDecimal voteAverage;
    private TmdbType tmdbType;
    private Long tmdbId;
}
```

**InteractionResponse**

```java
package org.tvl.tvlooker.api.dto.response;

@Builder
@Getter
@AllArgsConstructor
public class InteractionResponse {
    private Long id;
    private UUID userId;
    private Long itemId;
    private InteractionType interactionType;
    private Timestamp timestamp;
}
```

**ReviewResponse**

```java
package org.tvl.tvlooker.api.dto.response;

@Builder
@Getter
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private UUID userId;
    private Long itemId;
    private Integer rating;
    private String comment;
    private Timestamp timestamp;
}
```

**RecommendationResponse**

```java
package org.tvl.tvlooker.api.dto.response;

@Builder
@Getter
@AllArgsConstructor
public class RecommendationResponse {
    private UUID userId;
    private int count;
    private List<ItemResponse> items;
}
```

**ErrorResponse (RFC 7807)**

```java
package org.tvl.tvlooker.api.dto.response;

@Builder
@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String type;        // e.g., "/errors/not-found"
    private String title;       // e.g., "User Not Found"
    private int status;         // HTTP status code
    private String detail;      // Detailed message
    private String instance;    // Request path
    private Timestamp timestamp;
}
```

### Mapper Classes

#### Entity Mappers (Persistence Layer)

Located in `persistence.mapper.*` package. Convert JPA Entity ↔ Domain Model.

- `UserEntityMapper` - `User` ↔ `UserData`
- `ItemEntityMapper` - `Item` ↔ `ItemData`
- `InteractionEntityMapper` - `Interaction` ↔ `InteractionData`
- `ReviewEntityMapper` - `Review` ↔ `ReviewData`
- `ListFavoriteEntityMapper` - `ListFavorite` ↔ `ListFavoriteData`
- `GenreEntityMapper` - `Genre` ↔ `GenreData`
- `DirectorEntityMapper` - `Director` ↔ `DirectorData`
- `ActorEntityMapper` - `Actor` ↔ `ActorData`

#### API Mappers (Presentation Layer)

Located in `api.dto.mapper.*` package. Convert Domain Model ↔ API DTO.

- `UserMapper` - `UserData` ↔ `UserResponse`, `CreateUserRequest` → `UserData`
- `ItemMapper` - `ItemData` ↔ `ItemResponse`
- `InteractionMapper` - `InteractionData` ↔ `InteractionResponse`
- `ReviewMapper` - `ReviewData` ↔ `ReviewResponse`
- `ListFavoriteMapper` - `ListFavoriteData` ↔ `ListFavoriteResponse`
- `GenreMapper` - `GenreData` ↔ `GenreResponse`
- `DirectorMapper` - `DirectorData` ↔ `DirectorResponse`
- `ActorMapper` - `ActorData` ↔ `ActorResponse`

Mappers are utility classes with static methods, no external dependencies like MapStruct.

### Controllers

#### UserController

```java
package org.tvl.tvlooker.api.controller;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;
    
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserData> users = userService.getAll();
        List<UserResponse> response = users.stream()
            .map(userMapper::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserData user = userService.getById(id);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserData userData = userMapper.fromCreateRequest(request);
        UserData created = userService.create(userData);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header("Location", "/api/v1/users/" + created.getId())
            .body(userMapper.toResponse(created));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserData userData = userMapper.fromUpdateRequest(id, request);
        UserData updated = userService.update(userData);
        return ResponseEntity.ok(userMapper.toResponse(updated));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

#### RecommendationController

```java
package org.tvl.tvlooker.api.controller;

@RestController
@RequestMapping("/api/v1/users/{userId}/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final ItemMapper itemMapper;
    
    @GetMapping
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        
        List<ItemData> recommendations = 
            recommendationService.getUserRecommendations(userId, limit);
        
        List<ItemResponse> items = recommendations.stream()
            .map(itemMapper::toResponse)
            .toList();
            
        RecommendationResponse response = RecommendationResponse.builder()
            .userId(userId)
            .count(items.size())
            .items(items)
            .build();
            
        return ResponseEntity.ok(response);
    }
}
```

#### ItemController (Read-Only)

```java
package org.tvl.tvlooker.api.controller;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
    private final ItemMapper itemMapper;
    
    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        List<ItemData> items = itemService.getAll();
        List<ItemResponse> response = items.stream()
            .map(itemMapper::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable Long id) {
        ItemData item = itemService.getById(id);
        return ResponseEntity.ok(itemMapper.toResponse(item));
    }
}
```

#### Complete Controller List

**Full CRUD Controllers:**
- `UserController` - `/api/v1/users`
- `InteractionController` - `/api/v1/interactions`
- `ReviewController` - `/api/v1/reviews`
- `ListFavoriteController` - `/api/v1/lists`

**Read-Only Controllers:**
- `ItemController` - `/api/v1/items`
- `GenreController` - `/api/v1/genres`
- `DirectorController` - `/api/v1/directors`
- `ActorController` - `/api/v1/actors`

**Special Controllers:**
- `RecommendationController` - `/api/v1/users/{userId}/recommendations`

### Global Exception Handler

```java
package org.tvl.tvlooker.api.exception;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, 
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
            "/errors/user-not-found",
            "User Not Found",
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            request.getRequestURI()
        );
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleItemNotFound(
            ItemNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
            "/errors/item-not-found",
            "Item Not Found",
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            request.getRequestURI()
        );
        log.warn("Item not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
            "/errors/bad-request",
            "Invalid Request",
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            request.getRequestURI()
        );
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
            
        ErrorResponse error = buildErrorResponse(
            "/errors/validation-failed",
            "Validation Failed",
            HttpStatus.BAD_REQUEST.value(),
            errors,
            request.getRequestURI()
        );
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(InsufficientDataException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientData(
            InsufficientDataException ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
            "/errors/insufficient-data",
            "Insufficient Data",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ex.getMessage(),
            request.getRequestURI()
        );
        log.error("Insufficient data for recommendations: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        ErrorResponse error = buildErrorResponse(
            "/errors/internal-error",
            "Internal Server Error",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            request.getRequestURI()
        );
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    private ErrorResponse buildErrorResponse(
            String type, 
            String title, 
            int status, 
            String detail, 
            String instance) {
        return ErrorResponse.builder()
            .type(type)
            .title(title)
            .status(status)
            .detail(detail)
            .instance(instance)
            .timestamp(new Timestamp(System.currentTimeMillis()))
            .build();
    }
}
```

### Exception to HTTP Status Mapping

| Exception | HTTP Status | Type |
|-----------|-------------|------|
| `UserNotFoundException` | 404 | `/errors/user-not-found` |
| `ItemNotFoundException` | 404 | `/errors/item-not-found` |
| `InteractionNotFoundException` | 404 | `/errors/interaction-not-found` |
| `ReviewNotFoundException` | 404 | `/errors/review-not-found` |
| `IllegalArgumentException` | 400 | `/errors/bad-request` |
| `MethodArgumentNotValidException` | 400 | `/errors/validation-failed` |
| `InsufficientDataException` | 500 | `/errors/insufficient-data` |
| `NoRecommendationsAvailableException` | 500 | `/errors/no-recommendations` |
| Generic `Exception` | 500 | `/errors/internal-error` |

## Migration Plan

### Phase 1: Create Domain Models

1. Create domain model classes in `domain.model.*` package
   - `UserData`, `ItemData`, `InteractionData`, `ReviewData`, `ListFavoriteData`, `GenreData`, `DirectorData`, `ActorData`
2. Refactor `ScoredItem` to use `ItemData` instead of JPA `Item` entity
3. Keep domain models minimal - only fields needed for business logic

### Phase 2: Update Repositories

1. Create entity mappers in `persistence.mapper.*` package
   - `UserEntityMapper`, `ItemEntityMapper`, etc.
   - Static methods for JPA Entity ↔ Domain Model conversion
2. Refactor repository interfaces to return domain models:
   - **Before:** `Optional<User> findById(UUID id)`
   - **After:** `Optional<UserData> findById(UUID id)`
3. Update repository implementations to use mappers

### Phase 3: Update Services

1. Refactor all service methods to use domain models:
   - **Before:** `public User getById(UUID id)`
   - **After:** `public UserData getById(UUID id)`
2. Update `RecommendationService` to use domain models
3. Update `RecommendationContext` to use domain models

### Phase 4: Update Domain Motor Layer

1. Update `RecommendationEngine` interface to use `UserData`
2. Update `HybridRecommendationEngine`:
   - Change all `User` parameters to `UserData`
   - Change all `Item` references to `ItemData`
   - Fix Map key issue: `Map<Item, ScoredItem>` → `Map<Long, ScoredItem>`
3. Update strategy interfaces and implementations
4. Update data providers

### Phase 5: Create API Layer

1. Create API DTOs in `api.dto.*` package
2. Create API mappers in `api.dto.mapper.*` package
3. Create controllers in `api.controller.*` package
4. Create global exception handler in `api.exception.*` package

### Phase 6: Update Tests

1. Update service layer unit tests
2. Update integration tests
3. Create controller tests

### Critical Refactoring Points

**Issue #1: Map Key in HybridRecommendationEngine (line 163)**

```java
// BEFORE (BROKEN)
Map<Item, ScoredItem> seenItems = new HashMap<>();

// AFTER (FIXED)
Map<Long, ScoredItem> seenItems = new HashMap<>();
// Use item.getId() as key instead of Item object
```

**Issue #2: Repository Method Signatures**

```java
// BEFORE
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findById(UUID id);
}

// AFTER
public interface UserRepository extends JpaRepository<User, UUID> {
    default Optional<UserData> findByIdAsData(UUID id) {
        return findById(id).map(UserEntityMapper::toData);
    }
}
```

**Issue #3: Service Layer Transition**

```java
// BEFORE
public User getById(UUID id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
}

// AFTER
public UserData getById(UUID id) {
    return userRepository.findByIdAsData(id)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
}
```

### Migration Order

Recommended order to minimize breaking changes:

1. **User** (simplest, UUID-based ID, no complex relationships in motor)
2. **Item** (used heavily in motor, critical to get right)
3. **Interaction** (links User and Item)
4. **Review, ListFavorite** (less critical for recommendations)
5. **Genre, Director, Actor** (reference data, read-only)
6. **RecommendationEngine** (update after all entities migrated)
7. **Services** (update after repositories)
8. **Controllers** (final layer, uses already-migrated services)

## Package Structure

```
org.tvl.tvlooker/
├── api/
│   ├── controller/
│   │   ├── UserController.java
│   │   ├── ItemController.java
│   │   ├── InteractionController.java
│   │   ├── ReviewController.java
│   │   ├── ListFavoriteController.java
│   │   ├── GenreController.java
│   │   ├── DirectorController.java
│   │   ├── ActorController.java
│   │   └── RecommendationController.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CreateUserRequest.java
│   │   │   ├── UpdateUserRequest.java
│   │   │   ├── CreateInteractionRequest.java
│   │   │   ├── UpdateInteractionRequest.java
│   │   │   ├── CreateReviewRequest.java
│   │   │   ├── UpdateReviewRequest.java
│   │   │   ├── CreateListFavoriteRequest.java
│   │   │   └── UpdateListFavoriteRequest.java
│   │   ├── response/
│   │   │   ├── UserResponse.java
│   │   │   ├── ItemResponse.java
│   │   │   ├── InteractionResponse.java
│   │   │   ├── ReviewResponse.java
│   │   │   ├── ListFavoriteResponse.java
│   │   │   ├── GenreResponse.java
│   │   │   ├── DirectorResponse.java
│   │   │   ├── ActorResponse.java
│   │   │   ├── RecommendationResponse.java
│   │   │   └── ErrorResponse.java
│   │   └── mapper/
│   │       ├── UserMapper.java
│   │       ├── ItemMapper.java
│   │       ├── InteractionMapper.java
│   │       ├── ReviewMapper.java
│   │       ├── ListFavoriteMapper.java
│   │       ├── GenreMapper.java
│   │       ├── DirectorMapper.java
│   │       └── ActorMapper.java
│   └── exception/
│       └── GlobalExceptionHandler.java
│
├── domain/
│   ├── model/
│   │   ├── entity/ (JPA entities - unchanged)
│   │   │   ├── User.java
│   │   │   ├── Item.java
│   │   │   ├── Interaction.java
│   │   │   ├── Review.java
│   │   │   ├── ListFavorite.java
│   │   │   ├── Genre.java
│   │   │   ├── Director.java
│   │   │   └── Actor.java
│   │   ├── UserData.java (NEW)
│   │   ├── ItemData.java (NEW)
│   │   ├── InteractionData.java (NEW)
│   │   ├── ReviewData.java (NEW)
│   │   ├── ListFavoriteData.java (NEW)
│   │   ├── GenreData.java (NEW)
│   │   ├── DirectorData.java (NEW)
│   │   └── ActorData.java (NEW)
│   ├── data_structure/
│   │   └── ScoredItem.java (REFACTORED)
│   ├── motor/
│   │   ├── RecommendationEngine.java (REFACTORED)
│   │   ├── HybridRecommendationEngine.java (REFACTORED)
│   │   └── utils/
│   │       ├── RecommendationContext.java (REFACTORED)
│   │       └── provider/
│   │           └── ItemPopularityProvider.java (REFACTORED)
│   ├── strategy/
│   │   ├── recommendation/ (REFACTORED)
│   │   └── aggregation/ (REFACTORED)
│   └── exception/ (unchanged)
│
├── service/
│   ├── UserService.java (REFACTORED)
│   ├── ItemService.java (REFACTORED)
│   ├── InteractionService.java (REFACTORED)
│   ├── ReviewService.java (REFACTORED)
│   ├── ListFavoriteService.java (REFACTORED)
│   ├── GenreService.java (REFACTORED)
│   ├── DirectorService.java (REFACTORED)
│   ├── ActorService.java (REFACTORED)
│   └── RecommendationService.java (REFACTORED)
│
└── persistence/
    ├── repository/
    │   ├── UserRepository.java (REFACTORED)
    │   ├── ItemRepository.java (REFACTORED)
    │   ├── InteractionRepository.java (REFACTORED)
    │   ├── ReviewRepository.java (REFACTORED)
    │   ├── ListFavoriteRepository.java (REFACTORED)
    │   ├── GenreRepository.java (REFACTORED)
    │   ├── DirectorRepository.java (REFACTORED)
    │   └── ActorRepository.java (REFACTORED)
    └── mapper/
        ├── UserEntityMapper.java (NEW)
        ├── ItemEntityMapper.java (NEW)
        ├── InteractionEntityMapper.java (NEW)
        ├── ReviewEntityMapper.java (NEW)
        ├── ListFavoriteEntityMapper.java (NEW)
        ├── GenreEntityMapper.java (NEW)
        ├── DirectorEntityMapper.java (NEW)
        └── ActorEntityMapper.java (NEW)
```

## File Counts

**New Files:**
- 9 Controllers
- 8 Request DTOs
- 11 Response DTOs
- 8 API Mappers
- 1 GlobalExceptionHandler
- 8 Domain Models
- 8 Entity Mappers

**Total New Files:** ~52 files

**Refactored Files:**
- 9 Services
- 9 Repositories
- 1 ScoredItem
- 1 RecommendationEngine
- 1 HybridRecommendationEngine
- 1 RecommendationContext
- Multiple strategy classes

**Total Refactored Files:** ~25 files

## Testing Recommendations

### Unit Tests (Controllers)

- Mock services, test controller logic in isolation
- Verify correct HTTP status codes
- Verify DTO mapping
- Test validation failures

### Integration Tests (Full Stack)

- Test full HTTP flow: Request → Controller → Service → Repository → Database → Response
- Verify transaction boundaries
- Test exception handling with actual HTTP responses
- Verify JSON serialization/deserialization

### API Contract Tests

- Verify response structure matches DTO definitions
- Verify error responses follow RFC 7807 format
- Test all HTTP methods and status codes

## Dependencies

**Required Dependencies (already in pom.xml):**
- `spring-boot-starter-web` - REST controllers, Jackson
- `spring-boot-starter-validation` - Jakarta Bean Validation
- `spring-boot-starter-data-jpa` - Repository layer
- `lombok` - Reduce boilerplate

**No New Dependencies Required** - All functionality uses existing Spring Boot capabilities
