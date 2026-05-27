# Recommendation Controller E2E Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add real Spring MockMvc end-to-end tests for `GET /api/v1/users/{userId}/recommendations` using fake H2 data and the real recommendation pipeline.

**Architecture:** Create one new integration test class that boots the Spring context, disables security filters, seeds deterministic fake repository data, calls the real HTTP endpoint, and asserts the JSON response. Configure popularity-only recommendations and a small candidate page size so expected order is stable while the paginated engine path is exercised.

**Tech Stack:** Java 21, Spring Boot 4, JUnit 5, Spring MockMvc, H2 test profile, AssertJ, Hamcrest JSON path matchers, Maven.

---

## Source Spec

Approved design: `docs/superpowers/specs/2026-05-27-recommendation-controller-e2e-design.md`

## File Structure

- Create: `src/test/java/org/tvl/tvlooker/api/controller/RecommendationControllerIntegrationTest.java`
  - Full Spring integration test for the recommendation controller.
  - Owns fake data setup and cleanup helpers.
  - Uses repositories directly for fixture setup.
  - Uses `MockMvc` to call the real endpoint.
- No production files should be changed unless the new E2E test exposes a real bug.
- Do not modify `RecommendationControllerTest`; it remains the fast mocked controller unit test.
- Do not modify `RecommendationServiceIntegrationTest`; it remains service-level integration coverage.

## Task 1: Add Full Spring Recommendation Controller E2E Test

**Files:**
- Create: `src/test/java/org/tvl/tvlooker/api/controller/RecommendationControllerIntegrationTest.java`

- [ ] **Step 1: Create the failing integration test class**

Create `src/test/java/org/tvl/tvlooker/api/controller/RecommendationControllerIntegrationTest.java` with this content:

