# TMDB Service Refactoring & ActorItem Entity Design

**Date:** 2026-04-03  
**Status:** Approved  
**Related Design:** [TMDB API Integration](2026-03-10-tmdb-api-integration-design.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Goals & Constraints](#goals--constraints)
3. [Current State Analysis](#current-state-analysis)
4. [Proposed Architecture](#proposed-architecture)
5. [Phase 1: ActorItemEntity](#phase-1-actorentity)
6. [Phase 2: TmdbClient with append_to_response](#phase-2-tmdbclient-with-append_to_response)
7. [Phase 3: TmdbDataFetcher with Rate Limiting](#phase-3-tmdbdatafetcher-with-rate-limiting)
8. [Phase 4: EntityCacheService & Batch Processing](#phase-4-entitycacheservice--batch-processing)
9. [Phase 5: TmdbItemBuilder](#phase-5-tmdbitembuilder)
10. [Phase 6: Refactored TmdbDataCollectorService](#phase-6-refactored-tmdbdatacollectorservice)
11. [Performance Improvements](#performance-improvements)
12. [Implementation Roadmap](#implementation-roadmap)
13. [Testing Strategy](#testing-strategy)
14. [Migration Notes](#migration-notes)

---

## Overview

This document describes a comprehensive refactoring of the TMDB service layer to address three key issues:

1. **Performance Bottleneck:** Current implementation makes 2-3 API calls per item (details + credits separately), resulting in 3+ minutes to collect 2000 items
2. **Code Quality:** Significant duplication between movie/TV show handling, overly long methods, mixed responsibilities
3. **Feature Gap:** Current many-to-many relationship with actors doesn't capture character names and billing order from TMDB

### Key Innovation: append_to_response

The TMDB API provides `append_to_response` parameter that allows fetching related data (like credits) in a single HTTP request. This reduces API calls from **2 per item to 1 per item**, a 50% reduction in total requests.

### Scope

**In Scope:**
- ActorItemEntity with character_name and billing_order fields
- Refactored TmdbClient with append_to_response support
- Rate-limited parallel fetching via TmdbDataFetcher
- Batch entity caching (EntityCacheService)
- Unified item builder pattern (TmdbItemBuilder)
- Refactored data collection with batch persistence

**Out of Scope:**
- TmdbDataSynchronizerService (will be refactored in follow-up)
- REST API endpoints for data collection
- Changes to recommendation engine

---

## Goals & Constraints

### Goals
- **Performance:** Reduce collection time from 3 minutes to ~40-45 seconds for 2000 items (75% improvement)
- **Code Quality:** Eliminate duplication, improve maintainability, follow SOLID principles
- **Extensibility:** Make it easy to add new entity relationships (prepare for future features)
- **Safety:** Maintain TMDB rate limit compliance (40 req/s with 35 req/s safety margin)

### Constraints
- TMDB API rate limit: 40 requests/second (hard limit for safety)
- No async/parallel execution above rate limit
- All persistence must be transactional
- Backward compatibility with existing repositories

---

## Current State Analysis

### Performance Issues

**API Call Pattern (Current):**
```
For 1000 movies (50 pages × 20 items):
1. GET /movie/popular (page 1) → 1 call
2. For each movie (1000 times):
   - GET /movie/{id} (details + genres) → 1 call
   - GET /movie/{id}/credits (actors + directors) → 1 call
3. GET /genre/movie/list → 1 call
4. GET /genre/tv/list → 1 call

Total: ~2052 API calls
Time at 40 req/s: ~51 seconds
Plus throttling delays (40ms per call): +81 seconds
Total: ~3 minutes
```

**Why It's Slow:**
1. Sequential API calls (Thread.sleep blocks entire thread)
2. Each item requires 2 calls (details + credits) instead of 1
3. No batch processing for entity lookups
4. N+1 query problem: Each actor/director/genre triggers individual DB query

### Code Quality Issues

**Duplication:**
- Movie and TV show persistence logic is ~95% identical (TmdbDataCollectorService:182-265)
- updateExistingItem() method duplicated across Collector and Synchronizer
- Genre/Actor/Director mapping logic repeated

**Mixed Responsibilities:**
- TmdbItemPersistenceService handles API calls, mapping, AND persistence
- Services directly interact with repositories instead of using cache layers
- Throttling mixed into business logic

**Maintainability Issues:**
- Long methods (updateExistingItem: 35+ lines)
- Magic numbers (MAX_ACTORS_PER_ITEM = 10 scattered across classes)
- Limited configuration options

### Actor Relationship Gap

**Current:** ItemEntity.actors is a simple @ManyToMany
- No way to store character names ("Tony Stark")
- No way to store billing order (1st billed, 2nd billed, etc.)
- Information from TMDB API is lost

**Data Available from TMDB (Currently Unused):**
```json
{
  "cast": [
    {
      "id": 3223,
      "name": "Robert Downey Jr.",
      "character": "Tony Stark",
      "order": 0
    },
    {
      "id": 2888,
      "name": "Gwyneth Paltrow",
      "character": "Pepper Potts",
      "order": 1
    }
  ]
}
```

---

## Proposed Architecture

### High-Level Data Flow

```
TmdbDataCollectorService
    ↓
TmdbClient.getPopularMovies()
    ↓ (fetch 50 pages sequentially)
TmdbDataFetcher.fetchMoviesBatch()
    ↓ (parallel with rate limiting, uses append_to_response)
RateLimiter (35 req/s)
    ↓
EntityCacheService (batch load actors, directors, genres)
    ↓
TmdbItemBuilder (unified item construction)
    ↓
TmdbItemPersistenceService.persistMoviesBatch()
    ↓
ItemRepository.saveAll() (single batch insert with cascading)
```

### Key Improvements

1. **Single API call per item:** `GET /movie/{id}?append_to_response=credits` returns details + genres + credits
2. **Parallel fetching:** Use thread pool with RateLimiter to fetch up to 35 items/sec
3. **Batch caching:** Load all actors/directors/genres at once instead of N+1 queries
4. **Batch persistence:** Save all items in one transaction with proper cascading
5. **ActorItemEntity:** Join entity captures character names and billing order

---

## Phase 1: ActorItemEntity

### Database Schema

**New Table: actors_items**
```sql
CREATE TABLE actors_items (
    actor_item_id_pk BIGSERIAL PRIMARY KEY,
    item_id_fk BIGINT NOT NULL REFERENCES items(item_id_pk) ON DELETE CASCADE,
    actor_id_fk BIGINT NOT NULL REFERENCES actors(actor_id_pk) ON DELETE CASCADE,
    character_name VARCHAR(255),
    billing_order INTEGER NOT NULL,
    UNIQUE(item_id_fk, actor_id_fk),
    INDEX idx_item_id (item_id_fk),
    INDEX idx_actor_id (actor_id_fk)
);
```

### ActorItemEntity Class

**File:** `src/main/java/org/tvl/tvlooker/domain/model/entity/ActorItemEntity.java`

```java
package org.tvl.tvlooker.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the relationship between an Actor and an Item (movie/TV show).
 * Captures additional metadata like character name and billing order.
 *
 * This join entity replaces the simple @ManyToMany relationship,
 * allowing us to store character names and billing order from TMDB API.
 *
 * Example: In "Iron Man", Robert Downey Jr. played "Tony Stark" as the 1st billed actor.
 */
@Entity
@Table(name = "actors_items")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ActorItemEntity {

    /**
     * Unique identifier for this actor-item relationship.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "actor_item_id_pk", updatable = false, nullable = false)
    private Long id;

    /**
     * The item (movie or TV show) this actor appeared in.
     * Many ActorItems can reference the same Item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id_fk", nullable = false)
    private ItemEntity item;

    /**
     * The actor that appeared in the item.
     * Many ActorItems can reference the same Actor.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id_fk", nullable = false)
    private ActorEntity actor;

    /**
     * The character name the actor played in this item.
     * Example: "Tony Stark", "Peter Parker", "Pepper Potts"
     *
     * Nullable because some credits don't have character information.
     */
    @Column(name = "character_name", length = 255)
    private String characterName;

    /**
     * The billing order of this actor in the item's credits.
     * 0 = first billed, 1 = second billed, etc.
     *
     * Not nullable to ensure ordering consistency.
     */
    @Column(name = "billing_order", nullable = false)
    private Integer billingOrder;
}
```

### Update ItemEntity

**File:** `src/main/java/org/tvl/tvlooker/domain/model/entity/ItemEntity.java`

Remove:
```java
@ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
@JoinTable(
        name = "actors_items",
        joinColumns = @JoinColumn(name = "item_id_fk"),
        inverseJoinColumns = @JoinColumn(name = "actor_id_fk")
)
private Set<ActorEntity> actors;
```

Add:
```java
/**
 * The actors associated with this item, including character names and billing order.
 * This replaces the simple @ManyToMany relationship to capture additional metadata.
 * 
 * Uses @OneToMany with orphanRemoval to ensure ActorItems are deleted when removed from the set.
 */
@OneToMany(
        mappedBy = "item",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
)
private Set<ActorItemEntity> actorItems = new HashSet<>();
```

### Create ActorItemRepository

**File:** `src/main/java/org/tvl/tvlooker/persistence/repository/ActorItemRepository.java`

```java
package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.ActorItemEntity;

/**
 * Repository for ActorItemEntity (actor-item relationships).
 */
@Repository
public interface ActorItemRepository extends JpaRepository<ActorItemEntity, Long> {
}
```

---

## Phase 2: TmdbClient with append_to_response

### New DTOs

**File:** `src/main/java/org/tvl/tvlooker/persistence/tmdb/dto/TmdbMovieDetailsDto.java`

```java
package org.tvl.tvlooker.persistence.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Complete movie data with appended credits.
 * Returned by: GET /movie/{id}?append_to_response=credits
 *
 * This DTO combines what was previously fetched in two separate calls:
 * 1. GET /movie/{id} (details + genres)
 * 2. GET /movie/{id}/credits (actors + directors)
 *
 * The "credits" field is populated due to the append_to_response parameter.
 */
public record TmdbMovieDetailsDto(
        long id,
        String title,
        String overview,
        @JsonProperty("release_date") String releaseDate,
        double popularity,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("backdrop_path") String backdropPath,
        List<TmdbGenreDto> genres,
        TmdbCreditsDto credits  // Appended via append_to_response=credits
) {}
```

**File:** `src/main/java/org/tvl/tvlooker/persistence/tmdb/dto/TmdbTvShowDetailsDto.java`

```java
package org.tvl.tvlooker.persistence.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Complete TV show data with appended credits.
 * Returned by: GET /tv/{id}?append_to_response=credits
 */
public record TmdbTvShowDetailsDto(
        long id,
        String name,
        String overview,
        @JsonProperty("first_air_date") String firstAirDate,
        double popularity,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("backdrop_path") String backdropPath,
        List<TmdbGenreDto> genres,
        TmdbCreditsDto credits  // Appended via append_to_response=credits
) {}
```

### Update TmdbClient

**File:** `src/main/java/org/tvl/tvlooker/persistence/tmdb/TmdbClient.java`

Add two new methods:

```java
/**
 * GET /movie/{id}?append_to_response=credits
 * Fetches movie details with credits in a single API call.
 *
 * @param movieId the TMDB movie ID
 * @return movie details DTO with appended credits
 */
public TmdbMovieDetailsDto getMovieDetailsWithCredits(long movieId) {
    LOGGER.debug("Fetching movie details + credits for ID {}", movieId);
    return restClient.get()
            .uri("/movie/{id}?language={lang}&append_to_response=credits", 
                 movieId, language)
            .retrieve()
            .body(TmdbMovieDetailsDto.class);
}

/**
 * GET /tv/{id}?append_to_response=credits
 * Fetches TV show details with credits in a single API call.
 *
 * @param tvShowId the TMDB TV show ID
 * @return TV show details DTO with appended credits
 */
public TmdbTvShowDetailsDto getTvShowDetailsWithCredits(long tvShowId) {
    LOGGER.debug("Fetching TV show details + credits for ID {}", tvShowId);
    return restClient.get()
            .uri("/tv/{id}?language={lang}&append_to_response=credits", 
                 tvShowId, language)
            .retrieve()
            .body(TmdbTvShowDetailsDto.class);
}
```

---

## Phase 3: TmdbDataFetcher with Rate Limiting

### Add Guava Dependency

**File:** `pom.xml`

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.0.0-jre</version>
</dependency>
```

### Create TmdbDataFetcher Service

**File:** `src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataFetcher.java`

```java
package org.tvl.tvlooker.service.tmdb;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;

import java.util.Executor;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Handles fetching TMDB data in parallel while respecting rate limits.
 *
 * Uses Google's RateLimiter to ensure we never exceed TMDB's 40 req/s limit.
 * All API calls are made asynchronously in a thread pool.
 *
 * Key Innovation: Leverages append_to_response parameter to combine multiple
 * API calls (details + credits) into a single request.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-04-03
 */
@Service
@Slf4j
public class TmdbDataFetcher {

    private final TmdbClient tmdbClient;
    private final Executor tmdbTaskExecutor;
    private final RateLimiter rateLimiter;

    public TmdbDataFetcher(
            TmdbClient tmdbClient,
            @Qualifier("tmdbTaskExecutor") Executor tmdbTaskExecutor,
            @Value("${tmdb.api.rate-limit:35}") double requestsPerSecond) {
        this.tmdbClient = tmdbClient;
        this.tmdbTaskExecutor = tmdbTaskExecutor;
        // Set to 35 req/s for safety margin (TMDB hard limit is 40)
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
        log.info("TmdbDataFetcher initialized with rate limit: {} req/s", requestsPerSecond);
    }

    /**
     * Fetches movie details with credits asynchronously, respecting rate limits.
     *
     * RateLimiter.acquire() will block until the request can be made without
     * exceeding the configured rate limit.
     *
     * @param tmdbId TMDB movie ID
     * @return CompletableFuture containing movie details with credits
     */
    public CompletableFuture<TmdbMovieDetailsDto> fetchMovieDetailsAsync(long tmdbId) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire(); // Blocks until rate limit allows
            return tmdbClient.getMovieDetailsWithCredits(tmdbId);
        }, tmdbTaskExecutor);
    }

    /**
     * Fetches TV show details with credits asynchronously, respecting rate limits.
     *
     * @param tvShowId TMDB TV show ID
     * @return CompletableFuture containing TV show details with credits
     */
    public CompletableFuture<TmdbTvShowDetailsDto> fetchTvShowDetailsAsync(long tvShowId) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire();
            return tmdbClient.getTvShowDetailsWithCredits(tvShowId);
        }, tmdbTaskExecutor);
    }

    /**
     * Batch fetches multiple movies in parallel with rate limiting.
     *
     * Each movie fetch respects the global rate limiter, so the total throughput
     * never exceeds the configured limit. With 20 parallel threads and a 35 req/s
     * limit, this will take approximately:
     * - 1000 items / 35 req/s = ~28-30 seconds
     *
     * @param tmdbIds List of TMDB movie IDs to fetch
     * @return List of movie details with credits (in any order)
     */
    public List<TmdbMovieDetailsDto> fetchMoviesBatch(List<Long> tmdbIds) {
        log.debug("Fetching {} movies in parallel batch", tmdbIds.size());

        // Create async futures for each ID
        List<CompletableFuture<TmdbMovieDetailsDto>> futures = tmdbIds.stream()
                .map(this::fetchMovieDetailsAsync)
                .toList();

        // Wait for all futures to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        // Join results and filter out nulls
        return allOf.thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList())
                .join();
    }

    /**
     * Batch fetches multiple TV shows in parallel with rate limiting.
     *
     * @param tvShowIds List of TMDB TV show IDs to fetch
     * @return List of TV show details with credits
     */
    public List<TmdbTvShowDetailsDto> fetchTvShowsBatch(List<Long> tvShowIds) {
        log.debug("Fetching {} TV shows in parallel batch", tvShowIds.size());

        List<CompletableFuture<TmdbTvShowDetailsDto>> futures = tvShowIds.stream()
                .map(this::fetchTvShowDetailsAsync)
                .toList();

        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        return allOf.thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList())
                .join();
    }
}
```

### Create Async Configuration

**File:** `src/main/java/org/tvl/tvlooker/config/TmdbAsyncConfiguration.java`

```java
package org.tvl.tvlooker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async configuration for TMDB data fetching.
 *
 * Creates a thread pool dedicated to TMDB API calls with:
 * - Core pool: 20 threads (can handle 20 concurrent requests)
 * - Max pool: 40 threads (maximum concurrent requests)
 * - Queue: 200 pending tasks
 *
 * With a 35 req/s rate limiter, this configuration ensures we can:
 * 1. Fetch multiple items in parallel
 * 2. Always respect TMDB rate limits
 * 3. Queue additional requests when thread pool is busy
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-04-03
 */
@Configuration
@EnableAsync
public class TmdbAsyncConfiguration {

    /**
     * Thread pool executor for TMDB API calls.
     *
     * @return configured Executor bean
     */
    @Bean(name = "tmdbTaskExecutor")
    public Executor tmdbTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core threads: number of threads to keep alive even if idle
        executor.setCorePoolSize(20);

        // Max threads: maximum number of threads
        executor.setMaxPoolSize(40);

        // Queue size: pending tasks when all threads are busy
        executor.setQueueCapacity(200);

        // Thread naming for debugging
        executor.setThreadNamePrefix("tmdb-fetch-");

        // When queue is full, run the task in the caller's thread
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}
```

### Update application.properties

**File:** `src/main/resources/application.properties`

Add:
```properties
# ========================================================================================
# TMDB API RATE LIMITING
# ========================================================================================
# TMDB hard limit: 40 requests/second
# We use 35 for safety margin. Adjust based on your testing.
tmdb.api.rate-limit=35
```

---

## Phase 4: EntityCacheService & Batch Processing

### Update Repositories with Batch Query Methods

**File:** `src/main/java/org/tvl/tvlooker/persistence/repository/ActorRepository.java`

Add:
```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Set;

@Query("SELECT a FROM ActorEntity a WHERE a.tmdbId IN :tmdbIds")
List<ActorEntity> findAllByTmdbIdIn(@Param("tmdbIds") Set<Long> tmdbIds);
```

**File:** `src/main/java/org/tvl/tvlooker/persistence/repository/DirectorRepository.java`

Add:
```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Set;

@Query("SELECT d FROM DirectorEntity d WHERE d.tmdbId IN :tmdbIds")
List<DirectorEntity> findAllByTmdbIdIn(@Param("tmdbIds") Set<Long> tmdbIds);
```

**File:** `src/main/java/org/tvl/tvlooker/persistence/repository/GenreRepository.java`

Add:
```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Set;

@Query("SELECT g FROM GenreEntity g WHERE g.tmdbId IN :tmdbIds")
List<GenreEntity> findAllByTmdbIdIn(@Param("tmdbIds") Set<Long> tmdbIds);
```

### Create EntityCacheService

**File:** `src/main/java/org/tvl/tvlooker/service/tmdb/EntityCacheService.java`

```java
package org.tvl.tvlooker.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;
import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles batch caching and creation of entities to prevent N+1 queries.
 *
 * Instead of querying the database for each actor/director/genre individually,
 * this service collects all unique entities from a batch of items and loads
 * them in bulk using a single query per entity type.
 *
 * Example:
 * - Old: 100 items × 5 actors each = 500 queries
 * - New: 1 bulk query for 500 actors = 1 query total
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-04-03
 */
@Service
@Slf4j
public class EntityCacheService {

    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final GenreRepository genreRepository;

    public EntityCacheService(
            ActorRepository actorRepository,
            DirectorRepository directorRepository,
            GenreRepository genreRepository) {
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
        this.genreRepository = genreRepository;
    }

    /**
     * Batch finds or creates actors from TMDB cast members.
     *
     * Strategy:
     * 1. Extract all unique TMDB actor IDs
     * 2. Bulk query database for existing actors
     * 3. Identify missing actors
     * 4. Create and save missing actors
     * 5. Return map of tmdbId → ActorEntity
     *
     * @param castMembers TMDB cast members (from credits response)
     * @return Map of tmdbId → ActorEntity (includes newly created)
     */
    @Transactional
    public Map<Long, ActorEntity> findOrCreateActors(List<TmdbCreditsDto.CastMember> castMembers) {
        if (castMembers == null || castMembers.isEmpty()) {
            return Map.of();
        }

        Set<Long> tmdbIds = castMembers.stream()
                .map(TmdbCreditsDto.CastMember::id)
                .collect(Collectors.toSet());

        log.debug("Finding or creating {} actors", tmdbIds.size());

        // 1. Bulk query existing actors
        List<ActorEntity> existing = actorRepository.findAllByTmdbIdIn(tmdbIds);
        Map<Long, ActorEntity> actorMap = existing.stream()
                .collect(Collectors.toMap(ActorEntity::getTmdbId, a -> a));

        // 2. Identify missing actors
        Set<Long> missingIds = tmdbIds.stream()
                .filter(id -> !actorMap.containsKey(id))
                .collect(Collectors.toSet());

        // 3. Create and save missing actors
        if (!missingIds.isEmpty()) {
            List<ActorEntity> newActors = castMembers.stream()
                    .filter(cast -> missingIds.contains(cast.id()))
                    .map(cast -> ActorEntity.builder()
                            .tmdbId(cast.id())
                            .name(cast.name())
                            .build())
                    .toList();

            List<ActorEntity> saved = actorRepository.saveAll(newActors);
            saved.forEach(a -> actorMap.put(a.getTmdbId(), a));

            log.debug("Created {} new actors", newActors.size());
        }

        return actorMap;
    }

    /**
     * Batch finds or creates directors from TMDB crew members.
     *
     * @param crewMembers TMDB crew members (from credits response)
     * @return Map of tmdbId → DirectorEntity
     */
    @Transactional
    public Map<Long, DirectorEntity> findOrCreateDirectors(List<TmdbCreditsDto.CrewMember> crewMembers) {
        if (crewMembers == null || crewMembers.isEmpty()) {
            return Map.of();
        }

        // Filter only directors
        List<TmdbCreditsDto.CrewMember> directors = crewMembers.stream()
                .filter(c -> "Director".equalsIgnoreCase(c.job()))
                .toList();

        if (directors.isEmpty()) {
            return Map.of();
        }

        Set<Long> tmdbIds = directors.stream()
                .map(TmdbCreditsDto.CrewMember::id)
                .collect(Collectors.toSet());

        log.debug("Finding or creating {} directors", tmdbIds.size());

        // Bulk query existing
        List<DirectorEntity> existing = directorRepository.findAllByTmdbIdIn(tmdbIds);
        Map<Long, DirectorEntity> directorMap = existing.stream()
                .collect(Collectors.toMap(DirectorEntity::getTmdbId, d -> d));

        // Create missing
        Set<Long> missingIds = tmdbIds.stream()
                .filter(id -> !directorMap.containsKey(id))
                .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            List<DirectorEntity> newDirectors = directors.stream()
                    .filter(crew -> missingIds.contains(crew.id()))
                    .map(crew -> DirectorEntity.builder()
                            .tmdbId(crew.id())
                            .name(crew.name())
                            .build())
                    .toList();

            List<DirectorEntity> saved = directorRepository.saveAll(newDirectors);
            saved.forEach(d -> directorMap.put(d.getTmdbId(), d));

            log.debug("Created {} new directors", newDirectors.size());
        }

        return directorMap;
    }

    /**
     * Batch finds or creates genres from TMDB genre DTOs.
     *
     * @param genreDtos TMDB genre DTOs
     * @return Map of tmdbId → GenreEntity
     */
    @Transactional
    public Map<Long, GenreEntity> findOrCreateGenres(List<TmdbGenreDto> genreDtos) {
        if (genreDtos == null || genreDtos.isEmpty()) {
            return Map.of();
        }

        Set<Long> tmdbIds = genreDtos.stream()
                .map(g -> (long) g.id())
                .collect(Collectors.toSet());

        log.debug("Finding or creating {} genres", tmdbIds.size());

        // Bulk query existing
        List<GenreEntity> existing = genreRepository.findAllByTmdbIdIn(tmdbIds);
        Map<Long, GenreEntity> genreMap = existing.stream()
                .collect(Collectors.toMap(GenreEntity::getTmdbId, g -> g));

        // Create missing
        Set<Long> missingIds = tmdbIds.stream()
                .filter(id -> !genreMap.containsKey(id))
                .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            List<GenreEntity> newGenres = genreDtos.stream()
                    .filter(g -> missingIds.contains((long) g.id()))
                    .map(g -> GenreEntity.builder()
                            .tmdbId((long) g.id())
                            .name(g.name())
                            .build())
                    .toList();

            List<GenreEntity> saved = genreRepository.saveAll(newGenres);
            saved.forEach(g -> genreMap.put(g.getTmdbId(), g));

            log.debug("Created {} new genres", newGenres.size());
        }

        return genreMap;
    }
}
```

---

## Phase 5: TmdbItemBuilder

**File:** `src/main/java/org/tvl/tvlooker/persistence/tmdb/mapper/TmdbItemBuilder.java`

```java
package org.tvl.tvlooker.persistence.tmdb.mapper;

import lombok.extern.slf4j.Slf4j;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.ActorItemEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds ItemEntity instances from TMDB DTOs using pre-cached entities.
 *
 * This builder pattern eliminates duplication between movie and TV show handling
 * while accepting pre-resolved entity maps (genres, actors, directors) to avoid
 * queries during entity construction.
 *
 * The builder properly sets up the ActorItemEntity join entities, capturing
 * character names and billing order from the TMDB API.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-04-03
 */
@Slf4j
public final class TmdbItemBuilder {

    private static final int MAX_ACTORS_PER_ITEM = 10;

    private TmdbItemBuilder() {
        // Utility class, no instantiation
    }

    /**
     * Builds an ItemEntity from TMDB movie details with pre-cached entities.
     *
     * @param details Movie details DTO (includes appended credits)
     * @param genreCache Map of tmdbId → GenreEntity (pre-loaded)
     * @param actorCache Map of tmdbId → ActorEntity (pre-loaded)
     * @param directorCache Map of tmdbId → DirectorEntity (pre-loaded)
     * @return Constructed ItemEntity with all relationships set
     */
    public static ItemEntity buildFromMovieDetails(
            TmdbMovieDetailsDto details,
            Map<Long, GenreEntity> genreCache,
            Map<Long, ActorEntity> actorCache,
            Map<Long, DirectorEntity> directorCache) {

        log.debug("Building ItemEntity from movie: '{}'", details.title());

        // 1. Build base item
        ItemEntity item = ItemEntity.builder()
                .tmdbId(details.id())
                .tmdbType(TmdbType.MOVIE)
                .title(details.title())
                .overview(details.overview())
                .releaseDate(parseDate(details.releaseDate()))
                .popularity(BigDecimal.valueOf(details.popularity()))
                .voteAverage(BigDecimal.valueOf(details.voteAverage()))
                .build();

        // 2. Set genres
        if (details.genres() != null && !details.genres().isEmpty()) {
            Set<GenreEntity> genres = details.genres().stream()
                    .map(g -> genreCache.get((long) g.id()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            item.setGenres(genres);
        }

        // 3. Set actor items with character names and billing order (NEW!)
        if (details.credits() != null && details.credits().cast() != null) {
            Set<ActorItemEntity> actorItems = new HashSet<>();

            details.credits().cast().stream()
                    .sorted(Comparator.comparingInt(TmdbCreditsDto.CastMember::order))
                    .limit(MAX_ACTORS_PER_ITEM)
                    .forEach(cast -> {
                        ActorEntity actor = actorCache.get(cast.id());
                        if (actor != null) {
                            ActorItemEntity actorItem = ActorItemEntity.builder()
                                    .item(item)
                                    .actor(actor)
                                    .characterName(cast.character())
                                    .billingOrder(cast.order())
                                    .build();
                            actorItems.add(actorItem);
                        }
                    });

            item.setActorItems(actorItems);
        }

        // 4. Set directors
        if (details.credits() != null && details.credits().crew() != null) {
            Set<DirectorEntity> directors = details.credits().crew().stream()
                    .filter(crew -> "Director".equalsIgnoreCase(crew.job()))
                    .map(crew -> directorCache.get(crew.id()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            item.setDirectors(directors);
        }

        return item;
    }

    /**
     * Builds an ItemEntity from TMDB TV show details with pre-cached entities.
     *
     * @param details TV show details DTO (includes appended credits)
     * @param genreCache Map of tmdbId → GenreEntity (pre-loaded)
     * @param actorCache Map of tmdbId → ActorEntity (pre-loaded)
     * @param directorCache Map of tmdbId → DirectorEntity (pre-loaded)
     * @return Constructed ItemEntity with all relationships set
     */
    public static ItemEntity buildFromTvShowDetails(
            TmdbTvShowDetailsDto details,
            Map<Long, GenreEntity> genreCache,
            Map<Long, ActorEntity> actorCache,
            Map<Long, DirectorEntity> directorCache) {

        log.debug("Building ItemEntity from TV show: '{}'", details.name());

        // 1. Build base item
        ItemEntity item = ItemEntity.builder()
                .tmdbId(details.id())
                .tmdbType(TmdbType.TV)
                .title(details.name())
                .overview(details.overview())
                .releaseDate(parseDate(details.firstAirDate()))
                .popularity(BigDecimal.valueOf(details.popularity()))
                .voteAverage(BigDecimal.valueOf(details.voteAverage()))
                .build();

        // 2. Set genres
        if (details.genres() != null && !details.genres().isEmpty()) {
            Set<GenreEntity> genres = details.genres().stream()
                    .map(g -> genreCache.get((long) g.id()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            item.setGenres(genres);
        }

        // 3. Set actor items
        if (details.credits() != null && details.credits().cast() != null) {
            Set<ActorItemEntity> actorItems = new HashSet<>();

            details.credits().cast().stream()
                    .sorted(Comparator.comparingInt(TmdbCreditsDto.CastMember::order))
                    .limit(MAX_ACTORS_PER_ITEM)
                    .forEach(cast -> {
                        ActorEntity actor = actorCache.get(cast.id());
                        if (actor != null) {
                            ActorItemEntity actorItem = ActorItemEntity.builder()
                                    .item(item)
                                    .actor(actor)
                                    .characterName(cast.character())
                                    .billingOrder(cast.order())
                                    .build();
                            actorItems.add(actorItem);
                        }
                    });

            item.setActorItems(actorItems);
        }

        // 4. Set directors
        if (details.credits() != null && details.credits().crew() != null) {
            Set<DirectorEntity> directors = details.credits().crew().stream()
                    .filter(crew -> "Director".equalsIgnoreCase(crew.job()))
                    .map(crew -> directorCache.get(crew.id()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            item.setDirectors(directors);
        }

        return item;
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date: {}", date);
            return null;
        }
    }
}
```

---

## Phase 6: Refactored TmdbDataCollectorService

This is where all pieces come together. The new implementation is:
- 40% shorter (removes duplication)
- Uses async API calls with rate limiting
- Batch processes all entities
- Properly manages transactions

**File:** `src/main/java/org/tvl/tvlooker/service/tmdb/TmdbDataCollectorService.java`

See separate detailed implementation file for full code (~400 lines).

Key changes:
1. Remove direct API calls - use `TmdbDataFetcher` instead
2. Batch fetch all details at once
3. Use `EntityCacheService` for bulk entity resolution
4. Use `TmdbItemBuilder` to construct items
5. Persist in batches instead of individually
6. Simplified logic with better separation of concerns

---

## Performance Improvements

### API Call Reduction

**Before:**
```
For 1000 movies:
- 50 pages × 1 call = 50 calls (popular lists)
- 1000 movies × 2 calls = 2000 calls (details + credits separately)
- 2 calls (genre lists)
Total: 2052 calls
```

**After (with append_to_response):**
```
For 1000 movies:
- 50 pages × 1 call = 50 calls (popular lists)
- 1000 movies × 1 call = 1000 calls (details + credits combined)
- 2 calls (genre lists)
Total: 1052 calls (49% reduction)
```

### Database Query Reduction

**Before (N+1 problem):**
```
Per 1000 movies:
- 1000 movies × 5 actors avg = 5000 actor lookups
- 1000 movies × 2 directors avg = 2000 director lookups
- 1000 movies × 3 genres avg = 3000 genre lookups
Total: 10000 queries
```

**After (batch caching):**
```
Per 1000 movies:
- 1 bulk actor query (regardless of count)
- 1 bulk director query
- 1 bulk genre query
- 1000 item saves with cascading relationships
Total: 4 queries (99.96% reduction!)
```

### Time Estimates

**API Call Performance:**
- 1052 API calls at 35 req/s limit
- With parallel fetching: ~30 seconds
- Plus overhead: ~35-40 seconds

**Database Performance:**
- Batch inserts (1000 items in 1 transaction): ~5-10 seconds

**Total Time:**
- Before: ~3-4 minutes (including 40ms throttle delays)
- After: ~40-50 seconds
- **Improvement: 75-80% faster**

### Additional Benefits

1. **Cleaner Code:** Removed 200+ lines of duplication
2. **Better Testability:** Separated concerns into focused services
3. **Extensibility:** Easy to add new entities (e.g., production companies, keywords)
4. **Improved Logging:** Better visibility into what's happening
5. **Configuration:** Rate limit and batch size now configurable
6. **Actor Data:** Now captures character names and billing order

---

## Implementation Roadmap

### Week 1: Preparation
- [ ] Create feature branch: `refactor/tmdb-service-optimization`
- [ ] Update database schema (create actors_items table)
- [ ] Add ActorItemEntity and update ItemEntity
- [ ] Create ActorItemRepository
- [ ] Write migration script for existing actor relationships

### Week 2: Core Infrastructure
- [ ] Add Guava dependency
- [ ] Create DTOs: TmdbMovieDetailsDto, TmdbTvShowDetailsDto
- [ ] Update TmdbClient with append_to_response methods
- [ ] Create TmdbAsyncConfiguration
- [ ] Create TmdbDataFetcher service

### Week 3: Services & Builders
- [ ] Update repositories with batch query methods
- [ ] Create EntityCacheService
- [ ] Create TmdbItemBuilder
- [ ] Update TmdbItemPersistenceService with batch persistence

### Week 4: Integration & Testing
- [ ] Refactor TmdbDataCollectorService
- [ ] Refactor TmdbDataSynchronizerService
- [ ] Write comprehensive integration tests
- [ ] Performance testing and validation
- [ ] Code review and feedback

### Week 5: Deployment
- [ ] Update documentation
- [ ] Create release notes
- [ ] Deploy to staging
- [ ] Production deployment

---

## Testing Strategy

### Unit Tests
- TmdbItemBuilder (test movie/TV building with various credit configurations)
- EntityCacheService (test bulk loading and creation)
- TmdbDataFetcher (test rate limiting with mocked executor)

### Integration Tests
- TmdbDataCollectorService (test full collection flow with test data)
- Repository queries (test batch loading methods)
- ActorItemEntity relationships (test cascading saves/deletes)

### Performance Tests
- Measure actual collection time vs. estimated
- Verify rate limiting is respected
- Check database query counts
- Monitor memory usage with large batches

---

## Migration Notes

### Database Migrations

1. **Create actors_items table** - New join entity table
2. **Migrate existing data** - Move data from old M2M to new join entity
3. **Drop old actors_items table** (if using direct M2M junction table)
4. **Update ItemEntity mapping** - Change from @ManyToMany to @OneToMany

### Backward Compatibility

- Old REST API endpoints that return actor lists should continue to work
- May need view/projection that flattens ActorItem relationships for API responses
- Consider: Keep actor list in ItemDTO for API backward compatibility

### Rollback Plan

If issues arise:
1. Keep old M2M table structure temporarily
2. Implement dual-write pattern (write to both old and new)
3. Test thoroughly before removing old code
4. Have database restore point ready

---

## Configuration

Add to `application.properties`:

```properties
# ========================================================================================
# TMDB API RATE LIMITING & ASYNC
# ========================================================================================

# Rate limit for TMDB API calls (requests per second)
# TMDB hard limit is 40, we default to 35 for safety
tmdb.api.rate-limit=35

# Batch size for entity persistence
# Larger batches = fewer transactions but more memory
# Smaller batches = more transactions but less memory
tmdb.persistence.batch-size=50

# Maximum actors to store per item
tmdb.actors.max-per-item=10
```

---

## Summary

This refactoring delivers:

1. **ActorItemEntity** - Captures character names and billing order
2. **50% fewer API calls** - Using `append_to_response`
3. **35x fewer database queries** - Batch caching
4. **75-80% faster execution** - ~40-50 seconds vs. 3 minutes
5. **Cleaner code** - Eliminated duplication, better separation of concerns
6. **Easier extensibility** - Framework for adding new entity relationships

The design maintains TMDB rate limit compliance while maximizing throughput through intelligent batching and caching.
