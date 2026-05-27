# Saveable Recommendations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist generated user recommendations and reuse them until TTL expiry so the recommendation motor does not run on every fetch.

**Architecture:** Add a normalized `saved_recommendations` JPA table in the backend persistence layer. `RecommendationService` remains the orchestration point: validate input, load the user, return fresh saved rows when present, otherwise run the existing domain motor, replace saved rows, and return the requested item slice. The recommendation motor stays domain-only and unchanged.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, Hibernate, Maven, JUnit 5, Mockito, AssertJ, H2 PostgreSQL mode for tests.

---

## File Structure

- Create: `src/main/java/org/tvl/tvlooker/domain/model/entity/SavedRecommendationEntity.java`
- Create: `src/main/java/org/tvl/tvlooker/persistence/repository/SavedRecommendationRepository.java`
- Modify: `src/main/java/org/tvl/tvlooker/service/RecommendationService.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/test/resources/application-test.properties`
- Modify: `src/test/java/org/tvl/tvlooker/service/RecommendationServiceTest.java`
- Modify: `src/test/java/org/tvl/tvlooker/service/RecommendationServiceIntegrationTest.java`

## Implementation Notes

- Use `Instant` for `createdAt` and `expiresAt`; Hibernate maps it cleanly for PostgreSQL and H2.
- Use repository nested-property methods with `UserEntity.id`: `findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(...)` and `deleteByUser_Id(...)`.
- Use `UserRepository.getReferenceById(userId)` and `ItemRepository.getReferenceById(itemId)` when saving rows so saved recommendations point at managed entities without reloading full rows.
- Use `ItemEntityMapper.toDomain(savedRecommendation.getItem())` to return cached items through the existing domain DTO.
- Keep response DTOs, controller, and frontend unchanged.
- Keep regeneration controlled only by missing fresh rows or TTL expiry. If fresh cache contains fewer rows than the requested limit, return the available rows.

---

### Task 1: Add Saved Recommendation Persistence Model

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/domain/model/entity/SavedRecommendationEntity.java`
- Create: `src/main/java/org/tvl/tvlooker/persistence/repository/SavedRecommendationRepository.java`

- [ ] **Step 1: Create the entity**

Add `src/main/java/org/tvl/tvlooker/domain/model/entity/SavedRecommendationEntity.java`:

```java
package org.tvl.tvlooker.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "saved_recommendations",
        indexes = {
                @Index(name = "idx_saved_recommendations_user_expires", columnList = "user_id_fk, expires_at"),
                @Index(name = "idx_saved_recommendations_user_rank", columnList = "user_id_fk, rank_position")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SavedRecommendationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id_pk", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_fk", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id_fk", nullable = false)
    private ItemEntity item;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "source_strategy")
    private String sourceStrategy;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
```

- [ ] **Step 2: Create the repository**

Add `src/main/java/org/tvl/tvlooker/persistence/repository/SavedRecommendationRepository.java`:

```java
package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.SavedRecommendationEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SavedRecommendationRepository extends JpaRepository<SavedRecommendationEntity, Long> {
    List<SavedRecommendationEntity> findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(
            UUID userId,
            Instant expiresAt
    );

    void deleteByUser_Id(UUID userId);
}
```

- [ ] **Step 3: Run the focused compile check**

Run: `mvn test -DskipTests`

Expected: Maven compiles main and test sources without entity or repository method errors.

---

### Task 2: Add Cache Configuration Properties

**Files:**
- Modify: `src/main/resources/application.properties`
- Modify: `src/test/resources/application-test.properties`

- [ ] **Step 1: Add production defaults**

In `src/main/resources/application.properties`, add this block after the existing recommendation pipeline properties:

```properties
# Recommendation Persistence Cache
recommendation.cache.ttl=PT24H
recommendation.cache.generated-size=100
```

- [ ] **Step 2: Add deterministic test defaults**

In `src/test/resources/application-test.properties`, add this block after the recommendation engine test configuration:

```properties
recommendation.cache.ttl=PT24H
recommendation.cache.generated-size=100
```

- [ ] **Step 3: Run property binding compile check**

Run: `mvn test -DskipTests`

Expected: Maven compiles successfully. No tests need to run yet.

---

### Task 3: Make RecommendationService Cache-Aware

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/RecommendationService.java`

