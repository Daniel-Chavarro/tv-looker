# Pagination Strategy Design for `getAllItems`

## 1. Overview
The `getAllItems` API endpoint currently causes frontend applications to crash when it attempts to load and render thousands of records simultaneously (>10,000 items). To alleviate these memory and performance issues, we are transitioning to an **Offset-based Pagination** strategy via Spring Data JPA's native capabilities.

## 2. Core Strategy & API Definition
- **Pagination Strategy**: Offset-based, `Page<T>` implementation.
- **Default Page Size**: `50` items per page.
  - *Justification*: 50 items is a "sweet spot" size that allows seamless rendering on standard UIs without DOM freezing or frontend memory exhaustion, while maintaining a very fast backend response time.
- **Max Page Size Constraint**: We will cap requests at `1000` items maximum per request to prevent intentional or accidental massive queries.
- **API Example**: 
  `GET /api/items?page=0&size=50&sort=id,desc`

## 3. Backend Technical Approach
- **Framework integration**: We will utilize Spring Data JPA's `Pageable` and `Page<T>` interface.
- **Controller Layer**: Inject `Pageable` directly into the mapped Controller endpoint method, annotated with `@PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC)`.
- **Repository/Service Layer**: Convert `List<Item> findAll()` methods to `Page<Item> findAll(Pageable pageable)`.
- **Response Shape**: The API payload will switch from a raw array to a metadata-rich JSON envelope:
  ```json
  {
    "content": [{ "id": 1, "name": "Item Name" }],
    "pageable": { "sort": { ... }, "pageNumber": 0, "pageSize": 50 },
    "totalPages": 200,
    "totalElements": 10000,
    "last": false,
    "size": 50,
    "number": 0
  }
  ```

## 4. Frontend Impact
- **Resolution**: Fetching fixed sizes of 50 items natively stops the memory overload problem crashing the client.
- **Integration**: The frontend UI requires updating to read `content` inside the envelope, and render table-style Pagination Controls based on `totalPages` and the current `number`.
- **Loading states**: The frontend needs intermediate loading states when requesting the next page of `50`.

## 5. Error Handling
- **Constraint Handling**: Enforce a global configuration `spring.data.web.pageable.max-page-size=1000`.
- **Invalid parameters**: Any malformed or negative page requests will be caught by Spring's `MethodArgumentNotValidException`, returning a standard 400 Bad Request.
- **Out of bounds paging**: Requesting page `9999` when only 200 exist returns an empty `"content": []` array instead of throwing an unhandled exception, providing standard API robustness.

## 6. Future Scalability & Recommendation Integration
- **Per-Pagination Recommendations**: When building per-page contextual recommendations in the future, the backend will compute these strictly against the `O(N)` bounds of the current page size (`N=50`), ensuring the recommendations API remains extremely fast. This could optionally be triggered by an `includeRecommendations=true` query parameter, injected directly into the response envelope.
- **Aggregated Best Recommendations**: Instead of scanning millions of records on the fly, a background worker or materialized view will aggregate and compute top recommendations asynchronously. A secondary endpoint (`GET /api/recommendations/top`) will serve these paginated, pre-computed recommendations globally, preventing any single endpoint from being a scalability bottleneck.