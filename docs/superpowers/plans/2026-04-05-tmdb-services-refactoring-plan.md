# Tmdb Services Refactoring Implementation Plan

> **For agentic workers:** Execute tasks one by one in this session.

**Goal:** Refactorizar TmdbDataSynchronizerService y TmdbDataCollectorService para usar TmdbDataFetcher como puerta de entrada única a la API.

**Architecture:** Centralizar todas las llamadas API a través de TmdbDataFetcher que maneja rate limiting. El sincronizador procesará cambios en paralelo.

**Tech Stack:** Java, Spring, CompletableFuture, RateLimiter

---

## Task 1: Add fetchChangesAsync to TmdbDataFetcher

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataFetcher.java`

- [ ] **Step 1: Add fetchChangesAsync method**

Add after existing methods (before the closing brace):

```java
public CompletableFuture<TmdbPagedResponseDto<TmdbChangesDto>> fetchChangesAsync(
        TmdbMediaType type, LocalDate startDate, LocalDate endDate, int page) {
    return CompletableFuture.supplyAsync(() -> {
        rateLimiter.acquire();
        return tmdbClient.getChanges(type, startDate, endDate, page);
    }, tmdbTaskExecutor);
}
```

Add the import:
```java
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbChangesDto;
```

- [ ] **Step 2: Run compile to verify**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataFetcher.java
git commit -m "feat(tmdb): add fetchChangesAsync to TmdbDataFetcher"
```

---

## Task 2: Add fetchDetailsWithCreditsAsync generic to TmdbDataFetcher

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataFetcher.java`

- [ ] **Step 1: Add generic fetchDetailsWithCreditsAsync method**

Add after fetchChangesAsync:

```java
public <T extends TmdbMediaDetails> CompletableFuture<T> fetchDetailsWithCreditsAsync(
        TmdbMediaType type, long id) {
    return CompletableFuture.supplyAsync(() -> {
        rateLimiter.acquire();
        return tmdbClient.getDetailsWithCredits(type, id);
    }, tmdbTaskExecutor);
}
```

- [ ] **Step 2: Run compile to verify**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataFetcher.java
git commit -m "feat(tmdb): add fetchDetailsWithCreditsAsync generic method"
```

---

## Task 3: Add fetchPopularAsync generic to TmdbDataFetcher

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataFetcher.java`

- [ ] **Step 1: Add generic fetchPopularAsync method**

Add after fetchDetailsWithCreditsAsync:

```java
public <T extends TmdbMediaItem> CompletableFuture<TmdbPagedResponseDto<T>> fetchPopularAsync(
        TmdbMediaType type, int page) {
    return CompletableFuture.supplyAsync(() -> {
        rateLimiter.acquire();
        return tmdbClient.getPopular(type, page);
    }, tmdbTaskExecutor);
}
```

- [ ] **Step 2: Run compile to verify**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataFetcher.java
git commit -m "feat(tmdb): add fetchPopularAsync generic method"
```

---

## Task 4: Refactor TmdbDataSynchronizerService to use TmdbDataFetcher

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataSynchronizerService.java`

- [ ] **Step 1: Update imports and dependencies**

Current dependencies (remove TmdbClient, add TmdbDataFetcher):
```java
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbItemMapper;
```

Replace with:
```java
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
```

Add new import:
```java
import org.tvl.tvlooker.service.tmdb.TmdbDataFetcher;
```

Change constructor dependency from TmdbClient to TmdbDataFetcher:
```java
private final TmdbDataFetcher fetcher;

