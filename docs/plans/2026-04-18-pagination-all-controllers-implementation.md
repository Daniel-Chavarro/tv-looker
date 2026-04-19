# Pagination Implementation for All GetAll Endpoints

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement offset-based pagination on 6 controllers (Actor, Director, Interaction, ListFavorite, Review, User) using the generic PageResponse<T> DTO.

**Architecture:** Same pattern as ItemController - add `getAll(Pageable)` to each service, update controller to accept `Pageable` and return `PageResponse<TResponse>`. Use existing mappers to convert entity → DTO.

**Tech Stack:** Java, Spring Boot, Spring Data JPA, MockMvc

**Note:** `PageResponse<T>` DTO already exists at `src/main/java/org/tvl/tvlooker/api/dto/response/PageResponse.java`

---

## Task 1: ActorController Pagination

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/ActorService.java`
- Modify: `src/test/java/org/tvl/tvlooker/service/ActorServiceTest.java`
- Modify: `src/main/java/org/tvl/tvlooker/api/controller/ActorController.java`
- Modify: `src/test/java/org/tvl/tvlooker/api/controller/ActorControllerTest.java`

- [ ] **Step 1: Add getAll(Pageable) to ActorService**

Add imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Add method:
```java
    /**
     * Get all actors with pagination.
     *
     * @param pageable pagination information
     * @return page of actors
     */
    public Page<Actor> getAll(Pageable pageable) {
        return actorRepository.findAll(pageable)
                .map(ActorEntityMapper::toDomain);
    }
```

- [ ] **Step 2: Add test for getAll(Pageable) in ActorServiceTest**

Add imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
```

Add test:
```java
    @Test
    void givenPageable_whenGetAll_thenReturnsPageOfActors() {
        Pageable pageable = PageRequest.of(0, 10);
        List<ActorEntity> entities = List.of(actorEntity, createActorEntityWithId(2L));
        Page<ActorEntity> entityPage = new PageImpl<>(entities, pageable, 2);
        
        when(actorRepository.findAll(pageable)).thenReturn(entityPage);

        Page<Actor> result = actorService.getAll(pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(actorRepository, times(1)).findAll(pageable);
    }
```

- [ ] **Step 3: Run test to verify it passes**

Run: `mvnw test -Dtest=ActorServiceTest#givenPageable_whenGetAll_thenReturnsPageOfActors`
Expected: Pass

- [ ] **Step 4: Update ActorController for pagination**

Add imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.tvl.tvlooker.api.dto.response.PageResponse;
```

Update getAllActors method:
```java
    @GetMapping
    public ResponseEntity<PageResponse<ActorResponse>> getAllActors(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Actor> actorsPage = actorService.getAll(pageable);
        
        List<ActorResponse> content = actorsPage.getContent().stream()
                .map(actor -> ActorResponse.builder()
                        .id(actor.getId())
                        .name(actor.getName())
                        .tmdbId(actor.getTmdbId())
                        .build())
                .toList();
        
        PageResponse<ActorResponse> response = new PageResponse<>(
                content,
                actorsPage.getTotalElements(),
                actorsPage.getNumber(),
                actorsPage.getTotalPages(),
                actorsPage.isLast()
        );
        return ResponseEntity.ok(response);
    }
```

- [ ] **Step 5: Update ActorControllerTest**

Add imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
```

Update setUp() to support Pageable:
```java
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(actorController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
```

Replace GetAllActors tests with pagination tests:
```java
    @Nested
    class GetAllActors {

        @Test
        void givenActorsExist_whenGetAllActors_thenReturnsPaginatedList() throws Exception {
            Actor secondActor = Actor.builder()
                    .id(2L)
                    .name("Actor Two")
                    .tmdbId(22222L)
                    .build();
            List<Actor> actors = Arrays.asList(testActor, secondActor);
            Page<Actor> actorPage = new PageImpl<>(actors, PageRequest.of(0, 50), actors.size());

            when(actorService.getAll(any(Pageable.class))).thenReturn(actorPage);

            mockMvc.perform(get("/api/v1/actors")
                            .param("page", "0")
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.totalItems", is(2)))
                    .andExpect(jsonPath("$.actualPage", is(0)));

            verify(actorService, times(1)).getAll(any(Pageable.class));
        }

        @Test
        void givenNoActors_whenGetAllActors_thenReturnsEmptyPage() throws Exception {
            Page<Actor> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
            when(actorService.getAll(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/actors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalItems", is(0)));

            verify(actorService, times(1)).getAll(any(Pageable.class));
        }
    }
```