- [ ] **Step 1: Update imports and constants**

Replace the imports and constants in `RecommendationService` so the class has these additions:

```java
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.SavedRecommendationEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.mapper.ItemEntityMapper;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.repository.SavedRecommendationRepository;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
```

Add these constants below the existing property constants:

```java
private static final String CACHE_TTL_PROPERTY = "recommendation.cache.ttl";
private static final String CACHE_GENERATED_SIZE_PROPERTY = "recommendation.cache.generated-size";
```

- [ ] **Step 2: Add dependencies and constructor parameters**

Add these fields:

```java
private final SavedRecommendationRepository savedRecommendationRepository;
private final UserRepository userRepository;
private final ItemRepository itemRepository;
private final Duration cacheTtl;
private final int cacheGeneratedSize;
```

Update the constructor signature and assignments:

```java
public RecommendationService(
        RecommendationEngine recommendationEngine,
        UserService userService,
        RecommendationDataGateway recommendationDataGateway,
        SavedRecommendationRepository savedRecommendationRepository,
        UserRepository userRepository,
        ItemRepository itemRepository,
        @Value("${" + CANDIDATE_PAGE_SIZE_PROPERTY + ":100}") int candidatePageSize,
        @Value("${" + REPRESENTATIVES_PER_PAGE_PROPERTY + ":10}") int representativesPerPage,
        @Value("${" + ASYNC_TIMEOUT_MILLIS_PROPERTY + ":10000}") long asyncTimeoutMillis,
        @Value("${" + CACHE_TTL_PROPERTY + ":PT24H}") Duration cacheTtl,
        @Value("${" + CACHE_GENERATED_SIZE_PROPERTY + ":100}") int cacheGeneratedSize) {
    this.recommendationEngine = recommendationEngine;
    this.userService = userService;
    this.recommendationDataGateway = recommendationDataGateway;
    this.savedRecommendationRepository = savedRecommendationRepository;
    this.userRepository = userRepository;
    this.itemRepository = itemRepository;
    this.candidatePageSize = candidatePageSize;
    this.representativesPerPage = representativesPerPage;
    this.asyncTimeoutMillis = asyncTimeoutMillis;
    this.cacheTtl = cacheTtl;
    this.cacheGeneratedSize = cacheGeneratedSize;
}
```

- [ ] **Step 3: Replace the public method flow**

Change `@Transactional(readOnly=true)` to `@Transactional` and replace `getUserRecommendations` with:

```java
@Transactional
public List<Item> getUserRecommendations(UUID userId, int limit) {
    validateInput(userId, limit);

    User user = userService.getById(userId);
    Instant now = Instant.now();
    List<SavedRecommendationEntity> savedRecommendations = savedRecommendationRepository
            .findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(userId, now);

    if (!savedRecommendations.isEmpty()) {
        return savedRecommendations.stream()
                .limit(limit)
                .map(SavedRecommendationEntity::getItem)
                .map(ItemEntityMapper::toDomain)
                .toList();
    }

    List<ScoredItem> scoredItems = recommendationEngine.recommend(user, buildContext(user));
    saveRecommendations(userId, scoredItems, limit, now);

    return scoredItems.stream()
            .map(ScoredItem::getItem)
            .limit(limit)
            .toList();
}
```

- [ ] **Step 4: Extract context construction into a private helper**

Add this method below `getUserRecommendations`:

