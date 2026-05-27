# Saveable Recommendations Design

## Goal

Persist generated recommendations so the backend does not run the recommendation motor on every fetch. Recommendations are reused until they expire by TTL.

## Current Context

- `RecommendationService.getUserRecommendations(UUID userId, int limit)` builds a `RecommendationContext`, runs `RecommendationEngine.recommend(...)`, and returns only `Item` DTOs.
- `RecommendationController` exposes `GET /api/v1/users/{userId}/recommendations` and returns `{ userId, count, items }`.
- The recommendation motor is domain code and should stay free of JPA and HTTP concerns.
- The frontend already uses React Query `staleTime`, but backend recomputation still happens whenever the API is called after frontend cache invalidation or across sessions.

## Scope

Implement phases 1 through 5:

- Database persistence for recommendation results.
- TTL-based cache reuse.
- Service-level regeneration when the cache is missing or expired.
- Unchanged API response shape.
- Unit and integration coverage.

Out of scope for the first implementation:

- Frontend UI changes.
- Manual refresh endpoint or query parameter.
- Expiration triggered by ratings, reviews, lists, or interactions.
- Separate recommendation batch metadata table.
- Background cleanup job for expired rows.

## Phase 1: Persistence Model

Add a `saved_recommendations` table represented by `SavedRecommendationEntity`.

Fields:

- `recommendation_id_pk`: generated primary key.
- `user_id_fk`: required many-to-one reference to `UserEntity`.
- `item_id_fk`: required many-to-one reference to `ItemEntity`.
- `score`: recommendation score from `ScoredItem`.
- `explanation`: optional explanation from `ScoredItem`.
- `source_strategy`: optional source strategy from `ScoredItem`.
- `rank_position`: zero-based rank in the generated recommendation set.
- `created_at`: creation timestamp.
- `expires_at`: timestamp after which the row is stale.

Repository operations:

- Find fresh recommendations for a user, ordered by `rankPosition`.
- Delete all saved recommendations for a user before saving a regenerated set.

Freshness is row-based using `expires_at > now`. The service will treat a user cache as usable only when fresh rows exist.

## Phase 2: Service Cache Flow

Update `RecommendationService.getUserRecommendations(UUID userId, int limit)` to be cache-aware and transactional.

Flow:

1. Validate `userId` and `limit`.
2. Load the user through `UserService` as today.
3. Query fresh saved recommendations for that user.
4. If fresh rows exist, return their items ordered by `rank_position`, limited by `limit`.
5. If no fresh rows exist, build the existing `RecommendationContext` and run the motor.
6. Delete existing saved recommendations for that user.
7. Save the generated scored recommendations with rank, score, explanation, source strategy, `created_at`, and `expires_at`.
8. Return the generated items limited by `limit`.

Configuration:

```properties
recommendation.cache.ttl=PT24H
recommendation.cache.generated-size=100
```

`recommendation.cache.ttl` controls expiration. `recommendation.cache.generated-size` controls the minimum number of recommendations saved per regeneration so a request for a small limit does not force recomputation when a later request asks for more. The effective save size is `max(limit, generated-size)` so direct service calls with a larger limit can still return up to that larger requested amount.

If a fresh cache has fewer rows than the requested limit, the service returns the available fresh rows rather than regenerating early. Regeneration is controlled only by absence or TTL expiry.

## Phase 3: API Contract

Keep the response unchanged:

```json
{
  "userId": "...",
  "count": 20,
  "items": []
}
```

The backend stores `score`, `explanation`, and `sourceStrategy`, but the first API version still returns only items. This avoids frontend changes and preserves the existing client contract.

## Phase 4: Expiration Policy

Use TTL only.

Rules:

- Fresh rows are reused.
- Expired or absent rows trigger regeneration.
- Regeneration replaces all saved recommendations for the user in one transaction.
- User activity does not invalidate saved recommendations in this version.

## Phase 5: Testing

Unit tests should cover:

- Cache miss calls the motor and saves rows.
- Cache hit returns saved rows and does not call the motor.
- Expired cache causes regeneration.
- `limit` is applied to cached rows.
- Invalid input still fails before cache or motor work.

Integration tests should cover:

- First request persists saved recommendation rows.
- Second request reuses persisted rows.
- Expired rows are replaced.
- Returned item order follows `rank_position`.

## Design Decisions

- Persistence belongs in the service layer, not the domain motor, because the motor boundary is intentionally JPA-free.
- A normalized table is preferred over JSON/blob storage so saved recommendations can join to `items`, preserve ordering, and keep score metadata queryable.
- The API response remains unchanged to keep the first implementation small and avoid unnecessary frontend churn.
- TTL-only expiration is the selected first policy because it directly solves repeated recomputation while keeping invalidation predictable.

## Acceptance Criteria

- Fetching recommendations for a user creates saved recommendation rows when no fresh cache exists.
- Fetching recommendations again before TTL expiry returns saved rows without calling the recommendation motor.
- Expired recommendations are regenerated and replaced.
- The existing `/api/v1/users/{userId}/recommendations` response shape remains compatible.
- Backend recommendation service tests and relevant integration tests pass.