public TmdbDataSynchronizerService(
        TmdbDataFetcher fetcher,
        ItemRepository itemRepository,
        TmdbItemPersistenceService persistenceService) {
    this.fetcher = fetcher;
    this.itemRepository = itemRepository;
    this.persistenceService = persistenceService;
}
```

Remove the `tmdbClient` field.

- [ ] **Step 2: Refactor syncChanges method**

Replace current syncChanges method:
```java
private int syncChanges(TmdbType type, LocalDate startDate, LocalDate endDate) {
    log.info("Syncing {} changes from {} to {}", type, startDate, endDate);

    int updatedCount = 0;
    int page = 1;
    int totalPages = 1;

    while (page <= totalPages) {
        TmdbPagedResponseDto<TmdbChangesDto> changes = fetcher.fetchChangesAsync(
                type == TmdbType.MOVIE ? TmdbMediaType.MOVIE : TmdbMediaType.TV,
                startDate, endDate, page).join();
        
        if (changes == null || changes.results() == null) {
            break;
        }

        totalPages = changes.totalPages();

        // Filter only items that exist in our DB
        List<Long> existingIds = changes.results().stream()
            .filter(change -> itemRepository.findByTmdbIdAndTmdbType(change.id(), type).isPresent())
            .map(TmdbChangesDto::id)
            .toList();

        if (!existingIds.isEmpty()) {
            TmdbMediaType mediaType = type == TmdbType.MOVIE ? TmdbMediaType.MOVIE : TmdbMediaType.TV;
            
            // Fetch details in parallel
            List<CompletableFuture<?>> futures = existingIds.stream()
                .map(id -> fetcher.fetchDetailsWithCreditsAsync(mediaType, id))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Process results
            for (int i = 0; i < existingIds.size(); i++) {
                long tmdbId = existingIds.get(i);
                try {
                    Object details = futures.get(i).join();
                    Optional<ItemEntity> existing = itemRepository.findByTmdbIdAndTmdbType(tmdbId, type);
                    if (existing.isPresent() && details != null) {
                        updateExistingItem(existing.get(), details);
                        updatedCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to sync {} (tmdbId={}): {}", type, tmdbId, e.getMessage());
                }
            }
        }

        page++;
    }

    log.info("Synced {} {} updates", updatedCount, type);
    return updatedCount;
}
```

- [ ] **Step 3: Refactor discoverNewMovies method**

Replace:
```java
private int discoverNewMovies(int page) {
    int count = 0;
    TmdbPagedResponseDto<?> response = fetcher.fetchPopularAsync(TmdbMediaType.MOVIE, page).join();

    if (response != null && response.results() != null) {
        for (var item : response.results()) {
            if (!itemRepository.existsByTmdbIdAndTmdbType(item.id(), TmdbType.MOVIE)) {
                try {
                    persistenceService.persistMovie(item);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to add new movie '{}': {}", item.title(), e.getMessage());
                }
            }
        }
    }
    return count;
}
```

Note: Need to handle the generic type. Since we need TmdbMovieDto, we cast or create specific method. Let's create specific methods instead - see Task 2 for that.

Actually, let's make specific methods for discoverNewMovies/discoverNewTvShows:

```java
private int discoverNewMovies(int page) {
    int count = 0;
    TmdbPagedResponseDto<TmdbMovieDto> response = fetcher.fetchPopularMoviesAsync(page).join();

    if (response != null && response.results() != null) {
        for (TmdbMovieDto movie : response.results()) {
            if (!itemRepository.existsByTmdbIdAndTmdbType(movie.id(), TmdbType.MOVIE)) {
                try {
                    persistenceService.persistMovie(movie);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to add new movie '{}': {}", movie.title(), e.getMessage());
                }
            }
        }
    }
    return count;
}
```

Similarly update discoverNewTvShows to use fetcher.

- [ ] **Step 4: Refactor updateExistingItem method**

Replace the current method that makes multiple API calls with:

```java
@Transactional
protected void updateExistingItem(ItemEntity item, Object details) {
    if (details instanceof TmdbMovieDetailsDto movieDetails) {
        TmdbItemMapper.updateFromMovie(item, movieDetails);
        if (movieDetails.genres() != null) {
            item.setGenres(persistenceService.mapGenres(movieDetails.genres()));
        }
        if (movieDetails.credits() != null) {
            item.setActorItems(persistenceService.mapActors(movieDetails.credits()));
            item.setDirectors(persistenceService.mapDirectors(movieDetails.credits()));
        }
    } else if (details instanceof TmdbTvShowDetailsDto tvDetails) {
        TmdbItemMapper.updateFromTvShow(item, tvDetails);
        if (tvDetails.genres() != null) {
            item.setGenres(persistenceService.mapGenres(tvDetails.genres()));
        }
        if (tvDetails.credits() != null) {
            item.setActorItems(persistenceService.mapActors(tvDetails.credits()));
            item.setDirectors(persistenceService.mapDirectors(tvDetails.credits()));
        }
    }

    itemRepository.save(item);
    log.debug("Updated item '{}' (tmdbId={})", item.getTitle(), item.getTmdbId());
}
```

- [ ] **Step 5: Run compile to verify**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS (may need fixes)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataSynchronizerService.java
git commit -m "refactor(tmdb): use TmdbDataFetcher in TmdbDataSynchronizerService"
```

---

## Task 5: Refactor TmdbDataCollectorService to use TmdbDataFetcher exclusively

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataCollectorService.java`

- [ ] **Step 1: Update imports**

Remove direct TmdbClient usage - keep the dataFetcher import.

Current:
```java
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
```

Remove this import since we don't need TmdbClient directly anymore.

- [ ] **Step 2: Update collectGenres method**

Replace:
```java
public void collectGenres() {
    log.info("Collecting genres...");

    CompletableFuture<TmdbGenreListDto> movieGenresFuture = dataFetcher.fetchGenresAsync(TmdbMediaType.MOVIE);
    CompletableFuture<TmdbGenreListDto> tvGenresFuture = dataFetcher.fetchGenresAsync(TmdbMediaType.TV);

    TmdbGenreListDto movieGenres = movieGenresFuture.join();
    TmdbGenreListDto tvGenres = tvGenresFuture.join();

    Set<Integer> seen = new HashSet<>();
    int count = 0;

    count += persistGenreList(movieGenres, seen);
    count += persistGenreList(tvGenres, seen);

    log.info("Genres collected: {} total", count);
}
```

- [ ] **Step 3: Update collectPopularMovies method**

Replace direct getPopular call with fetcher:
```java
TmdbPagedResponseDto<TmdbMovieDto> response = dataFetcher.fetchPopularMoviesAsync(page).join();
```

- [ ] **Step 4: Update collectPopularTvShows method**

Replace direct getPopular call with fetcher:
```java
TmdbPagedResponseDto<TmdbTvShowDto> response = dataFetcher.fetchPopularTvShowsAsync(page).join();
```

- [ ] **Step 5: Run compile to verify**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataCollectorService.java
git commit -m "refactor(tmdb): use TmdbDataFetcher exclusively in TmdbDataCollectorService"
```

---

## Task 6: Update tests and verify

**Files:**
- Modify: Test files that mock TmdbClient

- [ ] **Step 1: Find tests that need updates**

```bash
grep -l "tmdbClient\." --include="*Test.java" src/test/
```

- [ ] **Step 2: Update test mocks**

Update tests to mock TmdbDataFetcher instead of TmdbClient where needed.

- [ ] **Step 3: Run tests**

```bash
./mvnw test -q 2>&1 | head -50
```

Expected: Most tests pass (some pre-existing failures are OK)

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "test: update tests for refactored Tmdb services"
```