```java
private RecommendationContext buildContext(User user) {
    return RecommendationContext.builder()
            .targetUser(user)
            .dataGateway(recommendationDataGateway)
            .candidatePageSize(candidatePageSize)
            .representativesPerPage(representativesPerPage)
            .asyncTimeoutMillis(asyncTimeoutMillis)
            .dataProviders(new HashMap<>())
            .dataCache(new HashMap<>())
            .build();
}
```

- [ ] **Step 5: Add save helper**

Add this method below `buildContext`:

```java
private void saveRecommendations(UUID userId, List<ScoredItem> scoredItems, int limit, Instant createdAt) {
    savedRecommendationRepository.deleteByUser_Id(userId);

    int saveSize = Math.max(limit, cacheGeneratedSize);
    Instant expiresAt = createdAt.plus(cacheTtl);
    UserEntity userReference = userRepository.getReferenceById(userId);
    List<SavedRecommendationEntity> recommendationsToSave = new ArrayList<>();

    for (int rank = 0; rank < scoredItems.size() && rank < saveSize; rank++) {
        ScoredItem scoredItem = scoredItems.get(rank);
        ItemEntity itemReference = itemRepository.getReferenceById(scoredItem.getItem().getId());
        recommendationsToSave.add(SavedRecommendationEntity.builder()
                .user(userReference)
                .item(itemReference)
                .score(scoredItem.getScore())
                .explanation(scoredItem.getExplanation())
                .sourceStrategy(scoredItem.getSourceStrategy())
                .rankPosition(rank)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build());
    }

    savedRecommendationRepository.saveAll(recommendationsToSave);
}
```

- [ ] **Step 6: Run service compile check**

Run: `mvn test -DskipTests`

Expected: Compilation passes. If Checkstyle later flags line length, wrap method chains without changing behavior.

---

### Task 4: Update RecommendationService Unit Tests

**Files:**
- Modify: `src/test/java/org/tvl/tvlooker/service/RecommendationServiceTest.java`

- [ ] **Step 1: Add mocks and constructor values**

Add imports:

```java
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.SavedRecommendationEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.repository.SavedRecommendationRepository;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
```

Add mocks:

```java
@Mock
private SavedRecommendationRepository savedRecommendationRepository;

@Mock
private UserRepository userRepository;

@Mock
private ItemRepository itemRepository;
```

Add fields:

```java
private Duration cacheTtl;
private int cacheGeneratedSize;
private UserEntity testUserEntity;
```

In `setUp`, initialize:

```java
cacheTtl = Duration.ofHours(24);
cacheGeneratedSize = 100;
testUserEntity = UserEntity.builder()
        .id(testUserId)
        .username("testuser")
        .password("password123")
        .email("test@test.com")
        .name("Test User")
        .build();
```

Update constructor call:

```java
recommendationService = new RecommendationService(
        recommendationEngine,
        userService,
        recommendationDataGateway,
        savedRecommendationRepository,
        userRepository,
        itemRepository,
        candidatePageSize,
        representativesPerPage,
        asyncTimeoutMillis,
        cacheTtl,
        cacheGeneratedSize);
```

- [ ] **Step 2: Update existing cache-miss tests to stub empty cache and entity references**

For tests that call the engine, add these stubs before invoking the service:

```java
when(savedRecommendationRepository.findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(eq(testUserId), any(Instant.class)))
        .thenReturn(List.of());
when(userRepository.getReferenceById(testUserId)).thenReturn(testUserEntity);
when(itemRepository.getReferenceById(2L)).thenReturn(itemEntity(2L, "Movie 2"));
when(itemRepository.getReferenceById(3L)).thenReturn(itemEntity(3L, "Movie 3"));
when(itemRepository.getReferenceById(1L)).thenReturn(itemEntity(1L, "Movie 1"));
```

Add this helper at the bottom of the test class:

```java
private ItemEntity itemEntity(Long id, String title) {
    return ItemEntity.builder()
            .id(id)
            .tmdbId(id)
            .tmdbType(TmdbType.MOVIE)
            .title(title)
            .overview("Overview " + id)
            .popularity(BigDecimal.valueOf(8.0))
            .build();
}
```

