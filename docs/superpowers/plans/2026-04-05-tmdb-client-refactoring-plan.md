# TmdbClient Refactoring Implementation Plan

> **For agentic workers:** Execute tasks one by one in this session.

**Goal:** Refactorizar TmdbClient para usar generics y eliminar código duplicado aplicando principios SOLID.

**Architecture:** Crear interfaces comunes (TmdbMediaItem, TmdbMediaDetails) y enum (TmdbMediaType) para unificar métodos de Movies y TV Shows.

**Tech Stack:** Java, Spring RestClient, Jackson

---

## Task 1: Create TmdbMediaType enum

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/persistence/tmdb/TmdbMediaType.java`

- [ ] **Step 1: Create TmdbMediaType enum**

```java
package org.tvl.tvlooker.persistence.tmdb;

public enum TmdbMediaType {
    MOVIE("movie"),
    TV("tv");

    private final String path;

    TmdbMediaType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getGenresEndpoint() {
        return path + "/genre/list";
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/persistence/tmdb/TmdbMediaType.java
git commit -m "feat(tmdb): add TmdbMediaType enum"
```

---

## Task 2: Create TmdbMediaItem interface

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/persistence/tmdb/dto/TmdbMediaItem.java`

- [ ] **Step 1: Create TmdbMediaItem interface**

```java
package org.tvl.tvlooker.persistence.tmdb.dto;

import java.util.List;

public interface TmdbMediaItem {
    long id();
    String title();
    String overview();
    String releaseDate();
    double popularity();
    double voteAverage();
    int voteCount();
    String posterPath();
    List<Integer> genreIds();
    List<TmdbGenreDto> genres();
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/persistence/tmdb/dto/TmdbMediaItem.java
git commit -m "feat(tmdb): add TmdbMediaItem interface"
```

---

## Task 3: Create TmdbMediaDetails interface

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/persistence/tmdb/dto/TmdbMediaDetails.java`

- [ ] **Step 1: Create TmdbMediaDetails interface**

```java
package org.tvl.tvlooker.persistence.tmdb.dto;

public interface TmdbMediaDetails extends TmdbMediaItem {
    TmdbCreditsDto credits();
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/persistence/tmdb/dto/TmdbMediaDetails.java
git commit -m "feat(tmdb): add TmdbMediaDetails interface"
```

---

## Task 4: Modify TmdbMovieDto to implement interfaces

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/persistence/tmdb/dto/TmdbMovieDto.java`

- [ ] **Step 1: Update TmdbMovieDto to implement TmdbMediaItem**

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovieDto(
        long id,
        String title,
        String overview,
        @JsonProperty("release_date") String releaseDate,
        double popularity,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("genre_ids") List<Integer> genreIds,
        List<TmdbGenreDto> genres
) implements TmdbMediaItem {}
```

- [ ] **Step 2: Verify TmdbMovieDetailsDto extends TmdbMovieDto**

Check if TmdbMovieDetailsDto already exists and extends TmdbMovieDto. If not, modify it.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/persistence/tmdb/dto/TmdbMovieDto.java
git commit -f "feat(tmdb): make TmdbMovieDto implement TmdbMediaItem"
```

---

## Task 5: Modify TmdbTvShowDto to implement interfaces

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/persistence/tmdb/dto/TmdbTvShowDto.java`

- [ ] **Step 1: Update TmdbTvShowDto to implement TmdbMediaItem with default methods**

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbTvShowDto(
        long id,
        String name,
        String overview,
        @JsonProperty("first_air_date") String firstAirDate,
        double popularity,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("genre_ids") List<Integer> genreIds,
        List<TmdbGenreDto> genres,
        TmdbCreditsDto credits
) implements TmdbMediaDetails {

    @Override
    public String title() {
        return name();
    }

    @Override
    public String releaseDate() {
        return firstAirDate();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/persistence/tmdb/dto/TmdbTvShowDto.java
git commit -f "feat(tmdb): make TmdbTvShowDto implement TmdbMediaDetails"
```

---

## Task 6: Add generic methods to TmdbClient

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/persistence/tmdb/TmdbClient.java`

- [ ] **Step 1: Add imports and new methods**

Add after existing imports:
```java
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaItem;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaDetails;
```

Add new generic methods after the existing methods:

```java
public <T extends TmdbMediaItem> TmdbPagedResponseDto<T> getPopular(
        TmdbMediaType type, int page) {
    LOGGER.debug("Fetching popular {} page {}", type, page);
    return restClient.get()
            .uri("/{type}/popular?language={lang}&page={page}",
                    type.getPath(), language, page)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
}

public <T extends TmdbMediaDetails> T getDetailsWithCredits(
        TmdbMediaType type, long id) {
    LOGGER.debug("Fetching {} details + credits for ID {}", type, id);
    return restClient.get()
            .uri("/{type}/{id}?language={lang}&append_to_response=credits",
                    type.getPath(), id, language)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
}

public TmdbPagedResponseDto<TmdbChangesDto> getChanges(
        TmdbMediaType type, LocalDate startDate, LocalDate endDate, int page) {
    LOGGER.debug("Fetching {} changes from {} to {}, page {}", 
            type, startDate, endDate, page);
    return restClient.get()
            .uri("/{type}/changes?start_date={start}&end_date={end}&page={page}",
                    type.getPath(), startDate, endDate, page)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
}

public TmdbGenreListDto getGenres(TmdbMediaType type) {
    LOGGER.debug("Fetching {} genres", type);
    return restClient.get()
            .uri("/{type}/genre/list?language={lang}", type.getPath(), language)
            .retrieve()
            .body(TmdbGenreListDto.class);
}
```

- [ ] **Step 2: Run build to verify compilation**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/persistence/tmdb/TmdbClient.java
git commit -m "feat(tmdb): add generic methods to TmdbClient"
```

---

## Task 7: Remove duplicate methods from TmdbClient

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/persistence/tmdb/TmdbClient.java`

- [ ] **Step 1: Remove duplicate methods**

Remove:
- `getPopularMovies(int)`
- `getPopularTvShows(int)`
- `getMovieDetailsWithCredits(long)`
- `getTvShowDetailsWithCredits(long)`
- `getMovieChanges(...)`
- `getTvShowChanges(...)`
- `getMovieGenres()`
- `getTvGenres()`

Keep only the @Deprecated methods that are already marked.

- [ ] **Step 2: Run build to verify**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS (any errors indicate callers that need updating)

- [ ] **Step 3: Run tests**

```bash
./mvnw test -q
```

Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
git add src/main/java/org/tvl/tvlooker/persistence/tmdb/TmdbClient.java
git commit -m "refactor(tmdb): remove duplicate methods, use generic methods"
```

---

## Task 8: Update callers to use new generic methods

**Files:**
- Find and modify: Files that call the removed methods

- [ ] **Step 1: Find all callers of removed methods**

```bash
grep -r "getPopularMovies\|getPopularTvShows\|getMovieDetailsWithCredits\|getTvShowDetailsWithCredits\|getMovieChanges\|getTvShowChanges\|getMovieGenres\|getTvGenres" --include="*.java"
```

- [ ] **Step 2: Update each caller**

Replace:
- `getPopularMovies(page)` → `getPopular(TmdbMediaType.MOVIE, page)`
- `getPopularTvShows(page)` → `getPopular(TmdbMediaType.TV, page)`
- `getMovieDetailsWithCredits(id)` → `getDetailsWithCredits(TmdbMediaType.MOVIE, id)`
- `getTvShowDetailsWithCredits(id)` → `getDetailsWithCredits(TmdbMediaType.TV, id)`
- `getMovieChanges(...)` → `getChanges(TmdbMediaType.MOVIE, ...)`
- `getTvShowChanges(...)` → `getChanges(TmdbMediaType.TV, ...)`
- `getMovieGenres()` → `getGenres(TmdbMediaType.MOVIE)`
- `getTvGenres()` → `getGenres(TmdbMediaType.TV)`

- [ ] **Step 3: Run tests**

```bash
./mvnw test -q
```

Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "refactor: update callers to use new TmdbClient generic methods"
```
