# Pagination Strategy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement offset-based pagination on the `getAllItems` API endpoint to handle large data volumes and prevent frontend memory overload.

**Architecture:** We use Spring Data JPA's native `Pageable` and `Page<T>` types. The `ItemRepository` already supports `findAll(Pageable)` natively by extending `JpaRepository`. We will expose a new `getAll(Pageable)` method in the `ItemService` (leaving the no-arg `getAll()` intact for internal usages) and adapt `ItemController` to consume the page parameters and return a structured `Page<ItemResponse>`.

**Tech Stack:** Java, Spring Boot, Spring Data JPA, MockMvc

---

### Task 1: Application Configuration

**Files:**
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: Set max page size limit**

Append the following configuration to `src/main/resources/application.properties` at the bottom of the "Application Configuration" block (around line 25):

```properties
# Pagination Configuration
spring.data.web.pageable.max-page-size=1000
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/application.properties
git commit -m "chore: configure max page size for pagination"
```

---

### Task 2: Service Layer Implementation

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/ItemService.java`
- Modify: `src/test/java/org/tvl/tvlooker/service/ItemServiceTest.java`

- [ ] **Step 1: Write failing test in `ItemServiceTest.java`**

First, add necessary imports at the top of `src/test/java/org/tvl/tvlooker/service/ItemServiceTest.java`:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
```

Then add this new test method inside the class:

```java
    @Test
    void givenPageable_whenGetAll_thenReturnsPageOfItems() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<ItemEntity> entities = List.of(itemEntity, createItemEntityWithId(2L));
        Page<ItemEntity> entityPage = new PageImpl<>(entities, pageable, 2);
        
        when(itemRepository.findAll(pageable)).thenReturn(entityPage);

        // Act
        Page<Item> result = itemService.getAll(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals("Test Title", result.getContent().get(0).getTitle());
        verify(itemRepository, times(1)).findAll(pageable);
    }
```

- [ ] **Step 2: Verify test fails**

Run: `mvnw test -Dtest=ItemServiceTest#givenPageable_whenGetAll_thenReturnsPageOfItems`
Expected: Compilation failure because `itemService.getAll(Pageable)` doesn't exist.

- [ ] **Step 3: Implement `getAll(Pageable)` in `ItemService.java`**

Add imports to `src/main/java/org/tvl/tvlooker/service/ItemService.java`:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Add the method logic below the existing `getAll()`:

```java
    /**
     * Get all items with pagination.
     *
     * @param pageable pagination information
     * @return page of items
     */
    public Page<Item> getAll(Pageable pageable) {
        return itemRepository.findAll(pageable)
                .map(ItemEntityMapper::toDomain);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvnw test -Dtest=ItemServiceTest#givenPageable_whenGetAll_thenReturnsPageOfItems`
Expected: Passes successfully.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/service/ItemService.java src/test/java/org/tvl/tvlooker/service/ItemServiceTest.java
git commit -m "feat: add paginated getAll method to ItemService"
```

---

### Task 3: Controller Layer Implementation

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/api/controller/ItemController.java`
- Modify: `src/test/java/org/tvl/tvlooker/api/controller/ItemControllerTest.java`

- [ ] **Step 1: Update MockMvc configuration and write failing tests**

In `src/test/java/org/tvl/tvlooker/api/controller/ItemControllerTest.java`, add the imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
```

Update `setUp()` to support Pageable:
```java
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(itemController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        // ... (keep the rest of setUp)
```

Replace the ENTIRE `GetAllItems` nested class with the new tests reflecting pagination structure:

```java
    @Nested
    class GetAllItems {

        @Test
        void givenItemsExist_whenGetAllItems_thenReturnsPaginatedList() throws Exception {
            Item secondItem = Item.builder()
                    .id(2L)
                    .title("Test TV Show")
                    .overview("This is a test TV show overview")
                    .releaseDate(LocalDate.of(2024, 2, 20))
                    .popularity(new BigDecimal("8.0"))
                    .voteAverage(new BigDecimal("8.5"))
                    .tmdbType(TmdbType.TV)
                    .tmdbId(67890L)
                    .genres(new HashSet<>())
                    .directors(new HashSet<>())
                    .actorsInItem(new HashSet<>())
                    .build();
            List<Item> items = Arrays.asList(testItem, secondItem);
            Page<Item> itemPage = new PageImpl<>(items, PageRequest.of(0, 50), items.size());

            when(itemService.getAll(any(Pageable.class))).thenReturn(itemPage);

            mockMvc.perform(get("/api/v1/items")
                            .param("page", "0")
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.content[0].title", is("Test Movie")))
                    .andExpect(jsonPath("$.content[1].title", is("Test TV Show")))
                    .andExpect(jsonPath("$.totalElements", is(2)))
                    .andExpect(jsonPath("$.totalPages", is(1)))
                    .andExpect(jsonPath("$.size", is(50)))
                    .andExpect(jsonPath("$.number", is(0)));

            verify(itemService, times(1)).getAll(any(Pageable.class));
        }

        @Test
        void givenNoItems_whenGetAllItems_thenReturnsEmptyPage() throws Exception {
            Page<Item> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
            when(itemService.getAll(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/items"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));

            verify(itemService, times(1)).getAll(any(Pageable.class));
        }
    }
```

- [ ] **Step 2: Verify test fails**

Run: `mvnw test -Dtest=ItemControllerTest`
Expected: Compilation or runtime failure because `itemController.getAllItems` returns a List, or `itemService.getAll(Pageable)` isn't mocked properly against the controller yet.

- [ ] **Step 3: Implement pagination in `ItemController.java`**

Add imports to `src/main/java/org/tvl/tvlooker/api/controller/ItemController.java`:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
```

Update `getAllItems` method in `ItemController.java` to:

```java
    /**
     * Retrieves all items with pagination.
     * @param pageable Pagination configuration.
     * @return A paginated list of item responses.
     */
    @GetMapping
    public ResponseEntity<Page<ItemResponse>> getAllItems(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Item> itemsPage = ITEM_SERVICE.getAll(pageable);
        Page<ItemResponse> response = itemsPage.map(ItemMapper::toResponse);
        return ResponseEntity.ok(response);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvnw test -Dtest=ItemControllerTest`
Expected: All tests pass successfully.

- [ ] **Step 5: Run full test suite for regression check**

Run: `mvnw test`
Expected: All tests in the project should pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/api/controller/ItemController.java src/test/java/org/tvl/tvlooker/api/controller/ItemControllerTest.java
git commit -m "feat: implement offset-based pagination in ItemController getAllItems"
```