- [ ] **Step 3: Add cache miss save assertion test**

Add a captor field:

```java
@Captor
private ArgumentCaptor<List<SavedRecommendationEntity>> savedRecommendationsCaptor;
```

Add test:

```java
@Test
@DisplayName("getUserRecommendations - should save generated recommendations on cache miss")
void getUserRecommendations_shouldSaveGeneratedRecommendations_onCacheMiss() {
    when(userService.getById(testUserId)).thenReturn(testUser);
    when(savedRecommendationRepository.findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(eq(testUserId), any(Instant.class)))
            .thenReturn(List.of());
    when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class))).thenReturn(scoredRecommendations);
    when(userRepository.getReferenceById(testUserId)).thenReturn(testUserEntity);
    when(itemRepository.getReferenceById(2L)).thenReturn(itemEntity(2L, "Movie 2"));
    when(itemRepository.getReferenceById(3L)).thenReturn(itemEntity(3L, "Movie 3"));
    when(itemRepository.getReferenceById(1L)).thenReturn(itemEntity(1L, "Movie 1"));

    recommendationService.getUserRecommendations(testUserId, 2);

    verify(savedRecommendationRepository).deleteByUser_Id(testUserId);
    verify(savedRecommendationRepository).saveAll(savedRecommendationsCaptor.capture());
    List<SavedRecommendationEntity> savedRows = savedRecommendationsCaptor.getValue();
    assertThat(savedRows).hasSize(3);
    assertThat(savedRows).extracting(SavedRecommendationEntity::getRankPosition).containsExactly(0, 1, 2);
    assertThat(savedRows).extracting(SavedRecommendationEntity::getScore).containsExactly(0.95, 0.85, 0.75);
    assertThat(savedRows).allSatisfy(row -> assertThat(row.getExpiresAt()).isAfter(row.getCreatedAt()));
}
```

- [ ] **Step 4: Add cache hit test**

Add test:

```java
@Test
@DisplayName("getUserRecommendations - should return saved recommendations on cache hit")
void getUserRecommendations_shouldReturnSavedRecommendations_onCacheHit() {
    SavedRecommendationEntity savedRow = SavedRecommendationEntity.builder()
            .user(testUserEntity)
            .item(itemEntity(3L, "Movie 3"))
            .score(0.85)
            .explanation("Popular choice")
            .sourceStrategy("popularity")
            .rankPosition(0)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plus(cacheTtl))
            .build();
    when(userService.getById(testUserId)).thenReturn(testUser);
    when(savedRecommendationRepository.findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(eq(testUserId), any(Instant.class)))
            .thenReturn(List.of(savedRow));

    List<Item> result = recommendationService.getUserRecommendations(testUserId, 5);

    assertThat(result).extracting(Item::getTitle).containsExactly("Movie 3");
    verify(recommendationEngine, never()).recommend(any(), any());
    verify(savedRecommendationRepository, never()).deleteByUser_Id(any());
    verify(savedRecommendationRepository, never()).saveAll(any());
}
```

- [ ] **Step 5: Add cached limit test**

Add test:

```java
@Test
@DisplayName("getUserRecommendations - should limit saved recommendations on cache hit")
void getUserRecommendations_shouldLimitSavedRecommendations_onCacheHit() {
    when(userService.getById(testUserId)).thenReturn(testUser);
    when(savedRecommendationRepository.findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(eq(testUserId), any(Instant.class)))
            .thenReturn(List.of(
                    SavedRecommendationEntity.builder().item(itemEntity(2L, "Movie 2")).rankPosition(0).build(),
                    SavedRecommendationEntity.builder().item(itemEntity(3L, "Movie 3")).rankPosition(1).build()
            ));

    List<Item> result = recommendationService.getUserRecommendations(testUserId, 1);

    assertThat(result).extracting(Item::getTitle).containsExactly("Movie 2");
    verify(recommendationEngine, never()).recommend(any(), any());
}
```