- [ ] **Step 6: Run tests to verify**

Run: `mvnw test -Dtest=ActorControllerTest`
Expected: Pass

- [ ] **Step 7: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/service/ActorService.java src/test/java/org/tvl/tvlooker/service/ActorServiceTest.java src/main/java/org/tvl/tvlooker/api/controller/ActorController.java src/test/java/org/tvl/tvlooker/api/controller/ActorControllerTest.java
git commit -m "feat: add pagination to ActorController getAllActors"
```

---

## Task 2: DirectorController Pagination

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/DirectorService.java`
- Modify: `src/test/java/org/tvl/tvlooker/service/DirectorServiceTest.java`
- Modify: `src/main/java/org/tvl/tvlooker/api/controller/DirectorController.java`
- Modify: `src/test/java/org/tvl/tvlooker/api/controller/DirectorControllerTest.java`

- [ ] **Step 1: Add getAll(Pageable) to DirectorService**

Add imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Add method:
```java
    /**
     * Get all directors with pagination.
     *
     * @param pageable pagination information
     * @return page of directors
     */
    public Page<Director> getAll(Pageable pageable) {
        return directorRepository.findAll(pageable)
                .map(DirectorEntityMapper::toDomain);
    }
```

- [ ] **Step 2: Add test for getAll(Pageable) in DirectorServiceTest**

