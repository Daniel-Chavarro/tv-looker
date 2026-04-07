# TMDB Services Testing Design

**Date:** 2026-04-07  
**Author:** TV Looker Team  
**Status:** Approved

## Objective

Design and implement comprehensive behavior-driven tests for the refactored TMDB services using Given-When-Then pattern. Tests should validate what each method does and returns, without relying on knowledge of implementation details.

## Problem

After refactoring TMDB services to follow SOLID principles:
- `TmdbDataFetcher` has new generic methods that need testing
- `TmdbDataSynchronizerService` now uses `TmdbDataFetcher` exclusively
- `TmdbDataCollectorService` now uses `TmdbDataFetcher` exclusively
- Existing tests are integration-style and need to shift to behavior-focused approach

## Solution

### Testing Philosophy

**Behavior-Driven Testing (Black-Box)**
- Test what each method does, not how it does it
- Focus on observable outcomes from the user's perspective
- Mock only external dependencies
- Use strict Given-When-Then format
- Descriptive test names that explain the scenario

### Test Structure

All tests follow this pattern:

```java
@Test
@DisplayName("Should [expected behavior] when [scenario]")
void test[MethodName]_[Scenario]() {
    // Given: [Setup preconditions and input data]
    
    // When: [Execute the method under test]
    
    // Then: [Assert expected outcomes]
}
```

## Test Coverage

### 1. TmdbDataFetcher Tests

**File:** `TmdbDataFetcherTest.java`

**Purpose:** Verify that the fetcher correctly retrieves data from TMDB and respects rate limits.

**Test Cases:**

1. **fetchDetailsWithCreditsAsync (generic) - Movies**
   - Given: A valid media type (MOVIE) and ID
   - When: Fetching details with credits
   - Then: Should return movie details with credits

2. **fetchDetailsWithCreditsAsync (generic) - TV Shows**
   - Given: A valid media type (TV) and ID
   - When: Fetching details with credits
   - Then: Should return TV show details with credits

3. **fetchChangesAsync**
   - Given: A media type, start date, end date, and page number
   - When: Fetching changes
   - Then: Should return a paged response with change items

4. **fetchPopularAsync (generic) - Movies**
   - Given: A media type (MOVIE) and page number
   - When: Fetching popular items
   - Then: Should return a paged response with popular movies

5. **fetchPopularAsync (generic) - TV Shows**
   - Given: A media type (TV) and page number
   - When: Fetching popular items
   - Then: Should return a paged response with popular TV shows

6. **Rate Limiting**
   - Given: Multiple concurrent fetch requests
   - When: Fetching data
   - Then: Should respect the configured rate limit

7. **fetchMovieDetailsAsync**
   - Given: A valid movie ID
   - When: Fetching movie details
   - Then: Should return movie details with credits

8. **fetchTvShowDetailsAsync**
   - Given: A valid TV show ID
   - When: Fetching TV show details
   - Then: Should return TV show details with credits

9. **fetchGenresAsync**
   - Given: A media type
   - When: Fetching genres
   - Then: Should return a list of genres for that media type

10. **fetchGenres (batch)**
    - Given: Request for all genres
    - When: Fetching genres for both movies and TV
    - Then: Should return combined list of genres

11. **fetchMoviesDetailsBatch**
    - Given: A list of movie IDs
    - When: Fetching movies in batch
    - Then: Should return details for all valid movies

12. **fetchTvShowsDetailsBatch**
    - Given: A list of TV show IDs
    - When: Fetching TV shows in batch
    - Then: Should return details for all valid TV shows

13. **fetchMoviesDetailsBatch - Empty list**
    - Given: An empty list of movie IDs
    - When: Fetching movies in batch
    - Then: Should return an empty list without errors

14. **fetchPopularMoviesAsync**
    - Given: A valid page number
    - When: Fetching popular movies
    - Then: Should return a paged response with popular movies

15. **fetchPopularTvShowsAsync**
    - Given: A valid page number
    - When: Fetching popular TV shows
    - Then: Should return a paged response with popular TV shows

**Mocks:** TmdbClient, Executor

### 2. TmdbDataSynchronizerService Tests

**File:** `TmdbDataSynchronizerServiceTest.java`

**Purpose:** Verify that the synchronizer correctly updates existing items and discovers new ones.

**Test Cases:**

1. **synchronize - Complete flow**
   - Given: Sync is enabled and last sync date is yesterday
   - When: Running synchronization
   - Then: Should sync movie changes, TV changes, and discover new popular items

2. **syncChanges - Updates existing items**
   - Given: A movie exists in the database and has changes on TMDB
   - When: Syncing changes for the date range
   - Then: Should fetch updated details and persist the changes

3. **syncChanges - Ignores non-existing items**
   - Given: TMDB reports changes for items not in our database
   - When: Syncing changes
   - Then: Should not persist any changes

4. **syncChanges - Multiple pages**
   - Given: Changes span multiple pages
   - When: Syncing changes
   - Then: Should process all pages and sync all changes

5. **syncChanges - Parallel processing**
   - Given: Multiple items have changes
   - When: Syncing changes
   - Then: Should fetch details for all items concurrently

6. **discoverNewPopularItems - Movies**
   - Given: Popular movies list contains new items
   - When: Discovering new popular movies
   - Then: Should persist only new movies not in database

7. **discoverNewPopularItems - TV Shows**
   - Given: Popular TV shows list contains new items
   - When: Discovering new popular TV shows
   - Then: Should persist only new TV shows not in database

8. **synchronize - Disabled sync**
   - Given: Sync is disabled
   - When: Running synchronization
   - Then: Should not perform any sync operations

