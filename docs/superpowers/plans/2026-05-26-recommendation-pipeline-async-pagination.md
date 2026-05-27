# Recommendation Pipeline Async Pagination Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor recommendations so requests page candidate items from the database, score pages asynchronously, and fuse page representatives with Borda count instead of loading the full catalog into request memory.

**Architecture:** Keep the recommendation engine operating on domain DTOs. Add a small domain-facing pagination gateway backed by existing Spring services, store that gateway in `RecommendationContext`, and let `HybridRecommendationEngine` process `CandidatePage`s through `CompletableFuture`s using a dedicated executor. Global model providers keep building cached SVD/TF-IDF/popularity models from paged domain data, while strategies only score the current candidate page.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data `Pageable`, `CompletableFuture`, JUnit 5, Mockito, AssertJ, Maven.

---

## File Structure

- Create `src/main/java/org/tvl/tvlooker/domain/motor/utils/DataPage.java`: generic domain page wrapper that avoids importing Spring Data into the domain motor.
- Create `src/main/java/org/tvl/tvlooker/domain/motor/utils/RecommendationDataGateway.java`: domain-facing pagination interface for candidate items, all items, interactions, and reviews.
- Create `src/main/java/org/tvl/tvlooker/service/RecommendationDomainDataGateway.java`: Spring service adapter that uses `ItemService`, `InteractionService`, and `ReviewService` paginated methods and returns `DataPage<T>`.
- Modify `src/main/java/org/tvl/tvlooker/domain/motor/utils/RecommendationContext.java`: add `User targetUser`, `RecommendationDataGateway dataGateway`, `int candidatePageSize`, `int representativesPerPage`, and `long asyncTimeoutMillis`; keep provider cache methods.
- Modify `src/main/java/org/tvl/tvlooker/service/RecommendationService.java`: stop calling `getAll()` and build a lightweight context with the gateway and tuning values.
- Modify `src/main/java/org/tvl/tvlooker/config/AsyncConfiguration.java`: add a dedicated `recommendationTaskExecutor` bean.
- Modify `src/main/java/org/tvl/tvlooker/config/RecommendationConfig.java`: inject `@Qualifier("recommendationTaskExecutor") Executor` into `HybridRecommendationEngine` and expose recommendation tuning properties.
- Modify `src/main/java/org/tvl/tvlooker/domain/motor/HybridRecommendationEngine.java`: process candidate pages asynchronously, aggregate each page with `RankingBasedAggregation`, then globally aggregate page representatives with Borda count.
- Modify providers under `src/main/java/org/tvl/tvlooker/domain/motor/utils/provider/`: replace direct `context.getItems()`, `context.getInteractions()`, and `context.getReviews()` reads with paged reads from `context.getDataGateway()`.
- Modify tests under `src/test/java/org/tvl/tvlooker/service/` and `src/test/java/org/tvl/tvlooker/domain/motor/`: replace bulk-loading expectations with gateway/context assertions and async page behavior.
- Modify `src/test/java/org/tvl/tvlooker/testutil/TestDataFactory.java`: add paged gateway fixtures for engine/provider tests.
- Modify `src/main/resources/application.properties`: add recommendation page size, representatives-per-page, async timeout, and executor pool settings.

## Task 1: Add Domain Paging Gateway Types

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/domain/motor/utils/DataPage.java`
- Create: `src/main/java/org/tvl/tvlooker/domain/motor/utils/RecommendationDataGateway.java`
- Test: `src/test/java/org/tvl/tvlooker/domain/motor/utils/RecommendationDataGatewayTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/org/tvl/tvlooker/domain/motor/utils/RecommendationDataGatewayTest.java`:

```java
package org.tvl.tvlooker.domain.motor.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationDataGateway domain paging tests")
class RecommendationDataGatewayTest {

