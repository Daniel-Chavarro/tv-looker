# Pagination Strategy for All GetAll Endpoints

## 1. Overview & Scope

- **Goal**: Apply the same offset-based pagination pattern to 6 controllers to prevent frontend memory overload when sending large datasets.
- **Controllers to update**: Actor, Director, Interaction, ListFavorite, Review, User (excluding Genre per user request)
- **Pattern**: Same as ItemController — `@PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC)` returning `PageResponse<TResponse>`
- **Max Page Size**: Enforced globally via `spring.data.web.pageable.max-page-size=1000` (already configured)

## 2. Technical Approach

- **Reusable Generic `PageResponse<T>`**: Already exists in `api/dto/response/PageResponse.java`
  ```java
  public record PageResponse<T>(
      List<T> content,
      long totalItems,
      int actualPage,
      int totalPages,
      boolean isLast
  ){}
  ```

- **Controller Pattern** (same for all 6):
  - Accept `Pageable pageable` with `@PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC)`
  - Return `ResponseEntity<PageResponse<TResponse>>` 
  - Service transformation: `Page<Entity> → Page<DTO>`, then wrap in `PageResponse<TResponse>`
  - Use existing mappers to convert entity → DTO (not the repository `Page` directly)

- **Testing**: Each controller gets pagination tests similar to ItemControllerTest

## 3. Implementation Order

Following the subagent-driven-development pattern with TDD:

1. **Actor** - Service + Controller + Tests
2. **Director** - Service + Controller + Tests
3. **Interaction** - Service + Controller + Tests
4. **ListFavorite** - Service + Controller + Tests
5. **Review** - Service + Controller + Tests
6. **User** - Service + Controller + Tests

## 4. API Usage

All endpoints will accept:
```
GET /api/v1/{entity}?page=0&size=50&sort=id,desc
```

And return the paginated `PageResponse<T>` structure:
```json
{
  "content": [...],
  "totalItems": 10000,
  "actualPage": 0,
  "totalPages": 200,
  "isLast": false
}
```

## 5. Error Handling

Same as ItemController:
- Invalid parameters → 400 Bad Request (handled by Spring)
- Out of bounds page → Empty `content: []` array
- Max page size enforced to 1000