9. **synchronize - Updates last sync date**
   - Given: Last sync date is yesterday
   - When: Synchronization completes successfully
   - Then: Should update last sync date to today

10. **syncChanges - Error handling**
    - Given: One item fails to sync
    - When: Syncing changes
    - Then: Should continue syncing other items

11. **syncChanges - Empty changes**
    - Given: No changes in the date range
    - When: Syncing changes
    - Then: Should complete without errors and return 0 updates

12. **discoverNewPopularItems - All existing**
    - Given: All popular items already exist in database
    - When: Discovering new popular items
    - Then: Should return 0 new items

**Mocks:** TmdbDataFetcher, ItemRepository, TmdbItemPersistenceService

### 3. TmdbDataCollectorService Tests

**File:** `TmdbDataCollectorServiceTest.java`

**Purpose:** Verify that the collector correctly retrieves and persists initial data from TMDB.

**Test Cases:**

1. **collectAll - Complete flow**
   - Given: Empty database
   - When: Collecting all data
   - Then: Should collect genres, movies, and TV shows

2. **collectGenres - Success**
   - Given: TMDB has movie and TV genres
   - When: Collecting genres
   - Then: Should fetch and persist both movie and TV genres

3. **collectGenres - Empty results**
   - Given: TMDB returns empty genre lists
   - When: Collecting genres
   - Then: Should complete without errors

4. **collectPopularMovies - New movies**
   - Given: Popular movies list contains items not in database
   - When: Collecting popular movies
   - Then: Should discover and persist new movies

5. **collectPopularMovies - Existing movies**
   - Given: Some movies already exist in database
   - When: Collecting popular movies
   - Then: Should skip existing movies and only add new ones

6. **collectPopularMovies - Multiple pages**
   - Given: Max pages is set to 3
   - When: Collecting popular movies
   - Then: Should fetch and process 3 pages

7. **collectPopularMovies - Empty results**
   - Given: TMDB returns empty results
   - When: Collecting popular movies
   - Then: Should complete without errors

8. **collectPopularTvShows - New TV shows**
   - Given: Popular TV shows list contains items not in database
   - When: Collecting popular TV shows
   - Then: Should batch fetch details and persist new TV shows

9. **collectPopularTvShows - Existing TV shows**
   - Given: Some TV shows already exist in database
   - When: Collecting popular TV shows
   - Then: Should skip existing TV shows

10. **collectPopularTvShows - Empty results**
    - Given: TMDB returns empty results
    - When: Collecting popular TV shows
    - Then: Should complete without errors

11. **collectAllAsync - Concurrent execution prevention**
    - Given: Collection is already in progress
    - When: Trying to start another collection
    - Then: Should throw TmdbCollectionInProgressException

12. **collectPopularMoviesAsync - Success**
    - Given: Valid configuration
    - When: Collecting movies asynchronously
    - Then: Should complete successfully and reset progress flag

13. **collectPopularTvShowsAsync - Success**
    - Given: Valid configuration
    - When: Collecting TV shows asynchronously
    - Then: Should complete successfully and reset progress flag

14. **isCollectionInProgress - Initially false**
    - Given: Service just initialized
    - When: Checking collection progress
    - Then: Should return false

15. **collectPopularMovies - Stops at max pages**
    - Given: Max pages is set to 2 but TMDB has 10 pages
    - When: Collecting popular movies
    - Then: Should only fetch 2 pages

16. **collectPopularTvShows - Stops at total pages**
    - Given: TMDB has only 1 page but max pages is 5
    - When: Collecting popular TV shows
    - Then: Should only fetch 1 page

**Mocks:** TmdbDataFetcher, ItemRepository, TmdbItemPersistenceService

## Testing Principles

### 1. Black-Box Approach
- Tests should not know about internal implementation
- Focus on method contracts: inputs and outputs
- Assert on results, not on how results were achieved

### 2. Given-When-Then Format
```java
// Given: Setup - create all necessary test data and mocks
TmdbMovieDto movie = new TmdbMovieDto(...);
when(fetcher.fetchPopularMoviesAsync(1))
    .thenReturn(CompletableFuture.completedFuture(response));

// When: Execute - call the method under test
collectorService.collectPopularMovies();

// Then: Assert - verify the expected behavior
verify(persistenceService).discoverAndPersistNewMovies(anyList());
```

### 3. Minimal Mocking
- Mock only external dependencies (TmdbClient, repositories, other services)
- Don't mock the class under test
- Don't verify internal method calls unless it's the observable behavior

### 4. Descriptive Naming
- Test names should read like documentation
- Format: `test[MethodName]_[Scenario]_[ExpectedOutcome]`
- Example: `testCollectGenres_EmptyResults_CompletesWithoutErrors`

### 5. Error Isolation
- Each test is independent
- Tests don't depend on execution order
- Setup and teardown handled by JUnit lifecycle

## Implementation Strategy

1. Create `TmdbDataFetcherTest.java` with 15 test cases
2. Update `TmdbDataSynchronizerServiceTest.java` with 12 test cases
3. Update `TmdbDataCollectorServiceTest.java` with 16 test cases
4. Run all tests to ensure they pass
5. Verify test coverage is comprehensive

## Success Criteria

- All test cases pass
- Tests follow Given-When-Then format
- Tests are behavior-focused (black-box)
- Test names are descriptive and self-documenting
- Each service has comprehensive coverage
- Tests remain valid even if internal implementation changes

## Notes

- Keep existing integration test (`TmdbDataCollectorLocalTest.java`) for manual verification with real API
- Use `@DisplayName` annotations for clear test descriptions
- Use `ReflectionTestUtils` sparingly, only for configuration values
- Mock async operations with `CompletableFuture.completedFuture()`
