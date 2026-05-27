# Recommendation Controller End-To-End Tests

## Objective
Add end-to-end integration tests for `GET /api/v1/users/{userId}/recommendations` using fake H2 data. The tests should prove that recommendations can flow from the HTTP controller through the real service, paged data gateway, asynchronous recommendation engine, providers, aggregation, DTO mapping, and final JSON response.

The tests intentionally bypass Spring Security filters. Security authorization is already covered elsewhere; these tests focus on recommendation behavior and response correctness.

## Current Gap
The project currently has two useful but separate layers of coverage:
- `RecommendationControllerTest` uses standalone `MockMvc` with a mocked `RecommendationService`. It verifies routing, limit defaults, and JSON shape, but it does not execute the recommendation pipeline.
- `RecommendationServiceIntegrationTest` uses Spring and H2 repositories. It verifies service-level recommendations, but it does not exercise the HTTP endpoint or response mapping.

The missing coverage is a full Spring `MockMvc` test that calls the recommendation endpoint and gets results from seeded fake database items through the real recommendation stack.

## Test Architecture
Create a new integration test class:

`src/test/java/org/tvl/tvlooker/api/controller/RecommendationControllerIntegrationTest.java`

Use:
- `@SpringBootTest`
- `@AutoConfigureMockMvc(addFilters = false)`
- `@ActiveProfiles("test")`
- `@TestPropertySource` for deterministic recommendation behavior

The test class should not use class-level `@Transactional`. Avoiding a test transaction keeps the test realistic for HTTP requests and prevents Hibernate sessions from hiding lazy-loading failures.

## Deterministic Recommendation Configuration
The tests should keep the recommendation behavior predictable while still exercising the paginated engine path.

Use test properties equivalent to:
- `recommendation.strategies.popularity.enabled=true`
- `recommendation.strategies.content.enabled=false`
- `recommendation.strategies.matrix-factorization.enabled=false`
- `recommendation.aggregation.type=ranking`
- `recommendation.pipeline.candidate-page-size=2`
- `recommendation.pipeline.representatives-per-page=2`
- `recommendation.pipeline.async-timeout-ms=10000`

Popularity-only recommendations make expected ordering simple: higher item popularity should rank ahead of lower item popularity. Small page size forces candidate pagination so the test covers the new page tournament flow.

## Fake Data
Seed H2 directly through repositories in `@BeforeEach` or per-test helper methods.

Required fixtures:
- One `UserEntity` target user.
- At least five `ItemEntity` records with distinct popularity values.
- One item with genre, director, and actor relationship data so `ItemEntityMapper` traverses lazy associations during recommendation item loading.

Interactions and reviews are not required for the first version because the tests use popularity-only recommendations. They can be added later if a content-based or matrix-factorization E2E scenario is needed.

Cleanup should delete repository data after each test in dependency-safe order, including relationship-owning entities where needed.

## Core Scenarios

### 1. Returns Recommendations From Real Endpoint
Seed fake items with known popularity values, call:

`GET /api/v1/users/{userId}/recommendations?limit=3`

Assert:
- HTTP status is `200 OK`.
- `$.userId` matches the seeded user ID.
- `$.count` is `3`.
- `$.items` has three entries.
- Returned item titles or IDs follow expected popularity order.

### 2. Respects Limit Query Parameter
Call the same endpoint with `?limit=1`.

Assert:
- HTTP status is `200 OK`.
- `$.count` is `1`.
- `$.items` has one entry.
- The single item is the highest-ranked fake recommendation.

### 3. Processes Multiple Candidate Pages
Set `recommendation.pipeline.candidate-page-size=2` and seed at least five items.

Assert the response can include an item that comes from beyond the first candidate page. This proves the controller request reaches the paginated recommendation pipeline rather than only scoring the first database page.

### 4. Does Not Fail On Lazy Item Associations
Seed at least one recommended item with related genre, director, and actor data.

Call the endpoint without a test-level transaction.

Assert:
- HTTP status is `200 OK`.
- Response contains the associated item.
- No `LazyInitializationException` occurs while mapping domain items to the API response.

This scenario protects against regressions where repository data is mapped after the Hibernate session closes, especially in async recommendation threads.

## Error Handling Scope
This spec focuses on successful controller-to-result behavior. Existing controller and service tests already cover invalid limits, missing users, and exception propagation. If later E2E bugs appear around error responses, add a separate integration scenario rather than broadening this first test class.

## Verification
After implementation, run:

`mvn clean test "-Dtest=RecommendationControllerIntegrationTest"`

Then run the broader backend verification:

`mvn clean verify`

Because project instructions require the knowledge graph to stay current after code changes, run:

`graphify update .`

## Out Of Scope
- Testing JWT or role authorization for the recommendation endpoint.
- Content-based or matrix-factorization specific ranking assertions.
- Real TMDB data loading.
- Frontend E2E coverage.