- [ ] **Step 6: Verify invalid input still short-circuits**

Update invalid limit/null user tests to keep these assertions:

```java
verify(userService, never()).getById(any());
verifyNoInteractions(savedRecommendationRepository, recommendationEngine);
```

- [ ] **Step 7: Run unit tests**

Run: `mvn test -Dtest=RecommendationServiceTest`

Expected: all `RecommendationServiceTest` tests pass.

---

### Task 5: Add Integration Coverage for Persisted Recommendations

**Files:**
- Modify: `src/test/java/org/tvl/tvlooker/service/RecommendationServiceIntegrationTest.java`

- [ ] **Step 1: Autowire saved recommendation repository**

Add imports:

```java
import org.tvl.tvlooker.domain.model.entity.SavedRecommendationEntity;
import org.tvl.tvlooker.persistence.repository.SavedRecommendationRepository;

import java.time.Instant;
```

Add field:

```java
@Autowired
private SavedRecommendationRepository savedRecommendationRepository;
```

Update `setUp` and `tearDown` so saved rows are deleted before items/users:

```java
savedRecommendationRepository.deleteAll();
interactionRepository.deleteAll();
itemRepository.deleteAll();
userRepository.deleteAll();
```

- [ ] **Step 2: Add first request persists rows test**

Add test:

```java
@Test
@DisplayName("Should persist saved recommendation rows on first request")
void testGetRecommendations_FirstRequest_PersistsSavedRows() {
    List<Item> recommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), 3);

    List<SavedRecommendationEntity> savedRows = savedRecommendationRepository
            .findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(testUser1Entity.getId(), Instant.now());
    assertThat(savedRows).isNotEmpty();
    assertThat(savedRows).hasSizeGreaterThanOrEqualTo(recommendations.size());
    assertThat(savedRows).extracting(SavedRecommendationEntity::getRankPosition)
            .containsExactlyElementsOf(savedRows.stream().map(SavedRecommendationEntity::getRankPosition).sorted().toList());
}
```

- [ ] **Step 3: Add second request reuses persisted rows test**

Add test:

```java
@Test
@DisplayName("Should reuse saved recommendation rows before TTL expiry")
void testGetRecommendations_SecondRequest_ReusesSavedRows() {
    List<Item> firstRecommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), 3);
    List<Long> savedIds = savedRecommendationRepository
            .findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(testUser1Entity.getId(), Instant.now())
            .stream()
            .map(SavedRecommendationEntity::getId)
            .toList();

    List<Item> secondRecommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), 3);
    List<Long> savedIdsAfterSecondRequest = savedRecommendationRepository
            .findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(testUser1Entity.getId(), Instant.now())
            .stream()
            .map(SavedRecommendationEntity::getId)
            .toList();

    assertThat(secondRecommendations).extracting(Item::getId)
            .containsExactlyElementsOf(firstRecommendations.stream().map(Item::getId).toList());
    assertThat(savedIdsAfterSecondRequest).containsExactlyElementsOf(savedIds);
}
```

- [ ] **Step 4: Add expired rows replaced test**

Add test:

```java
@Test
@DisplayName("Should replace expired saved recommendation rows")
void testGetRecommendations_ExpiredRows_ReplacesSavedRows() {
    recommendationService.getUserRecommendations(testUser1Entity.getId(), 3);
    List<SavedRecommendationEntity> savedRows = savedRecommendationRepository
            .findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(testUser1Entity.getId(), Instant.now());
    assertThat(savedRows).isNotEmpty();
    savedRows.forEach(row -> row.setExpiresAt(Instant.now().minusSeconds(1)));
    savedRecommendationRepository.saveAll(savedRows);
    List<Long> expiredIds = savedRows.stream().map(SavedRecommendationEntity::getId).toList();

    recommendationService.getUserRecommendations(testUser1Entity.getId(), 3);

    List<SavedRecommendationEntity> freshRows = savedRecommendationRepository
            .findByUser_IdAndExpiresAtAfterOrderByRankPositionAsc(testUser1Entity.getId(), Instant.now());
    assertThat(freshRows).isNotEmpty();
    assertThat(freshRows).extracting(SavedRecommendationEntity::getId).doesNotContainAnyElementsOf(expiredIds);
}
```