```java
package org.tvl.tvlooker.api.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.ActorItemEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;
import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.repository.InteractionRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.repository.ReviewRepository;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "recommendation.strategies.popularity.enabled=true",
        "recommendation.strategies.content.enabled=false",
        "recommendation.strategies.matrix-factorization.enabled=false",
        "recommendation.aggregation.type=ranking",
        "recommendation.pipeline.candidate-page-size=2",
        "recommendation.pipeline.representatives-per-page=2",
        "recommendation.pipeline.async-timeout-ms=10000"
})
@DisplayName("RecommendationController E2E Tests")
class RecommendationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private DirectorRepository directorRepository;

    @Autowired
    private ActorRepository actorRepository;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private UserEntity targetUser;
    private ItemEntity mostPopularItem;
    private ItemEntity secondMostPopularItem;
    private ItemEntity thirdMostPopularItem;
    private ItemEntity pagedItem;
    private ItemEntity associatedItem;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        targetUser = userRepository.save(UserEntity.builder()
                .username("recommendation-e2e-user")
                .password("password123")
                .email("recommendation-e2e@test.com")
                .name("Recommendation E2E User")
                .build());

        mostPopularItem = itemRepository.save(createItem(1001L, "Aurora Prime", "990.0000"));
        secondMostPopularItem = itemRepository.save(createItem(1002L, "Binary Horizon", "880.0000"));
        thirdMostPopularItem = itemRepository.save(createItem(1003L, "Crimson Signal", "770.0000"));
        pagedItem = itemRepository.save(createItem(1004L, "Deep Page Candidate", "660.0000"));
        associatedItem = itemRepository.save(createAssociatedItem(1005L, "Lazy Association Sentinel", "550.0000"));
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    @DisplayName("GET recommendations returns real pipeline results from fake database items")
    void givenFakeItems_whenGetRecommendations_thenReturnsRecommendationsFromRealPipeline() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/recommendations", targetUser.getId())
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(targetUser.getId().toString())))
                .andExpect(jsonPath("$.count", is(3)))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].id", is(mostPopularItem.getId().intValue())))
                .andExpect(jsonPath("$.items[0].title", is("Aurora Prime")))
                .andExpect(jsonPath("$.items[*].title", hasItem(secondMostPopularItem.getTitle())))
                .andExpect(jsonPath("$.items[*].title", hasItem(thirdMostPopularItem.getTitle())));
    }

    @Test
    @DisplayName("GET recommendations respects limit query parameter")
    void givenLimitOne_whenGetRecommendations_thenReturnsSingleHighestRankedItem() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/recommendations", targetUser.getId())
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(targetUser.getId().toString())))
                .andExpect(jsonPath("$.count", is(1)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id", is(mostPopularItem.getId().intValue())))
                .andExpect(jsonPath("$.items[0].title", is("Aurora Prime")));
    }

    @Test
    @DisplayName("GET recommendations processes multiple candidate pages")
    void givenMoreItemsThanPageSize_whenGetRecommendations_thenIncludesPagedCandidates() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/recommendations", targetUser.getId())
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(5)))
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.items[*].title", hasItem(pagedItem.getTitle())))
                .andExpect(jsonPath("$.items[*].title", hasItem(associatedItem.getTitle())));
    }

    @Test
    @DisplayName("GET recommendations succeeds for items with lazy associations")
    void givenItemWithLazyAssociations_whenGetRecommendations_thenDoesNotFail() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/recommendations", targetUser.getId())
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].title", hasItem("Lazy Association Sentinel")));
    }

    private void cleanDatabase() {
        reviewRepository.deleteAll();
        interactionRepository.deleteAll();
        itemRepository.deleteAll();
        actorRepository.deleteAll();
        directorRepository.deleteAll();
        genreRepository.deleteAll();
        userRepository.deleteAll();
    }

    private ItemEntity createItem(Long tmdbId, String title, String popularity) {
        return ItemEntity.builder()
                .tmdbId(tmdbId)
                .tmdbType(TmdbType.MOVIE)
                .title(title)
                .overview("Overview for " + title)
                .releaseDate(LocalDate.of(2024, 1, 1))
                .popularity(new BigDecimal(popularity))
                .voteAverage(new BigDecimal("8.00"))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actorItems(new HashSet<>())
                .build();
    }

    private ItemEntity createAssociatedItem(Long tmdbId, String title, String popularity) {
        GenreEntity genre = genreRepository.saveAndFlush(GenreEntity.builder()
                .tmdbId(9001L)
                .name("Integration Genre")
                .build());
        DirectorEntity director = directorRepository.saveAndFlush(DirectorEntity.builder()
                .tmdbId(9002L)
                .name("Integration Director")
                .build());
        ActorEntity actor = actorRepository.saveAndFlush(ActorEntity.builder()
                .tmdbId(9003L)
                .name("Integration Actor")
                .build());

        ItemEntity item = createItem(tmdbId, title, popularity);
        item.setGenres(new HashSet<>(Set.of(genre)));
        item.setDirectors(new HashSet<>(Set.of(director)));

        ActorItemEntity actorItem = ActorItemEntity.builder()
                .item(item)
                .actor(actor)
                .characterName("Integration Character")
                .billingOrder(0)
                .build();
        item.getActorItems().add(actorItem);

        return item;
    }
}
```

- [ ] **Step 2: Run the new test and confirm whether it fails for test-code or product-code reasons**

Run:

```bash
mvn clean test "-Dtest=RecommendationControllerIntegrationTest"
```

Expected before any follow-up fixes:
- Preferred outcome: PASS, because production code already supports the E2E flow after the async pagination refactor and lazy-loading transaction fix.
- If FAIL, inspect the first root-cause stack trace before editing. Common likely failures are JSON path field names, fixture delete ordering, or a real recommendation pipeline issue.

Do not assert full global popularity order across all returned items. The engine uses page-local representatives plus global Borda aggregation, so items from different pages can tie. Keep strict ordering only for `items[0]`, which is deterministic because the highest-popularity fake item is also the lowest ID among the top page representatives.

- [ ] **Step 3: If JSON path field names fail, align assertions to `ItemResponse` without changing production code**

If the test fails only because response field names differ, inspect `src/main/java/org/tvl/tvlooker/api/dto/response/ItemResponse.java` and update only the JSON paths in `RecommendationControllerIntegrationTest.java`.