Add imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
```

Add test:
```java
    @Test
    void givenPageable_whenGetAll_thenReturnsPageOfDirectors() {
        Pageable pageable = PageRequest.of(0, 10);
        List<DirectorEntity> entities = List.of(directorEntity, createDirectorEntityWithId(2L));
        Page<DirectorEntity> entityPage = new PageImpl<>(entities, pageable, 2);
        
        when(directorRepository.findAll(pageable)).thenReturn(entityPage);

        Page<Director> result = directorService.getAll(pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(directorRepository, times(1)).findAll(pageable);
    }
```

- [ ] **Step 3: Run test to verify**

Run: `mvnw test -Dtest=DirectorServiceTest#givenPageable_whenGetAll_thenReturnsPageOfDirectors`
Expected: Pass

- [ ] **Step 4: Update DirectorController for pagination**

Add imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.tvl.tvlooker.api.dto.response.PageResponse;
```

Update getAllDirectors method:
```java
    @GetMapping
    public ResponseEntity<PageResponse<DirectorResponse>> getAllDirectors(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Director> directorsPage = directorService.getAll(pageable);
        
        List<DirectorResponse> content = directorsPage.getContent().stream()
                .map(DirectorMapper::toResponse)
                .toList();
        
        PageResponse<DirectorResponse> response = new PageResponse<>(
                content,
                directorsPage.getTotalElements(),
                directorsPage.getNumber(),
                directorsPage.getTotalPages(),
                directorsPage.isLast()
        );
        return ResponseEntity.ok(response);
    }
```

- [ ] **Step 5: Update DirectorControllerTest**

Add imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
```

Update setUp():
```java
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(directorController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
```

Replace GetAllDirectors tests with pagination tests (same pattern as ActorControllerTest, but for /api/v1/directors endpoint):

- [ ] **Step 6: Run tests**

Run: `mvnw test -Dtest=DirectorControllerTest`
Expected: Pass

- [ ] **Step 7: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/service/DirectorService.java src/test/java/org/tvl/tvlooker/service/DirectorServiceTest.java src/main/java/org/tvl/tvlooker/api/controller/DirectorController.java src/test/java/org/tvl/tvlooker/api/controller/DirectorControllerTest.java
git commit -m "feat: add pagination to DirectorController getAllDirectors"
```

---

## Task 3: InteractionController Pagination

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/InteractionService.java`
- Modify: `src/test/java/org/tvl/tvlooker/service/InteractionServiceTest.java`
- Modify: `src/main/java/org/tvl/tvlooker/api/controller/InteractionController.java`
- Modify: `src/test/java/org/tvl/tvlooker/api/controller/InteractionControllerTest.java`

- [ ] **Step 1: Add getAll(Pageable) to InteractionService**

```java
    /**
     * Get all interactions with pagination.
     *
     * @param pageable pagination information
     * @return page of interactions
     */
    public Page<Interaction> getAll(Pageable pageable) {
        return interactionRepository.findAll(pageable)
                .map(InteractionEntityMapper::toDomain);
    }
```

- [ ] **Step 2: Add test for getAll(Pageable)**

Add test similar to previous services.

- [ ] **Step 3: Update InteractionController**

Update getAllInteractions method to use Pageable and return PageResponse:

```java
    @GetMapping
    public ResponseEntity<PageResponse<InteractionResponse>> getAllInteractions(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Interaction> interactionsPage = interactionService.getAll(pageable);
        
        List<InteractionResponse> content = interactionsPage.getContent().stream()
                .map(InteractionMapper::toResponse)
                .toList();
        
        PageResponse<InteractionResponse> response = new PageResponse<>(
                content,
                interactionsPage.getTotalElements(),
                interactionsPage.getNumber(),
                interactionsPage.getTotalPages(),
                interactionsPage.isLast()
        );
        return ResponseEntity.ok(response);
    }
```

- [ ] **Step 4: Update InteractionControllerTest**

Add Pageable support and pagination tests.

- [ ] **Step 5: Run tests and commit**

Run: `mvnw test -Dtest=InteractionControllerTest`
Commit with appropriate message.

---

## Task 4: ListFavoriteController Pagination

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/ListFavoriteService.java`
- Modify: `src/test/java/org/tvl/tvlooker/service/ListFavoriteServiceTest.java`
- Modify: `src/main/java/org/tvl/tvlooker/api/controller/ListFavoriteController.java`
- Modify: `src/test/java/org/tvl/tvlooker/api/controller/ListFavoriteControllerTest.java`

- [ ] **Step 1: Add getAll(Pageable) to ListFavoriteService**

```java
    public Page<ListFavorite> getAll(Pageable pageable) {
        return listFavoriteRepository.findAll(pageable)
                .map(ListFavoriteEntityMapper::toDomain);
    }
```

- [ ] **Step 2: Add test and implement**

- [ ] **Step 3: Update ListFavoriteController**

Method `listFavorites()` becomes paginated.

- [ ] **Step 4: Update tests and commit**

---

## Task 5: ReviewController Pagination

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/ReviewService.java`
- Modify: `src/test/java/org/tvl/tvlooker/service/ReviewServiceTest.java`
- Modify: `src/main/java/org/tvl/tvlooker/api/controller/ReviewController.java`
- Modify: `src/test/java/org/tvl/tvlooker/api/controller/ReviewControllerTest.java`

- [ ] **Step 1: Add getAll(Pageable) to ReviewService**

```java
    public Page<Review> getAll(Pageable pageable) {
        return reviewRepository.findAll(pageable)
                .map(ReviewEntityMapper::toDomain);
    }
```

- [ ] **Step 2: Add test and implement**

- [ ] **Step 3: Update ReviewController**

Method `getAllReviews()` becomes paginated.

- [ ] **Step 4: Update tests and commit**

---

## Task 6: UserController Pagination

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/UserService.java`
- Modify: `src/test/java/org/tvl/tvlooker/service/UserServiceTest.java`
- Modify: `src/main/java/org/tvl/tvlooker/api/controller/UserController.java`
- Modify: `src/test/java/org/tvl/tvlooker/api/controller/UserControllerTest.java`

- [ ] **Step 1: Add getAll(Pageable) to UserService**

```java
    public Page<User> getAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserEntityMapper::toDomain);
    }
```

- [ ] **Step 2: Add test and implement**

- [ ] **Step 3: Update UserController**

Method `getAllUsers()` becomes paginated.

- [ ] **Step 4: Update tests and commit**

---

## Final: Run Full Test Suite

After all tasks are complete, run the full test suite to ensure no regressions:

```bash
mvnw test
```

Expected: All tests pass.