- [ ] **Step 5: Add cached rank order test**

Add test:

```java
@Test
@DisplayName("Should return cached items ordered by rank position")
void testGetRecommendations_CacheHit_ReturnsRankOrder() {
    savedRecommendationRepository.deleteByUser_Id(testUser1Entity.getId());
    Instant now = Instant.now();
    savedRecommendationRepository.saveAll(List.of(
            SavedRecommendationEntity.builder()
                    .user(testUser1Entity)
                    .item(popularItem2Entity)
                    .score(0.6)
                    .rankPosition(1)
                    .createdAt(now)
                    .expiresAt(now.plusSeconds(3600))
                    .build(),
            SavedRecommendationEntity.builder()
                    .user(testUser1Entity)
                    .item(popularItem3Entity)
                    .score(0.9)
                    .rankPosition(0)
                    .createdAt(now)
                    .expiresAt(now.plusSeconds(3600))
                    .build()
    ));

    List<Item> recommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), 2);

    assertThat(recommendations).extracting(Item::getId)
            .containsExactly(popularItem3Entity.getId(), popularItem2Entity.getId());
}
```

- [ ] **Step 6: Run integration tests**

Run: `mvn test -Dtest=RecommendationServiceIntegrationTest`

Expected: all recommendation service integration tests pass using H2.

---

### Task 6: Final Verification and Graph Update

**Files:**
- Modify only if verification exposes a real issue in files from earlier tasks.

- [ ] **Step 1: Run focused backend tests**

Run: `mvn test -Dtest=RecommendationServiceTest,RecommendationServiceIntegrationTest`

Expected: both focused test classes pass.

- [ ] **Step 2: Run backend verify**

Run: `mvn clean verify`

Expected: checkstyle, PMD, unit tests, and integration tests pass. If unrelated pre-existing tests fail, capture the failing class/method and verify the recommendation-focused tests still pass.

- [ ] **Step 3: Update graphify knowledge graph**

Run: `graphify update .`

Expected: graph update completes. Dirty `graphify-out` files are expected after this command.

- [ ] **Step 4: Review git diff**

Run: `git diff -- src/main/java/org/tvl/tvlooker/domain/model/entity/SavedRecommendationEntity.java src/main/java/org/tvl/tvlooker/persistence/repository/SavedRecommendationRepository.java src/main/java/org/tvl/tvlooker/service/RecommendationService.java src/main/resources/application.properties src/test/resources/application-test.properties src/test/java/org/tvl/tvlooker/service/RecommendationServiceTest.java src/test/java/org/tvl/tvlooker/service/RecommendationServiceIntegrationTest.java docs/superpowers/plans/2026-05-27-saveable-recommendations.md`

Expected: diff contains only saveable recommendation persistence, tests, properties, and this plan.

---

## Self-Review

- Spec coverage: the plan includes the normalized table, TTL reuse, service regeneration flow, unchanged API, unit tests, and integration tests.
- Scope check: frontend changes, manual refresh, activity invalidation, batch metadata, and cleanup jobs remain out of scope.
- Type consistency: `recommendation.cache.ttl`, `recommendation.cache.generated-size`, `SavedRecommendationEntity`, and `SavedRecommendationRepository` names are consistent across tasks.
- Placeholder scan: no placeholder implementation steps remain; every code-changing step names the target file and provides concrete code.