Use the endpoint's actual field names. Keep these behavioral assertions intact:

```java
.andExpect(jsonPath("$.userId", is(targetUser.getId().toString())))
.andExpect(jsonPath("$.count", is(3)))
.andExpect(jsonPath("$.items", hasSize(3)))
```

- [ ] **Step 4: If cleanup fails due to relationship constraints, make cleanup dependency-safe**

If `cleanDatabase()` fails because item relationships still reference actors, directors, or genres, update cleanup to clear item relationships before deleting related entities:

```java
private void cleanDatabase() {
    reviewRepository.deleteAll();
    interactionRepository.deleteAll();
    itemRepository.findAll().forEach(item -> {
        item.getActorItems().clear();
        item.getDirectors().clear();
        item.getGenres().clear();
        itemRepository.save(item);
    });
    itemRepository.deleteAll();
    actorRepository.deleteAll();
    directorRepository.deleteAll();
    genreRepository.deleteAll();
    userRepository.deleteAll();
}
```

Then rerun:

```bash
mvn clean test "-Dtest=RecommendationControllerIntegrationTest"
```

Expected: PASS. If it still fails, stop and use systematic debugging before changing production code.

- [ ] **Step 5: Run focused recommendation regression tests**

Run:

```bash
mvn clean test "-Dtest=RecommendationControllerIntegrationTest,RecommendationControllerTest,RecommendationServiceIntegrationTest,HybridRecommendationEngineTest,ItemServiceTest"
```

Expected: PASS. This confirms the new HTTP E2E test coexists with existing mocked controller, service integration, engine, and lazy-loading transaction coverage.

- [ ] **Step 6: Commit the new E2E test**

Inspect the staged diff before committing:

```bash
git status --short
git diff -- src/test/java/org/tvl/tvlooker/api/controller/RecommendationControllerIntegrationTest.java
git add src/test/java/org/tvl/tvlooker/api/controller/RecommendationControllerIntegrationTest.java
git diff --cached --stat
git commit -m "test: add recommendation controller e2e coverage"
```

Expected: Commit includes only `RecommendationControllerIntegrationTest.java`, unless a real production bug was found and fixed during implementation.

## Task 2: Run Full Verification and Update Graph

**Files:**
- Modify: `graphify-out/graph.json`
- Modify: `graphify-out/GRAPH_REPORT.md`
- Possible: `graphify-out/manifest.json`

- [ ] **Step 1: Run full Maven verification**

Run:

```bash
mvn clean verify
```

Expected: PASS with all backend tests, Checkstyle, and PMD passing.

- [ ] **Step 2: Update graphify after code changes**

Run:

```bash
graphify update .
```

Expected: graphify completes and updates `graphify-out/graph.json` and `graphify-out/GRAPH_REPORT.md`. If it skips `graph.html` because the graph has more than 5000 nodes, that is acceptable and should be noted in the final summary.

- [ ] **Step 3: Commit graph update if desired by repository convention**

Check current working tree and include only graphify files produced by this task if committing them:

```bash
git status --short
git add graphify-out/graph.json graphify-out/GRAPH_REPORT.md graphify-out/manifest.json
git diff --cached --stat
git commit -m "chore: update graph after recommendation e2e tests"
```

If `graphify-out/graph.html` is deleted only because the graph is over the HTML node limit, do not stage that deletion unless the repository already expects it.

- [ ] **Step 4: Report verification results**

Final response should include:
- New test file path.
- The exact Maven commands run and pass/fail result.
- Whether graphify update ran and whether `graph.html` was skipped.
- Commit hashes if commits were made.

## Self-Review

- Spec coverage: The plan creates the requested full Spring MockMvc E2E test, bypasses security, seeds fake H2 items, exercises pagination, verifies limits, and covers lazy associations.
- Red-flag scan: No incomplete task descriptions remain; failures have concrete debugging branches and commands.
- Type consistency: Repository, entity, property, and endpoint names match the approved spec and current codebase conventions.