    @Test
    @DisplayName("DataPage should expose immutable page content and next-page flag")
    void dataPageShouldExposeContentAndNextPageFlag() {
        List<Item> items = TestDataFactory.createItems(2);

        DataPage<Item> page = new DataPage<>(items, true);

        assertThat(page.content()).containsExactlyElementsOf(items);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("DataPage empty factory should return empty page without next page")
    void emptyFactoryShouldReturnEmptyPageWithoutNextPage() {
        DataPage<Item> page = DataPage.empty();

        assertThat(page.content()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.isEmpty()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RecommendationDataGatewayTest`

Expected: FAIL because `DataPage` and `RecommendationDataGateway` do not exist.

- [ ] **Step 3: Add `DataPage`**

Create `src/main/java/org/tvl/tvlooker/domain/motor/utils/DataPage.java`:

```java
package org.tvl.tvlooker.domain.motor.utils;

import java.util.List;

public record DataPage<T>(List<T> content, boolean hasNext) {

    public DataPage {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public static <T> DataPage<T> empty() {
        return new DataPage<>(List.of(), false);
    }
}
```

- [ ] **Step 4: Add `RecommendationDataGateway`**

Create `src/main/java/org/tvl/tvlooker/domain/motor/utils/RecommendationDataGateway.java`:

```java
package org.tvl.tvlooker.domain.motor.utils;

import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.Review;

public interface RecommendationDataGateway {

    DataPage<Item> getCandidateItems(int pageNumber, int pageSize);

    DataPage<Item> getAllItems(int pageNumber, int pageSize);

    DataPage<Interaction> getAllInteractions(int pageNumber, int pageSize);

    DataPage<Review> getAllReviews(int pageNumber, int pageSize);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=RecommendationDataGatewayTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/domain/motor/utils/DataPage.java src/main/java/org/tvl/tvlooker/domain/motor/utils/RecommendationDataGateway.java src/test/java/org/tvl/tvlooker/domain/motor/utils/RecommendationDataGatewayTest.java
git commit -m "feat: add recommendation data paging gateway"
```

## Task 2: Implement Spring Service Adapter For Paged Domain Data

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/service/RecommendationDomainDataGateway.java`
- Test: `src/test/java/org/tvl/tvlooker/service/RecommendationDomainDataGatewayTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/org/tvl/tvlooker/service/RecommendationDomainDataGatewayTest.java`:

```java
package org.tvl.tvlooker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.Review;
import org.tvl.tvlooker.domain.motor.utils.DataPage;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationDomainDataGateway tests")
class RecommendationDomainDataGatewayTest {

    @Mock
    private ItemService itemService;

    @Mock
    private InteractionService interactionService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private RecommendationDomainDataGateway gateway;

    @Test
    @DisplayName("getCandidateItems should delegate to ItemService pagination")
    void getCandidateItemsShouldDelegateToItemServicePagination() {
        List<Item> items = TestDataFactory.createItems(2);
        Pageable pageable = PageRequest.of(1, 2);
        when(itemService.getAll(pageable)).thenReturn(new PageImpl<>(items, pageable, 5));

        DataPage<Item> result = gateway.getCandidateItems(1, 2);

        assertThat(result.content()).containsExactlyElementsOf(items);
        assertThat(result.hasNext()).isTrue();
        verify(itemService).getAll(eq(pageable));
    }

    @Test
    @DisplayName("getAllInteractions should delegate to InteractionService pagination")
    void getAllInteractionsShouldDelegateToInteractionServicePagination() {
        Interaction interaction = TestDataFactory.createWatchInteraction(1L, java.util.UUID.randomUUID(), 1L);
        Pageable pageable = PageRequest.of(0, 25);
        when(interactionService.getAll(pageable)).thenReturn(new PageImpl<>(List.of(interaction), pageable, 1));

        DataPage<Interaction> result = gateway.getAllInteractions(0, 25);

        assertThat(result.content()).containsExactly(interaction);
        assertThat(result.hasNext()).isFalse();
        verify(interactionService).getAll(eq(pageable));
    }

    @Test
    @DisplayName("getAllReviews should delegate to ReviewService pagination")
    void getAllReviewsShouldDelegateToReviewServicePagination() {
        Review review = Review.builder().id(1L).itemId(1L).score(5).build();
        Pageable pageable = PageRequest.of(0, 50);
        when(reviewService.getAll(pageable)).thenReturn(new PageImpl<>(List.of(review), pageable, 1));

        DataPage<Review> result = gateway.getAllReviews(0, 50);

        assertThat(result.content()).containsExactly(review);
        assertThat(result.hasNext()).isFalse();
        verify(reviewService).getAll(eq(pageable));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RecommendationDomainDataGatewayTest`

Expected: FAIL because `RecommendationDomainDataGateway` does not exist.

- [ ] **Step 3: Add Spring service adapter**

Create `src/main/java/org/tvl/tvlooker/service/RecommendationDomainDataGateway.java`:

```java
package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.Review;
import org.tvl.tvlooker.domain.motor.utils.DataPage;
import org.tvl.tvlooker.domain.motor.utils.RecommendationDataGateway;

@Service
@RequiredArgsConstructor
public class RecommendationDomainDataGateway implements RecommendationDataGateway {

    private final ItemService itemService;
    private final InteractionService interactionService;
    private final ReviewService reviewService;

    @Override
    public DataPage<Item> getCandidateItems(int pageNumber, int pageSize) {
        return toDataPage(itemService.getAll(PageRequest.of(pageNumber, pageSize)));
    }

    @Override
    public DataPage<Item> getAllItems(int pageNumber, int pageSize) {
        return getCandidateItems(pageNumber, pageSize);
    }

    @Override
    public DataPage<Interaction> getAllInteractions(int pageNumber, int pageSize) {
        return toDataPage(interactionService.getAll(PageRequest.of(pageNumber, pageSize)));
    }

    @Override
    public DataPage<Review> getAllReviews(int pageNumber, int pageSize) {
        return toDataPage(reviewService.getAll(PageRequest.of(pageNumber, pageSize)));
    }

    private static <T> DataPage<T> toDataPage(Page<T> page) {
        return new DataPage<>(page.getContent(), page.hasNext());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=RecommendationDomainDataGatewayTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/service/RecommendationDomainDataGateway.java src/test/java/org/tvl/tvlooker/service/RecommendationDomainDataGatewayTest.java
git commit -m "feat: adapt recommendation data gateway to services"
```

## Task 3: Make RecommendationContext Lightweight And Page-Aware

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/domain/motor/utils/RecommendationContext.java`
- Modify: `src/test/java/org/tvl/tvlooker/testutil/TestDataFactory.java`
- Test: `src/test/java/org/tvl/tvlooker/domain/motor/utils/RecommendationContextTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/org/tvl/tvlooker/domain/motor/utils/RecommendationContextTest.java`:

```java
package org.tvl.tvlooker.domain.motor.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationContext tests")
class RecommendationContextTest {

    @Test
    @DisplayName("checkDataNotNull should accept target user and data gateway without bulk lists")
    void checkDataNotNullShouldAcceptGatewayContext() {
        User user = TestDataFactory.createUser("target");
        RecommendationDataGateway gateway = TestDataFactory.createGateway(TestDataFactory.createItems(3));

        RecommendationContext context = RecommendationContext.builder()
                .targetUser(user)
                .dataGateway(gateway)
                .candidatePageSize(2)
                .representativesPerPage(1)
                .asyncTimeoutMillis(1_000L)
                .build();

        assertThat(context.checkDataNotNull()).isTrue();
        assertThat(context.getTargetUser()).isEqualTo(user);
        assertThat(context.getCandidatePageSize()).isEqualTo(2);
        assertThat(context.getRepresentativesPerPage()).isEqualTo(1);
        assertThat(context.getAsyncTimeoutMillis()).isEqualTo(1_000L);
    }

    @Test
    @DisplayName("test gateway fixture should return item pages")
    void testGatewayFixtureShouldReturnItemPages() {
        List<Item> items = TestDataFactory.createItems(3);
        RecommendationDataGateway gateway = TestDataFactory.createGateway(items);

        DataPage<Item> firstPage = gateway.getCandidateItems(0, 2);
        DataPage<Item> secondPage = gateway.getCandidateItems(1, 2);

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RecommendationContextTest`

Expected: FAIL because `RecommendationContext` does not have the new fields and `TestDataFactory.createGateway` does not exist.

- [ ] **Step 3: Update `RecommendationContext` fields and validation**

In `src/main/java/org/tvl/tvlooker/domain/motor/utils/RecommendationContext.java`, add fields inside the class:

```java
private User targetUser;

private RecommendationDataGateway dataGateway;

@Builder.Default
private int candidatePageSize = 100;

@Builder.Default
private int representativesPerPage = 10;

@Builder.Default
private long asyncTimeoutMillis = 10_000L;
```

Replace `checkDataNotNull()` with:

```java
public boolean checkDataNotNull() {
    return dataGateway != null || items != null;
}
```

Keep the existing bulk list fields during this task so older tests still compile. They are removed after provider and engine tests move to the paged gateway.

- [ ] **Step 4: Add paged gateway fixture to `TestDataFactory`**

Add imports to `src/test/java/org/tvl/tvlooker/testutil/TestDataFactory.java`:

```java
import org.tvl.tvlooker.domain.model.dto.Review;
import org.tvl.tvlooker.domain.motor.utils.DataPage;
import org.tvl.tvlooker.domain.motor.utils.RecommendationDataGateway;
```

Add this method before the closing brace:

```java
public static RecommendationDataGateway createGateway(List<Item> items) {
    return createGateway(items, new ArrayList<>(), new ArrayList<>());
}

public static RecommendationDataGateway createGateway(
        List<Item> items,
        List<Interaction> interactions,
        List<Review> reviews) {
    return new RecommendationDataGateway() {
        @Override
        public DataPage<Item> getCandidateItems(int pageNumber, int pageSize) {
            return page(items, pageNumber, pageSize);
        }

        @Override
        public DataPage<Item> getAllItems(int pageNumber, int pageSize) {
            return page(items, pageNumber, pageSize);
        }

        @Override
        public DataPage<Interaction> getAllInteractions(int pageNumber, int pageSize) {
            return page(interactions, pageNumber, pageSize);
        }

        @Override
        public DataPage<Review> getAllReviews(int pageNumber, int pageSize) {
            return page(reviews, pageNumber, pageSize);
        }
    };
}

private static <T> DataPage<T> page(List<T> values, int pageNumber, int pageSize) {
    int start = pageNumber * pageSize;
    if (start >= values.size()) {
        return DataPage.empty();
    }
    int end = Math.min(start + pageSize, values.size());
    return new DataPage<>(values.subList(start, end), end < values.size());
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=RecommendationContextTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/domain/motor/utils/RecommendationContext.java src/test/java/org/tvl/tvlooker/testutil/TestDataFactory.java src/test/java/org/tvl/tvlooker/domain/motor/utils/RecommendationContextTest.java
git commit -m "feat: make recommendation context page aware"
```

## Task 4: Stop Bulk Loading In RecommendationService

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/RecommendationService.java`
- Modify: `src/test/java/org/tvl/tvlooker/service/RecommendationServiceTest.java`
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: Replace service test expectations**

In `src/test/java/org/tvl/tvlooker/service/RecommendationServiceTest.java`, replace the `ReviewService reviewService` mock with:

```java
@Mock
private RecommendationDataGateway recommendationDataGateway;
```

Remove stubbing and verification of `userService.getAll()`, `itemService.getAll()`, `interactionService.getAll()`, and `reviewService.getAll()` from recommendation service tests. Replace the current context-capture test with:

```java
@Test
@DisplayName("getUserRecommendations - should build lightweight paged context")
void getUserRecommendations_shouldBuildLightweightPagedContext() {
    int limit = 5;
    when(userService.getById(testUserId)).thenReturn(testUser);
    when(recommendationEngine.recommend(eq(testUser), contextCaptor.capture()))
            .thenReturn(scoredRecommendations);

    recommendationService.getUserRecommendations(testUserId, limit);

    RecommendationContext capturedContext = contextCaptor.getValue();
    assertThat(capturedContext).isNotNull();
    assertThat(capturedContext.getTargetUser()).isEqualTo(testUser);
    assertThat(capturedContext.getDataGateway()).isSameAs(recommendationDataGateway);
    assertThat(capturedContext.getCandidatePageSize()).isEqualTo(100);
    assertThat(capturedContext.getRepresentativesPerPage()).isEqualTo(10);
    assertThat(capturedContext.getAsyncTimeoutMillis()).isEqualTo(10_000L);
}
```

Add this verification to a successful recommendation test:

```java
verify(userService, times(1)).getById(testUserId);
verifyNoInteractions(itemService, interactionService);
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RecommendationServiceTest`

Expected: FAIL because `RecommendationService` still requires `ItemService`, `InteractionService`, and `ReviewService` and still bulk-loads data.

- [ ] **Step 3: Update `RecommendationService` dependencies and properties**

Replace the fields in `src/main/java/org/tvl/tvlooker/service/RecommendationService.java` with:

```java
private final RecommendationEngine recommendationEngine;
private final UserService userService;
private final RecommendationDataGateway recommendationDataGateway;

@org.springframework.beans.factory.annotation.Value("${recommendation.pipeline.candidate-page-size:100}")
private int candidatePageSize;

@org.springframework.beans.factory.annotation.Value("${recommendation.pipeline.representatives-per-page:10}")
private int representativesPerPage;

@org.springframework.beans.factory.annotation.Value("${recommendation.pipeline.async-timeout-ms:10000}")
private long asyncTimeoutMillis;
```

Replace the context construction in `getUserRecommendations` with:

```java
RecommendationContext context = RecommendationContext.builder()
        .targetUser(user)
        .dataGateway(recommendationDataGateway)
        .candidatePageSize(candidatePageSize)
        .representativesPerPage(representativesPerPage)
        .asyncTimeoutMillis(asyncTimeoutMillis)
        .build();
```

- [ ] **Step 4: Add properties**

Append to the recommendation configuration section of `src/main/resources/application.properties`:

```properties
recommendation.pipeline.candidate-page-size=100
recommendation.pipeline.representatives-per-page=10
recommendation.pipeline.async-timeout-ms=10000
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=RecommendationServiceTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/service/RecommendationService.java src/test/java/org/tvl/tvlooker/service/RecommendationServiceTest.java src/main/resources/application.properties
git commit -m "feat: build lightweight recommendation context"
```

## Task 5: Add Dedicated Recommendation Executor Wiring

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/config/AsyncConfiguration.java`
- Modify: `src/main/java/org/tvl/tvlooker/config/RecommendationConfig.java`
- Modify: `src/main/resources/application.properties`
- Test: `src/test/java/org/tvl/tvlooker/config/RecommendationConfigTest.java`

- [ ] **Step 1: Write the failing context test**

Create `src/test/java/org/tvl/tvlooker/config/RecommendationConfigTest.java`:

```java
package org.tvl.tvlooker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.tvl.tvlooker.domain.motor.RecommendationEngine;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "recommendation.strategies.content.enabled=false",
        "recommendation.strategies.matrix-factorization.enabled=false"
})
@DisplayName("Recommendation configuration tests")
class RecommendationConfigTest {

    @Autowired
    private RecommendationEngine recommendationEngine;

    @Autowired
    @Qualifier("recommendationTaskExecutor")
    private Executor recommendationTaskExecutor;

    @Test
    @DisplayName("should wire recommendation engine and dedicated executor")
    void shouldWireRecommendationEngineAndDedicatedExecutor() {
        assertThat(recommendationEngine).isNotNull();
        assertThat(recommendationTaskExecutor).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RecommendationConfigTest`

Expected: FAIL because `recommendationTaskExecutor` does not exist.

- [ ] **Step 3: Add executor bean**

Add this method to `src/main/java/org/tvl/tvlooker/config/AsyncConfiguration.java`:

```java
@Bean(name = "recommendationTaskExecutor")
public Executor recommendationTaskExecutor(
        @org.springframework.beans.factory.annotation.Value("${recommendation.executor.core-pool-size:4}") int corePoolSize,
        @org.springframework.beans.factory.annotation.Value("${recommendation.executor.max-pool-size:8}") int maxPoolSize,
        @org.springframework.beans.factory.annotation.Value("${recommendation.executor.queue-capacity:100}") int queueCapacity) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("recommendation-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

- [ ] **Step 4: Inject executor into engine config**

Change `src/main/java/org/tvl/tvlooker/config/RecommendationConfig.java` bean method to:

```java
@Bean
public RecommendationEngine recommendationEngine(
        List<RecommendationStrategy> strategies,
        AggregationStrategy aggregation,
        List<DataProvider<?>> dataProviders,
        @org.springframework.beans.factory.annotation.Qualifier("recommendationTaskExecutor") java.util.concurrent.Executor executor) {
    return new HybridRecommendationEngine(strategies, aggregation, dataProviders, executor);
}
```

- [ ] **Step 5: Add executor properties**

Append to `src/main/resources/application.properties`:

```properties
recommendation.executor.core-pool-size=4
recommendation.executor.max-pool-size=8
recommendation.executor.queue-capacity=100
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn test -Dtest=RecommendationConfigTest`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/config/AsyncConfiguration.java src/main/java/org/tvl/tvlooker/config/RecommendationConfig.java src/main/resources/application.properties src/test/java/org/tvl/tvlooker/config/RecommendationConfigTest.java
git commit -m "feat: add recommendation executor"
```

## Task 6: Implement Async Page Tournament In HybridRecommendationEngine

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/domain/motor/HybridRecommendationEngine.java`
- Modify: `src/test/java/org/tvl/tvlooker/domain/motor/HybridRecommendationEngineTest.java`

- [ ] **Step 1: Add failing async page tests**

Add imports to `HybridRecommendationEngineTest.java`:

```java
import java.util.concurrent.Executor;
```

Add helper:

```java
private Executor directExecutor() {
    return Runnable::run;
}
```

Update constructor calls to pass `directExecutor()`:

```java
engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders, directExecutor());
```

Add this test:

```java
@Test
@DisplayName("Should process candidate pages and keep representatives from each page")
void shouldProcessCandidatePagesAndKeepRepresentatives() {
    RecommendationStrategy strategy = mock(RecommendationStrategy.class);
    when(strategy.getStrategyName()).thenReturn("page-aware");
    when(strategy.recommend(any(), any(), any())).thenAnswer(invocation -> {
        List<Item> candidates = invocation.getArgument(1);
        return TestDataFactory.createScoredItems(candidates, "page-aware");
    });

    strategies.add(strategy);
    aggregationStrategy = new org.tvl.tvlooker.domain.strategy.aggregation.RankingBasedAggregation();
    engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders, directExecutor());

    List<Item> items = TestDataFactory.createItems(5);
    context = RecommendationContext.builder()
            .targetUser(testUser)
            .dataGateway(TestDataFactory.createGateway(items))
            .candidatePageSize(2)
            .representativesPerPage(1)
            .asyncTimeoutMillis(1_000L)
            .build();

    List<ScoredItem> recommendations = engine.recommend(testUser, context);

    assertEquals(3, recommendations.size());
    verify(strategy, times(3)).recommend(eq(testUser), any(), eq(context));
}
```

Add this failure-resilience test:

```java
@Test
@DisplayName("Should continue when one candidate page fails")
void shouldContinueWhenOneCandidatePageFails() {
    RecommendationStrategy strategy = mock(RecommendationStrategy.class);
    when(strategy.getStrategyName()).thenReturn("sometimes-fails");
    when(strategy.recommend(any(), any(), any()))
            .thenThrow(new RuntimeException("first page failed"))
            .thenAnswer(invocation -> TestDataFactory.createScoredItems(invocation.getArgument(1), "sometimes-fails"));

    strategies.add(strategy);
    aggregationStrategy = new org.tvl.tvlooker.domain.strategy.aggregation.RankingBasedAggregation();
    engine = new HybridRecommendationEngine(strategies, aggregationStrategy, dataProviders, directExecutor());

    context = RecommendationContext.builder()
            .targetUser(testUser)
            .dataGateway(TestDataFactory.createGateway(TestDataFactory.createItems(4)))
            .candidatePageSize(2)
            .representativesPerPage(1)
            .asyncTimeoutMillis(1_000L)
            .build();

    List<ScoredItem> recommendations = engine.recommend(testUser, context);

    assertEquals(1, recommendations.size());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=HybridRecommendationEngineTest`

Expected: FAIL because `HybridRecommendationEngine` has no executor constructor and does not process pages.

- [ ] **Step 3: Update engine constructor and field**

In `HybridRecommendationEngine.java`, add imports:

```java
import org.tvl.tvlooker.domain.motor.utils.DataPage;
import org.tvl.tvlooker.domain.strategy.aggregation.RankingBasedAggregation;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
```

Remove `@AllArgsConstructor`, add field:

```java
private final Executor executor;
private final AggregationStrategy pageAggregationStrategy = new RankingBasedAggregation();
```

Add constructor:

```java
public HybridRecommendationEngine(
        List<RecommendationStrategy> strategies,
        AggregationStrategy aggregationStrategy,
        List<DataProvider<?>> dataProviders,
        Executor executor) {
    this.STRATEGIES = strategies;
    this.AGGREGATION_STRATEGY = aggregationStrategy;
    this.DATA_PROVIDERS = dataProviders;
    this.executor = executor;
}
```

- [ ] **Step 4: Replace `recommend` with paged flow**

Use this implementation skeleton:

```java
@Override
public List<ScoredItem> recommend(User user, RecommendationContext context) {
    if (context == null) {
        throw new InvalidEngineConfigurationException("RecommendationContext cannot be null");
    }
    DATA_PROVIDERS.forEach(context::registerDataProvider);
    validateInputs(user, context);

    if (context.getDataGateway() == null) {
        List<Item> candidateItems = filterCandidates(user, context);
        Map<String, List<ScoredItem>> strategyResults = executeStrategies(user, candidateItems, context);
        return postProcess(AGGREGATION_STRATEGY.aggregate(strategyResults, context));
    }

    List<CompletableFuture<PageResult>> futures = new ArrayList<>();
    int pageNumber = 0;
    boolean hasNext;
    do {
        DataPage<Item> page = context.getDataGateway().getCandidateItems(pageNumber, context.getCandidatePageSize());
        hasNext = page.hasNext();
        if (!page.isEmpty()) {
            int currentPage = pageNumber;
            futures.add(CompletableFuture.supplyAsync(
                    () -> processPage(user, page.content(), context, currentPage), executor)
                    .completeOnTimeout(PageResult.empty(currentPage), context.getAsyncTimeoutMillis(), TimeUnit.MILLISECONDS)
                    .exceptionally(error -> {
                        logger.warn("Recommendation page {} failed for user {}: {}",
                                currentPage, user.getId(), error.getMessage());
                        return PageResult.empty(currentPage);
                    }));
        }
        pageNumber++;
    } while (hasNext);

    Map<String, List<ScoredItem>> pageRepresentatives = new HashMap<>();
    for (CompletableFuture<PageResult> future : futures) {
        PageResult result = future.join();
        if (!result.representatives().isEmpty()) {
            pageRepresentatives.put("page-" + result.pageNumber(), result.representatives());
        }
    }

    if (pageRepresentatives.isEmpty()) {
        return List.of();
    }

    return postProcess(pageAggregationStrategy.aggregate(pageRepresentatives, context));
}
```

Add helper methods and record inside the class:

```java
private PageResult processPage(User user, List<Item> candidates, RecommendationContext context, int pageNumber) {
    Map<String, List<ScoredItem>> strategyResults = executeStrategies(user, candidates, context);
    List<ScoredItem> representatives = pageAggregationStrategy.aggregate(strategyResults, context).stream()
            .limit(context.getRepresentativesPerPage())
            .toList();
    return new PageResult(pageNumber, representatives);
}

private record PageResult(int pageNumber, List<ScoredItem> representatives) {
    private static PageResult empty(int pageNumber) {
        return new PageResult(pageNumber, List.of());
    }
}
```

- [ ] **Step 5: Run engine tests**

Run: `mvn test -Dtest=HybridRecommendationEngineTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/domain/motor/HybridRecommendationEngine.java src/test/java/org/tvl/tvlooker/domain/motor/HybridRecommendationEngineTest.java
git commit -m "feat: process recommendation pages asynchronously"
```

## Task 7: Move Providers To Paged Gateway Reads

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/domain/motor/utils/provider/ItemPopularityProvider.java`
- Modify: `src/main/java/org/tvl/tvlooker/domain/motor/utils/provider/ItemFeatureVectorProvider.java`
- Modify: `src/main/java/org/tvl/tvlooker/domain/motor/utils/provider/UserProfileProvider.java`
- Modify: `src/main/java/org/tvl/tvlooker/domain/motor/utils/provider/MatrixFactorizationProvider.java`
- Modify: provider tests under `src/test/java/org/tvl/tvlooker/domain/motor/utils/provider/`

- [ ] **Step 1: Add provider tests for gateway-backed contexts**

In `ItemPopularityProviderTest.java`, add a test that builds context with only gateway data:

```java
@Test
@DisplayName("Should compute popularity from paged gateway data")
void shouldComputePopularityFromPagedGatewayData() {
    ItemPopularityProvider provider = new ItemPopularityProvider();
    RecommendationContext context = RecommendationContext.builder()
            .dataGateway(TestDataFactory.createGateway(TestDataFactory.createItems(3)))
            .candidatePageSize(2)
            .build();

    Map<Long, Double> scores = provider.provide(context);

    assertThat(scores).hasSize(3);
    assertThat(scores.get(3L)).isEqualTo(1.0);
}
```

In `MatrixFactorizationProviderTest.java`, add a test that uses gateway interactions and no `context.interactions` list:

```java
@Test
@DisplayName("Should compute SVD factors from paged gateway interactions")
void shouldComputeSvdFactorsFromPagedGatewayInteractions() {
    User user = TestDataFactory.createUser("svd-user");
    List<Item> items = TestDataFactory.createItems(3);
    List<Interaction> interactions = TestDataFactory.createInteractionsForUser(user, items);
    RecommendationContext context = RecommendationContext.builder()
            .dataGateway(TestDataFactory.createGateway(items, interactions, List.of()))
            .candidatePageSize(2)
            .build();

    SVDFactors factors = provider.provide(context);

    assertThat(factors).isNotNull();
}
```

- [ ] **Step 2: Run provider tests to verify failures**

Run: `mvn test -Dtest=ItemPopularityProviderTest,ItemFeatureVectorProviderTest,UserProfileProviderTest,MatrixFactorizationProviderTest`

Expected: FAIL because providers still read bulk context lists.

- [ ] **Step 3: Add paging helpers inside providers**

For each provider that reads all items/interactions/reviews, add private helpers following this pattern:

```java
private List<Item> loadItems(RecommendationContext context) {
    if (context.getDataGateway() == null) {
        return context.getItems() == null ? List.of() : context.getItems();
    }
    List<Item> items = new java.util.ArrayList<>();
    int pageNumber = 0;
    boolean hasNext;
    do {
        org.tvl.tvlooker.domain.motor.utils.DataPage<Item> page =
                context.getDataGateway().getAllItems(pageNumber, context.getCandidatePageSize());
        items.addAll(page.content());
        hasNext = page.hasNext();
        pageNumber++;
    } while (hasNext);
    return items;
}
```

Use equivalent helpers for `Interaction` and `Review`:

```java
private List<Interaction> loadInteractions(RecommendationContext context) { ... context.getDataGateway().getAllInteractions(...) ... }

private List<Review> loadReviews(RecommendationContext context) { ... context.getDataGateway().getAllReviews(...) ... }
```

- [ ] **Step 4: Replace direct context list reads**

Apply these replacements:

- In `ItemPopularityProvider.provide`, replace `context.getItems()` with `loadItems(context)`.
- In `ItemFeatureVectorProvider.provide`, replace `List<Item> items = context.getItems();` with `List<Item> items = loadItems(context);`.
- In `UserProfileProvider.provide`, replace `context.getInteractions()` with `loadInteractions(context)` and replace `buildReviewScoreMap(context)` with `buildReviewScoreMap(loadReviews(context))`.
- In `MatrixFactorizationProvider.provide`, replace `context.getInteractions()` with `loadInteractions(context)`.

For `UserProfileProvider`, change `buildReviewScoreMap` signature to:

```java
private Map<Long, Double> buildReviewScoreMap(List<Review> reviews) {
    if (reviews == null) {
        return Map.of();
    }
    Map<Long, Double> map = new HashMap<>();
    for (Review review : reviews) {
        if (review.getId() != null) {
            map.put(review.getId(), review.getScore() != null ? review.getScore() : 4.0);
        }
    }
    return map;
}
```

- [ ] **Step 5: Run provider tests**

Run: `mvn test -Dtest=ItemPopularityProviderTest,ItemFeatureVectorProviderTest,UserProfileProviderTest,MatrixFactorizationProviderTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/domain/motor/utils/provider src/test/java/org/tvl/tvlooker/domain/motor/utils/provider
git commit -m "feat: read recommendation provider data in pages"
```

## Task 8: Remove Bulk Context Fields From Recommendation Flow Tests

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/domain/motor/utils/RecommendationContext.java`
- Modify: `src/test/java/org/tvl/tvlooker/testutil/TestDataFactory.java`
- Modify: affected tests under `src/test/java/org/tvl/tvlooker/domain/motor/` and `src/test/java/org/tvl/tvlooker/domain/strategy/`

- [ ] **Step 1: Update test factory context builders to use gateway**

Replace `createContext` implementation in `TestDataFactory` with:

```java
public static RecommendationContext createContext(
        List<User> users,
        List<Item> items,
        List<Interaction> interactions) {
    User targetUser = users == null || users.isEmpty() ? null : users.get(0);
    return RecommendationContext.builder()
            .targetUser(targetUser)
            .dataGateway(createGateway(items == null ? new ArrayList<>() : items,
                    interactions == null ? new ArrayList<>() : interactions,
                    new ArrayList<>()))
            .candidatePageSize(100)
            .representativesPerPage(10)
            .asyncTimeoutMillis(10_000L)
            .dataProviders(new HashMap<>())
            .dataCache(new HashMap<>())
            .build();
}
```

- [ ] **Step 2: Remove bulk list fields from `RecommendationContext`**

Delete these fields from `RecommendationContext.java`:

```java
private List<User> users;
private List<Item> items;
private List<Interaction> interactions;
private List<Review> reviews;
```

Replace `checkDataNotNull()` with:

```java
public boolean checkDataNotNull() {
    return dataGateway != null;
}
```

- [ ] **Step 3: Compile tests to find remaining bulk-list calls**

Run: `mvn test -DskipTests`

Expected: FAIL if any code still calls `getItems()`, `getUsers()`, `getInteractions()`, or `getReviews()` on `RecommendationContext`.

- [ ] **Step 4: Replace remaining context getter usage**

For strategy/provider tests that need candidate items, read from the gateway fixture or pass candidates directly into strategies. Example replacement:

```java
List<Item> candidates = context.getDataGateway().getCandidateItems(0, 100).content();
List<ScoredItem> result = strategy.recommend(testUser, candidates, context);
```

- [ ] **Step 5: Run core tests**

Run: `mvn test -Dtest=HybridRecommendationEngineTest,RecommendationServiceTest,PopularityStrategyTest,ContentBasedStrategyTest,MatrixFactorizationStrategyTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/domain/motor/utils/RecommendationContext.java src/test/java/org/tvl/tvlooker/testutil/TestDataFactory.java src/test/java/org/tvl/tvlooker/domain src/test/java/org/tvl/tvlooker/service/RecommendationServiceTest.java
git commit -m "refactor: remove bulk recommendation context data"
```

## Task 9: Validate API Behavior And Full Backend Build

**Files:**
- Modify only if tests expose a compile or behavioral regression.
- Test: `src/test/java/org/tvl/tvlooker/api/controller/RecommendationControllerTest.java`
- Test: `src/test/java/org/tvl/tvlooker/service/RecommendationServiceIntegrationTest.java`

- [ ] **Step 1: Run API and integration tests**

Run: `mvn test -Dtest=RecommendationControllerTest,RecommendationServiceIntegrationTest`

Expected: PASS. The public API still returns `RecommendationResponse` with `items`, `count`, and `userId` unchanged.

- [ ] **Step 2: Fix any test setup that still assumes bulk context**

If integration tests fail because Spring wiring changed, update test properties to keep at least popularity enabled:

```java
@SpringBootTest(properties = {
        "recommendation.strategies.popularity.enabled=true",
        "recommendation.strategies.content.enabled=false",
        "recommendation.strategies.matrix-factorization.enabled=false"
})
```

- [ ] **Step 3: Run full test suite**

Run: `mvn test`

Expected: PASS.

- [ ] **Step 4: Run verify checks**

Run: `mvn clean verify`

Expected: PASS, including checkstyle and PMD.

- [ ] **Step 5: Update graphify after code changes**

Run: `graphify update .`

Expected: graph updates successfully. Dirty `graphify-out/` files are expected and should be included only if the repository already tracks them and the user wants them committed.

- [ ] **Step 6: Commit final verification fixes**

```bash
git status --short
git add src/main/java src/test/java src/main/resources/application.properties
git commit -m "test: verify async recommendation pipeline"
```

## Self-Review

**Spec coverage:**
- Database-level pagination is covered by Tasks 1, 2, 3, and 4.
- Async page execution is covered by Tasks 5 and 6.
- Local and global Borda tournament aggregation is covered by Task 6.
- Global model providers using paged domain data are covered by Task 7.
- Removal of bulk context data is covered by Task 8.
- Error handling for page failures and timeouts is covered by Task 6.
- API behavior and backend verification are covered by Task 9.

**Placeholder scan:** No placeholder markers, incomplete sections, or ambiguous instructions remain.

**Type consistency:** `DataPage`, `RecommendationDataGateway`, `RecommendationDomainDataGateway`, `RecommendationContext`, and `HybridRecommendationEngine` signatures are consistent across tasks.
