# TMDB API Integration — Data Collector & Synchronizer Design

**Date:** 2026-03-10  
**Status:** Proposed  
**Related Design:** [Recommendation Engine Architecture](2026-03-05-recommendation-engine-design.md) | [Strategies & Aggregations](2026-03-06-recommendation-strategies-and-aggregations-design.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Decision](#architecture-decision)
3. [Package Structure](#package-structure)
4. [Configuration & RestClient Setup](#configuration--restclient-setup)
5. [TMDB DTO Classes](#tmdb-dto-classes)
6. [Mapper Layer](#mapper-layer)
7. [Spring Data JPA Repositories](#spring-data-jpa-repositories)
8. [TmdbClient — API Client](#tmdbclient--api-client)
9. [TmdbDataCollector — Bulk Load](#tmdbdatacollector--bulk-load)
10. [TmdbDataSynchronizer — Scheduled Sync](#tmdbdatasynchronizer--scheduled-sync)
11. [Error Handling & Resilience](#error-handling--resilience)
12. [Schema Changes Required](#schema-changes-required)
13. [Testing Strategy](#testing-strategy)
14. [Implementation Roadmap](#implementation-roadmap)
15. [Constraints & Assumptions](#constraints--assumptions)

---

## Overview

This document describes the design for integrating the TMDB (The Movie Database) API into TV Looker. The integration consists of two core components:

1. **TmdbDataCollector** — A class that fetches all necessary information from the TMDB API to populate the database with movies, TV shows, genres, actors, and directors.
2. **TmdbDataSynchronizer** — A scheduled class that periodically syncs changed or new information from the TMDB API, keeping the local database up to date.

### Goals

- **Complete Data Population:** Fetch all data that the recommendation engine strategies need (popularity, vote average, genres, actors, directors)
- **Incremental Sync:** Efficiently update only changed data using TMDB's changes endpoints
- **Non-Blocking Updates:** Updates must not interfere with user operations (no long locks, per-item transactions)
- **Rate-Limit Awareness:** Respect TMDB's rate limits (~40 requests/second)
- **Idempotent Operations:** Re-running the collector should not create duplicates
- **Production Ready:** Comprehensive error handling, logging, and configuration

### Scope

**Includes:**
- TMDB API client with RestClient
- DTO classes for TMDB JSON responses
- Mapper classes (TMDB DTO → JPA Entity)
- Spring Data JPA repositories for Item, Genre, Actor, Director
- Collector class for initial bulk data load
- Synchronizer class for periodic incremental updates
- Configuration and error handling

**Does NOT include:**
- Insertion of data into the recommendation engine
- Custom database queries for the recommendation engine's DataProviders
- REST API endpoints for the application
- User-facing features

---

## Architecture Decision

### Data Flow Architecture

```
TMDB API (External)
    ↓  HTTP (RestClient)
TmdbClient (API abstraction layer)
    ↓  TMDB DTOs (Java records)
Mapper Layer (DTO → Entity conversion)
    ↓  JPA Entities
Repositories (Spring Data JPA)
    ↓  SQL
PostgreSQL Database
```

### Why This Layered Approach?

1. **TmdbClient** isolates all HTTP communication. If TMDB changes their API, only this class changes.
2. **DTOs as Java records** provide clean deserialization without coupling to JPA entities.
3. **Mappers** handle the complex logic of find-or-create (e.g., if an actor already exists, reuse it).
4. **Repositories** provide standard JPA access with custom queries by `tmdbId`.
5. **Collector and Synchronizer** are orchestrators that coordinate the flow, each with distinct triggers (manual vs. scheduled).

---

## Package Structure

```
org.tvl.tvlooker
├── config/
│   ├── RecommendationConfig.java          (existing)
│   └── TmdbConfig.java                    (NEW - RestClient bean, @EnableScheduling)
│
├── domain/
│   ├── model/
│   │   ├── entity/                        (existing entities)
│   │   └── enums/                         (existing enums)
│   ├── motor/                             (existing recommendation engine)
│   ├── strategy/                          (existing strategies)
│   ├── data_structure/                    (existing)
│   └── exception/                         (existing + new TMDB exceptions)
│
├── infrastructure/
│   └── tmdb/
│       ├── TmdbClient.java               (NEW - RestClient wrapper for TMDB API)
│       ├── dto/                           (NEW - TMDB response DTOs)
│       │   ├── TmdbMovieDto.java
│       │   ├── TmdbTvShowDto.java
│       │   ├── TmdbCreditsDto.java
│       │   ├── TmdbGenreListDto.java
│       │   ├── TmdbChangesDto.java
│       │   └── TmdbPagedResponseDto.java
│       └── mapper/                        (NEW - DTO → Entity mappers)
│           ├── TmdbItemMapper.java
│           ├── TmdbGenreMapper.java
│           └── TmdbPersonMapper.java
│
├── repository/                            (NEW - Spring Data JPA repositories)
│   ├── ItemRepository.java
│   ├── GenreRepository.java
│   ├── ActorRepository.java
│   └── DirectorRepository.java
│
└── service/
    └── tmdb/                              (NEW - Business logic orchestrators)
        ├── TmdbDataCollector.java
        └── TmdbDataSynchronizer.java
```

---

## Configuration & RestClient Setup

### Application Properties

```properties
# ========================================================================================
# TMDB API CONFIGURATION
# ========================================================================================

# TMDB API Authentication (API Read Access Token v4)
# Get yours at: https://www.themoviedb.org/settings/api
tmdb.api.key=${TMDB_API_KEY:your-api-read-access-token-here}
tmdb.api.base-url=https://api.themoviedb.org/3

# Language for TMDB responses
tmdb.api.language=es-MX

# Collector Configuration
# Maximum number of pages to fetch per category (20 items/page, max 500)
tmdb.collector.max-pages=50
# Whether to run the collector on application startup
tmdb.collector.run-on-startup=false
# Delay between API calls in milliseconds (40ms = ~25 req/s, safely under 40 req/s limit)
tmdb.collector.request-delay-ms=40

# Synchronizer Configuration
# Sync interval in milliseconds (default: 24 hours)
tmdb.sync.interval-ms=86400000
# Whether the synchronizer is enabled
tmdb.sync.enabled=true
# Maximum number of pages to fetch for new popular items during sync
tmdb.sync.popular-pages=5
```

### TmdbConfig Class

```java
package org.tvl.tvlooker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
public class TmdbConfig {

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.base-url:https://api.themoviedb.org/3}")
    private String baseUrl;

    @Bean
    public RestClient tmdbRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
```

**Key Decisions:**
- The API key is injected via environment variable `TMDB_API_KEY` for security (no hardcoded keys)
- RestClient (Spring Boot 4.x synchronous HTTP client) is used instead of WebClient since the collector/synchronizer are background tasks that don't need reactive processing
- `@EnableScheduling` enables Spring's task scheduling for the synchronizer
- Language defaults to `es-MX` but is configurable

---

## TMDB DTO Classes

All DTOs are **Java records** (immutable, concise) for clean deserialization. They live in `infrastructure.tmdb.dto` and are completely separate from JPA entities.

### TmdbPagedResponseDto

```java
package org.tvl.tvlooker.infrastructure.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Generic paginated response from TMDB API.
 * Used for /movie/popular, /tv/popular, /movie/changes, etc.
 */
public record TmdbPagedResponseDto<T>(
        int page,
        List<T> results,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("total_results") int totalResults
) {}
```

### TmdbMovieDto

```java
package org.tvl.tvlooker.infrastructure.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a movie from the TMDB API.
 * Used for both /movie/popular list results and /movie/{id} detail responses.
 */
public record TmdbMovieDto(
        long id,
        String title,
        String overview,
        @JsonProperty("release_date") String releaseDate,
        double popularity,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("backdrop_path") String backdropPath,
        @JsonProperty("genre_ids") List<Integer> genreIds,
        List<TmdbGenreDto> genres
) {}
```

### TmdbTvShowDto

```java
package org.tvl.tvlooker.infrastructure.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a TV show from the TMDB API.
 * Used for both /tv/popular list results and /tv/{id} detail responses.
 */
public record TmdbTvShowDto(
        long id,
        String name,
        String overview,
        @JsonProperty("first_air_date") String firstAirDate,
        double popularity,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("backdrop_path") String backdropPath,
        @JsonProperty("genre_ids") List<Integer> genreIds,
        List<TmdbGenreDto> genres
) {}
```

### TmdbCreditsDto

```java
package org.tvl.tvlooker.infrastructure.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Credits (cast and crew) for a movie or TV show from TMDB API.
 * GET /movie/{id}/credits or /tv/{id}/credits
 */
public record TmdbCreditsDto(
        long id,
        List<CastMember> cast,
        List<CrewMember> crew
) {
    /**
     * Represents an actor in the cast.
     */
    public record CastMember(
            long id,
            String name,
            String character,
            @JsonProperty("known_for_department") String knownForDepartment,
            @JsonProperty("profile_path") String profilePath,
            int order
    ) {}

    /**
     * Represents a crew member (filter by job="Director" for directors).
     */
    public record CrewMember(
            long id,
            String name,
            String department,
            String job,
            @JsonProperty("profile_path") String profilePath
    ) {}
}
```

### TmdbGenreListDto & TmdbGenreDto

```java
package org.tvl.tvlooker.infrastructure.tmdb.dto;

import java.util.List;

/**
 * Response from GET /genre/movie/list or /genre/tv/list.
 */
public record TmdbGenreListDto(
        List<TmdbGenreDto> genres
) {}
```

```java
package org.tvl.tvlooker.infrastructure.tmdb.dto;

/**
 * A single genre from TMDB.
 */
public record TmdbGenreDto(
        int id,
        String name
) {}
```

### TmdbChangesDto

```java
package org.tvl.tvlooker.infrastructure.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single changed item from GET /movie/changes or /tv/changes.
 */
public record TmdbChangesDto(
        long id,
        @JsonProperty("adult") Boolean adult
) {}
```

---

## Mapper Layer

Mappers convert TMDB DTOs to JPA entities. The critical responsibility is **find-or-create logic** — if an actor with a given `tmdbId` already exists in the DB, reuse it instead of creating a duplicate.

### TmdbItemMapper

```java
package org.tvl.tvlooker.infrastructure.tmdb.mapper;

import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbTvShowDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Maps TMDB movie/TV DTOs to Item JPA entities.
 * Does NOT set genres, actors, or directors — those are handled by their respective mappers.
 */
public class TmdbItemMapper {

    /**
     * Maps a TMDB movie DTO to a new Item entity.
     */
    public static Item fromMovie(TmdbMovieDto dto) {
        return Item.builder()
                .tmdbId(dto.id())
                .tmdbType(TmdbType.MOVIE)
                .title(dto.title())
                .overview(dto.overview())
                .releaseDate(parseDate(dto.releaseDate()))
                .popularity(BigDecimal.valueOf(dto.popularity()))
                .voteAverage(BigDecimal.valueOf(dto.voteAverage()))
                .build();
    }

    /**
     * Maps a TMDB TV show DTO to a new Item entity.
     */
    public static Item fromTvShow(TmdbTvShowDto dto) {
        return Item.builder()
                .tmdbId(dto.id())
                .tmdbType(TmdbType.TV)
                .title(dto.name())
                .overview(dto.overview())
                .releaseDate(parseDate(dto.firstAirDate()))
                .popularity(BigDecimal.valueOf(dto.popularity()))
                .voteAverage(BigDecimal.valueOf(dto.voteAverage()))
                .build();
    }

    /**
     * Updates mutable fields on an existing Item entity with fresh TMDB data.
     * Used by the synchronizer to update popularity, vote average, etc.
     */
    public static void updateItemFromMovie(Item existing, TmdbMovieDto dto) {
        existing.setTitle(dto.title());
        existing.setOverview(dto.overview());
        existing.setPopularity(BigDecimal.valueOf(dto.popularity()));
        existing.setVoteAverage(BigDecimal.valueOf(dto.voteAverage()));
        existing.setReleaseDate(parseDate(dto.releaseDate()));
    }

    /**
     * Updates mutable fields on an existing Item entity with fresh TMDB TV data.
     */
    public static void updateItemFromTvShow(Item existing, TmdbTvShowDto dto) {
        existing.setTitle(dto.name());
        existing.setOverview(dto.overview());
        existing.setPopularity(BigDecimal.valueOf(dto.popularity()));
        existing.setVoteAverage(BigDecimal.valueOf(dto.voteAverage()));
        existing.setReleaseDate(parseDate(dto.firstAirDate()));
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
```

### TmdbGenreMapper

```java
package org.tvl.tvlooker.infrastructure.tmdb.mapper;

import org.tvl.tvlooker.domain.model.entity.Genre;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.repository.GenreRepository;

/**
 * Maps TMDB genre DTOs to Genre JPA entities.
 * Uses find-or-create pattern: if genre with tmdbId already exists, reuse it.
 */
public class TmdbGenreMapper {

    /**
     * Finds an existing Genre by tmdbId or creates a new one.
     */
    public static Genre findOrCreate(TmdbGenreDto dto, GenreRepository repository) {
        return repository.findByTmdbId((long) dto.id())
                .orElseGet(() -> {
                    Genre genre = new Genre();
                    genre.setTmdbId((long) dto.id());
                    genre.setName(dto.name());
                    return repository.save(genre);
                });
    }
}
```

### TmdbPersonMapper

```java
package org.tvl.tvlooker.infrastructure.tmdb.mapper;

import org.tvl.tvlooker.domain.model.entity.Actor;
import org.tvl.tvlooker.domain.model.entity.Director;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.repository.ActorRepository;
import org.tvl.tvlooker.repository.DirectorRepository;

/**
 * Maps TMDB cast/crew members to Actor and Director JPA entities.
 * Uses find-or-create pattern by tmdbId.
 */
public class TmdbPersonMapper {

    /**
     * Finds an existing Actor by tmdbId or creates a new one.
     */
    public static Actor findOrCreateActor(
            TmdbCreditsDto.CastMember castMember,
            ActorRepository repository) {
        return repository.findByTmdbId(castMember.id())
                .orElseGet(() -> {
                    Actor actor = new Actor();
                    actor.setTmdbId(castMember.id());
                    actor.setName(castMember.name());
                    return repository.save(actor);
                });
    }

    /**
     * Finds an existing Director by tmdbId or creates a new one.
     * Only crew members with job="Director" should be passed here.
     */
    public static Director findOrCreateDirector(
            TmdbCreditsDto.CrewMember crewMember,
            DirectorRepository repository) {
        return repository.findByTmdbId(crewMember.id())
                .orElseGet(() -> {
                    Director director = new Director();
                    director.setTmdbId(crewMember.id());
                    director.setName(crewMember.name());
                    return repository.save(director);
                });
    }
}
```

---

## Spring Data JPA Repositories

These are the first repositories in the project. They provide the data access layer needed by the collector and synchronizer.

### ItemRepository

```java
package org.tvl.tvlooker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Finds an item by its TMDB ID and type (MOVIE or TV).
     * Used to check if an item already exists before inserting.
     */
    Optional<Item> findByTmdbIdAndTmdbType(Long tmdbId, TmdbType tmdbType);

    /**
     * Checks if an item exists by its TMDB ID and type.
     * More efficient than findBy when we only need existence check.
     */
    boolean existsByTmdbIdAndTmdbType(Long tmdbId, TmdbType tmdbType);
}
```

### GenreRepository

```java
package org.tvl.tvlooker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.Genre;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    /**
     * Finds a genre by its TMDB ID.
     * Used for find-or-create pattern when mapping TMDB genres.
     */
    Optional<Genre> findByTmdbId(Long tmdbId);
}
```

### ActorRepository

```java
package org.tvl.tvlooker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.Actor;

import java.util.Optional;

@Repository
public interface ActorRepository extends JpaRepository<Actor, Long> {

    /**
     * Finds an actor by their TMDB ID.
     * Used for find-or-create pattern when mapping TMDB cast.
     */
    Optional<Actor> findByTmdbId(Long tmdbId);
}
```

### DirectorRepository

```java
package org.tvl.tvlooker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.Director;

import java.util.Optional;

@Repository
public interface DirectorRepository extends JpaRepository<Director, Long> {

    /**
     * Finds a director by their TMDB ID.
     * Used for find-or-create pattern when mapping TMDB crew.
     */
    Optional<Director> findByTmdbId(Long tmdbId);
}
```

---

## TmdbClient — API Client

The `TmdbClient` encapsulates all HTTP communication with the TMDB API. No other class in the application should directly call the TMDB API.

```java
package org.tvl.tvlooker.infrastructure.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbTvShowDto;

import java.time.LocalDate;

/**
 * Client for the TMDB API v3.
 * Encapsulates all HTTP communication with the TMDB API.
 * 
 * Rate Limit: TMDB allows ~40 requests/second.
 * This client does NOT handle rate limiting — that responsibility belongs to the caller
 * (TmdbDataCollector and TmdbDataSynchronizer) which add delays between calls.
 */
@Component
public class TmdbClient {

    private static final Logger logger = LoggerFactory.getLogger(TmdbClient.class);

    private final RestClient restClient;
    private final String language;

    public TmdbClient(
            RestClient tmdbRestClient,
            @Value("${tmdb.api.language:es-MX}") String language) {
        this.restClient = tmdbRestClient;
        this.language = language;
    }

    // ===================== MOVIES =====================

    /**
     * GET /movie/popular — Paginated list of popular movies.
     * @param page Page number (1-based, max 500)
     */
    public TmdbPagedResponseDto<TmdbMovieDto> getPopularMovies(int page) {
        logger.debug("Fetching popular movies page {}", page);
        return restClient.get()
                .uri("/movie/popular?language={lang}&page={page}", language, page)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /**
     * GET /movie/{id} — Full details for a movie.
     */
    public TmdbMovieDto getMovieDetails(long movieId) {
        logger.debug("Fetching movie details for ID {}", movieId);
        return restClient.get()
                .uri("/movie/{id}?language={lang}", movieId, language)
                .retrieve()
                .body(TmdbMovieDto.class);
    }

    /**
     * GET /movie/{id}/credits — Cast and crew for a movie.
     */
    public TmdbCreditsDto getMovieCredits(long movieId) {
        logger.debug("Fetching movie credits for ID {}", movieId);
        return restClient.get()
                .uri("/movie/{id}/credits?language={lang}", movieId, language)
                .retrieve()
                .body(TmdbCreditsDto.class);
    }

    /**
     * GET /movie/changes — IDs of movies that changed in a date range.
     * Used for incremental synchronization.
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param page Page number
     */
    public TmdbPagedResponseDto<TmdbChangesDto> getMovieChanges(
            LocalDate startDate, LocalDate endDate, int page) {
        logger.debug("Fetching movie changes from {} to {}, page {}", startDate, endDate, page);
        return restClient.get()
                .uri("/movie/changes?start_date={start}&end_date={end}&page={page}",
                        startDate, endDate, page)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    // ===================== TV SHOWS =====================

    /**
     * GET /tv/popular — Paginated list of popular TV shows.
     * @param page Page number (1-based, max 500)
     */
    public TmdbPagedResponseDto<TmdbTvShowDto> getPopularTvShows(int page) {
        logger.debug("Fetching popular TV shows page {}", page);
        return restClient.get()
                .uri("/tv/popular?language={lang}&page={page}", language, page)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /**
     * GET /tv/{id} — Full details for a TV show.
     */
    public TmdbTvShowDto getTvShowDetails(long tvShowId) {
        logger.debug("Fetching TV show details for ID {}", tvShowId);
        return restClient.get()
                .uri("/tv/{id}?language={lang}", tvShowId, language)
                .retrieve()
                .body(TmdbTvShowDto.class);
    }

    /**
     * GET /tv/{id}/credits — Cast and crew for a TV show.
     */
    public TmdbCreditsDto getTvShowCredits(long tvShowId) {
        logger.debug("Fetching TV show credits for ID {}", tvShowId);
        return restClient.get()
                .uri("/tv/{id}/credits?language={lang}", tvShowId, language)
                .retrieve()
                .body(TmdbCreditsDto.class);
    }

    /**
     * GET /tv/changes — IDs of TV shows that changed in a date range.
     */
    public TmdbPagedResponseDto<TmdbChangesDto> getTvShowChanges(
            LocalDate startDate, LocalDate endDate, int page) {
        logger.debug("Fetching TV show changes from {} to {}, page {}", startDate, endDate, page);
        return restClient.get()
                .uri("/tv/changes?start_date={start}&end_date={end}&page={page}",
                        startDate, endDate, page)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    // ===================== GENRES =====================

    /**
     * GET /genre/movie/list — All movie genres.
     */
    public TmdbGenreListDto getMovieGenres() {
        logger.debug("Fetching movie genre list");
        return restClient.get()
                .uri("/genre/movie/list?language={lang}", language)
                .retrieve()
                .body(TmdbGenreListDto.class);
    }

    /**
     * GET /genre/tv/list — All TV genres.
     */
    public TmdbGenreListDto getTvGenres() {
        logger.debug("Fetching TV genre list");
        return restClient.get()
                .uri("/genre/tv/list?language={lang}", language)
                .retrieve()
                .body(TmdbGenreListDto.class);
    }
}
```

---

## TmdbDataCollector — Bulk Load

The collector is responsible for the **initial population** of the database with TMDB data. It is designed to be run once (or on-demand) and is idempotent.

### Trigger Mechanism

The collector can be triggered in two ways:
1. **On application startup** — via an `ApplicationRunner` bean when `tmdb.collector.run-on-startup=true`
2. **Manually** — via a future admin endpoint (out of scope for this issue)

### Implementation

```java
package org.tvl.tvlooker.service.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.Actor;
import org.tvl.tvlooker.domain.model.entity.Director;
import org.tvl.tvlooker.domain.model.entity.Genre;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.infrastructure.tmdb.TmdbClient;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.infrastructure.tmdb.mapper.TmdbGenreMapper;
import org.tvl.tvlooker.infrastructure.tmdb.mapper.TmdbItemMapper;
import org.tvl.tvlooker.infrastructure.tmdb.mapper.TmdbPersonMapper;
import org.tvl.tvlooker.repository.ActorRepository;
import org.tvl.tvlooker.repository.DirectorRepository;
import org.tvl.tvlooker.repository.GenreRepository;
import org.tvl.tvlooker.repository.ItemRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects and persists data from the TMDB API into the local database.
 * 
 * Responsible for the initial bulk load of:
 * - Genres (from /genre/movie/list and /genre/tv/list)
 * - Popular movies (from /movie/popular, paginated)
 * - Popular TV shows (from /tv/popular, paginated)
 * - Credits for each item (actors and directors from /movie/{id}/credits and /tv/{id}/credits)
 * 
 * Design Principles:
 * - Idempotent: Re-running does not create duplicates (checks by tmdbId)
 * - Per-item transactions: One item failing does not abort the entire batch
 * - Rate-limit aware: Adds configurable delay between API calls
 * - Comprehensive logging: Logs progress, skips, and errors
 */
@Service
@ConditionalOnProperty(name = "tmdb.collector.run-on-startup", havingValue = "true")
public class TmdbDataCollector implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TmdbDataCollector.class);

    private final TmdbClient tmdbClient;
    private final ItemRepository itemRepository;
    private final GenreRepository genreRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;

    @Value("${tmdb.collector.max-pages:50}")
    private int maxPages;

    @Value("${tmdb.collector.request-delay-ms:40}")
    private long requestDelayMs;

    /** Maximum number of actors to store per item (top billed) */
    private static final int MAX_ACTORS_PER_ITEM = 10;

    public TmdbDataCollector(
            TmdbClient tmdbClient,
            ItemRepository itemRepository,
            GenreRepository genreRepository,
            ActorRepository actorRepository,
            DirectorRepository directorRepository) {
        this.tmdbClient = tmdbClient;
        this.itemRepository = itemRepository;
        this.genreRepository = genreRepository;
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("========== TMDB DATA COLLECTION STARTED ==========");
        collectAll();
        logger.info("========== TMDB DATA COLLECTION FINISHED ==========");
    }

    /**
     * Main entry point: orchestrates the full data collection.
     */
    public void collectAll() {
        collectGenres();
        collectPopularMovies();
        collectPopularTvShows();
    }

    /**
     * Fetches and persists all genres from TMDB (both movie and TV genres).
     */
    public void collectGenres() {
        logger.info("Collecting genres...");

        TmdbGenreListDto movieGenres = tmdbClient.getMovieGenres();
        TmdbGenreListDto tvGenres = tmdbClient.getTvGenres();
        throttle();

        int count = 0;
        // Combine movie and TV genres (some overlap, handled by find-or-create)
        Set<TmdbGenreDto> allGenres = new HashSet<>();
        if (movieGenres != null && movieGenres.genres() != null) {
            allGenres.addAll(movieGenres.genres());
        }
        if (tvGenres != null && tvGenres.genres() != null) {
            allGenres.addAll(tvGenres.genres());
        }

        for (TmdbGenreDto genreDto : allGenres) {
            TmdbGenreMapper.findOrCreate(genreDto, genreRepository);
            count++;
        }

        logger.info("Genres collected: {} total", count);
    }

    /**
     * Fetches popular movies from TMDB, page by page, and persists each one.
     */
    public void collectPopularMovies() {
        logger.info("Collecting popular movies (max {} pages)...", maxPages);

        int totalCollected = 0;
        int totalSkipped = 0;

        for (int page = 1; page <= maxPages; page++) {
            TmdbPagedResponseDto<TmdbMovieDto> response = tmdbClient.getPopularMovies(page);
            throttle();

            if (response == null || response.results() == null || response.results().isEmpty()) {
                logger.info("No more movies at page {}", page);
                break;
            }

            for (TmdbMovieDto movieDto : response.results()) {
                try {
                    if (itemRepository.existsByTmdbIdAndTmdbType(movieDto.id(), TmdbType.MOVIE)) {
                        totalSkipped++;
                        continue;
                    }
                    persistMovie(movieDto);
                    totalCollected++;
                } catch (Exception e) {
                    logger.warn("Failed to persist movie '{}' (tmdbId={}): {}",
                            movieDto.title(), movieDto.id(), e.getMessage());
                }
            }

            // Stop if we've reached the last available page
            if (page >= response.totalPages()) {
                break;
            }

            logger.info("Movies progress: page {}/{}, collected={}, skipped={}",
                    page, Math.min(maxPages, response.totalPages()), totalCollected, totalSkipped);
        }

        logger.info("Popular movies collection complete: {} collected, {} skipped", totalCollected, totalSkipped);
    }

    /**
     * Fetches popular TV shows from TMDB, page by page, and persists each one.
     */
    public void collectPopularTvShows() {
        logger.info("Collecting popular TV shows (max {} pages)...", maxPages);

        int totalCollected = 0;
        int totalSkipped = 0;

        for (int page = 1; page <= maxPages; page++) {
            TmdbPagedResponseDto<TmdbTvShowDto> response = tmdbClient.getPopularTvShows(page);
            throttle();

            if (response == null || response.results() == null || response.results().isEmpty()) {
                logger.info("No more TV shows at page {}", page);
                break;
            }

            for (TmdbTvShowDto tvDto : response.results()) {
                try {
                    if (itemRepository.existsByTmdbIdAndTmdbType(tvDto.id(), TmdbType.TV)) {
                        totalSkipped++;
                        continue;
                    }
                    persistTvShow(tvDto);
                    totalCollected++;
                } catch (Exception e) {
                    logger.warn("Failed to persist TV show '{}' (tmdbId={}): {}",
                            tvDto.name(), tvDto.id(), e.getMessage());
                }
            }

            if (page >= response.totalPages()) {
                break;
            }

            logger.info("TV shows progress: page {}/{}, collected={}, skipped={}",
                    page, Math.min(maxPages, response.totalPages()), totalCollected, totalSkipped);
        }

        logger.info("Popular TV shows collection complete: {} collected, {} skipped", totalCollected, totalSkipped);
    }

    // ===================== PRIVATE HELPERS =====================

    /**
     * Persists a single movie with its genres, actors, and directors.
     * Each movie is persisted in its own transaction.
     */
    @Transactional
    protected void persistMovie(TmdbMovieDto movieDto) {
        // 1. Map base item
        Item item = TmdbItemMapper.fromMovie(movieDto);

        // 2. Fetch and map genres
        TmdbMovieDto details = tmdbClient.getMovieDetails(movieDto.id());
        throttle();

        if (details != null && details.genres() != null) {
            Set<Genre> genres = new HashSet<>();
            for (TmdbGenreDto genreDto : details.genres()) {
                genres.add(TmdbGenreMapper.findOrCreate(genreDto, genreRepository));
            }
            item.setGenres(genres);
        }

        // 3. Fetch and map credits
        TmdbCreditsDto credits = tmdbClient.getMovieCredits(movieDto.id());
        throttle();

        if (credits != null) {
            item.setActors(mapActors(credits));
            item.setDirectors(mapDirectors(credits));
        }

        // 4. Save item
        itemRepository.save(item);

        logger.debug("Persisted movie: '{}' (tmdbId={})", movieDto.title(), movieDto.id());
    }

    /**
     * Persists a single TV show with its genres, actors, and directors.
     */
    @Transactional
    protected void persistTvShow(TmdbTvShowDto tvDto) {
        // 1. Map base item
        Item item = TmdbItemMapper.fromTvShow(tvDto);

        // 2. Fetch and map genres
        TmdbTvShowDto details = tmdbClient.getTvShowDetails(tvDto.id());
        throttle();

        if (details != null && details.genres() != null) {
            Set<Genre> genres = new HashSet<>();
            for (TmdbGenreDto genreDto : details.genres()) {
                genres.add(TmdbGenreMapper.findOrCreate(genreDto, genreRepository));
            }
            item.setGenres(genres);
        }

        // 3. Fetch and map credits
        TmdbCreditsDto credits = tmdbClient.getTvShowCredits(tvDto.id());
        throttle();

        if (credits != null) {
            item.setActors(mapActors(credits));
            item.setDirectors(mapDirectors(credits));
        }

        // 4. Save item
        itemRepository.save(item);

        logger.debug("Persisted TV show: '{}' (tmdbId={})", tvDto.name(), tvDto.id());
    }

    /**
     * Maps cast members to Actor entities (top billed only).
     */
    private Set<Actor> mapActors(TmdbCreditsDto credits) {
        Set<Actor> actors = new HashSet<>();
        if (credits.cast() != null) {
            List<TmdbCreditsDto.CastMember> topActors = credits.cast().stream()
                    .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                    .limit(MAX_ACTORS_PER_ITEM)
                    .toList();

            for (TmdbCreditsDto.CastMember castMember : topActors) {
                actors.add(TmdbPersonMapper.findOrCreateActor(castMember, actorRepository));
            }
        }
        return actors;
    }

    /**
     * Maps crew members with job="Director" to Director entities.
     */
    private Set<Director> mapDirectors(TmdbCreditsDto credits) {
        Set<Director> directors = new HashSet<>();
        if (credits.crew() != null) {
            List<TmdbCreditsDto.CrewMember> directorCrew = credits.crew().stream()
                    .filter(c -> "Director".equalsIgnoreCase(c.job()))
                    .toList();

            for (TmdbCreditsDto.CrewMember crewMember : directorCrew) {
                directors.add(TmdbPersonMapper.findOrCreateDirector(crewMember, directorRepository));
            }
        }
        return directors;
    }

    /**
     * Adds a delay between API calls to respect TMDB rate limits.
     */
    private void throttle() {
        try {
            Thread.sleep(requestDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Throttle sleep interrupted");
        }
    }
}
```

### API Calls per Item

For each movie or TV show, the collector makes **3 API calls**:
1. (Already part of the paginated list) — details included in list response
2. `GET /movie/{id}` or `GET /tv/{id}` — to get full genre objects (list only returns `genre_ids[]`)
3. `GET /movie/{id}/credits` or `GET /tv/{id}/credits` — to get cast and crew

With 50 pages × 20 items = 1000 items, plus genre calls, that's approximately:
- 50 (popular list pages) + 1000 × 2 (details + credits) + 2 (genre lists) = **~2052 API calls** per type
- At ~25 req/s with 40ms delay = **~82 seconds per type** (movies + TV)
- Total bulk load time: **~3 minutes** for 2000 items

---

## TmdbDataSynchronizer — Scheduled Sync

The synchronizer runs periodically to keep the local database in sync with TMDB. It uses two strategies:

1. **Changes endpoint** — TMDB's `/movie/changes` and `/tv/changes` return IDs of items that changed recently. The synchronizer re-fetches details for items we already have in our DB.
2. **New popular items** — Fetches the first few pages of popular movies/TV to discover new additions.

### Implementation

```java
package org.tvl.tvlooker.service.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.Actor;
import org.tvl.tvlooker.domain.model.entity.Director;
import org.tvl.tvlooker.domain.model.entity.Genre;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.infrastructure.tmdb.TmdbClient;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.infrastructure.tmdb.mapper.TmdbGenreMapper;
import org.tvl.tvlooker.infrastructure.tmdb.mapper.TmdbItemMapper;
import org.tvl.tvlooker.infrastructure.tmdb.mapper.TmdbPersonMapper;
import org.tvl.tvlooker.repository.ActorRepository;
import org.tvl.tvlooker.repository.DirectorRepository;
import org.tvl.tvlooker.repository.GenreRepository;
import org.tvl.tvlooker.repository.ItemRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Periodically synchronizes TMDB data with the local database.
 *
 * Sync Strategy:
 * 1. Use TMDB's /changes endpoints to discover items that changed since last sync
 * 2. Re-fetch details + credits for changed items that exist in our DB
 * 3. Also fetch first pages of popular movies/TV to discover new items
 *
 * Design Principles:
 * - Non-blocking: Uses per-item transactions so users are never affected
 * - Error isolation: One item failing does not abort the batch
 * - Configurable interval: Defaults to every 24 hours
 * - Rate-limit aware: Adds delays between API calls
 */
@Service
@ConditionalOnProperty(name = "tmdb.sync.enabled", havingValue = "true", matchIfMissing = true)
public class TmdbDataSynchronizer {

    private static final Logger logger = LoggerFactory.getLogger(TmdbDataSynchronizer.class);

    private final TmdbClient tmdbClient;
    private final ItemRepository itemRepository;
    private final GenreRepository genreRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;

    @Value("${tmdb.collector.request-delay-ms:40}")
    private long requestDelayMs;

    @Value("${tmdb.sync.popular-pages:5}")
    private int popularPages;

    /** Maximum number of actors to store per item (top billed) */
    private static final int MAX_ACTORS_PER_ITEM = 10;

    /** Last sync date — tracks when the last successful sync completed */
    private LocalDate lastSyncDate = LocalDate.now().minusDays(1);

    public TmdbDataSynchronizer(
            TmdbClient tmdbClient,
            ItemRepository itemRepository,
            GenreRepository genreRepository,
            ActorRepository actorRepository,
            DirectorRepository directorRepository) {
        this.tmdbClient = tmdbClient;
        this.itemRepository = itemRepository;
        this.genreRepository = genreRepository;
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
    }

    /**
     * Main scheduled sync method.
     * Runs at a fixed interval (default: every 24 hours).
     */
    @Scheduled(fixedDelayString = "${tmdb.sync.interval-ms:86400000}",
               initialDelayString = "${tmdb.sync.initial-delay-ms:60000}")
    public void synchronize() {
        logger.info("========== TMDB SYNC STARTED (changes since {}) ==========", lastSyncDate);

        LocalDate today = LocalDate.now();

        try {
            int updatedMovies = syncChanges(TmdbType.MOVIE, lastSyncDate, today);
            int updatedTvShows = syncChanges(TmdbType.TV, lastSyncDate, today);
            int newMovies = discoverNewPopularItems(TmdbType.MOVIE);
            int newTvShows = discoverNewPopularItems(TmdbType.TV);

            lastSyncDate = today;

            logger.info("========== TMDB SYNC COMPLETED ==========");
            logger.info("Updated: {} movies, {} TV shows", updatedMovies, updatedTvShows);
            logger.info("New: {} movies, {} TV shows", newMovies, newTvShows);
        } catch (Exception e) {
            logger.error("TMDB sync failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Uses TMDB's /changes endpoint to find and update items that changed since the last sync.
     * Only updates items that already exist in our database.
     *
     * @param type MOVIE or TV
     * @param startDate Start of change window
     * @param endDate End of change window
     * @return Number of items successfully updated
     */
    private int syncChanges(TmdbType type, LocalDate startDate, LocalDate endDate) {
        logger.info("Syncing {} changes from {} to {}", type, startDate, endDate);

        int updatedCount = 0;
        int page = 1;
        int totalPages = 1;

        while (page <= totalPages) {
            TmdbPagedResponseDto<TmdbChangesDto> changes;

            if (type == TmdbType.MOVIE) {
                changes = tmdbClient.getMovieChanges(startDate, endDate, page);
            } else {
                changes = tmdbClient.getTvShowChanges(startDate, endDate, page);
            }
            throttle();

            if (changes == null || changes.results() == null) {
                break;
            }

            totalPages = changes.totalPages();

            for (TmdbChangesDto change : changes.results()) {
                try {
                    // Only update items we already have in our DB
                    Optional<Item> existingItem = itemRepository.findByTmdbIdAndTmdbType(
                            change.id(), type);

                    if (existingItem.isPresent()) {
                        updateExistingItem(existingItem.get(), type);
                        updatedCount++;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to sync {} (tmdbId={}): {}",
                            type, change.id(), e.getMessage());
                }
            }

            page++;
        }

        logger.info("Synced {} {} updates", updatedCount, type);
        return updatedCount;
    }

    /**
     * Fetches the first N pages of popular movies/TV to discover new items
     * that were not in our database yet.
     *
     * @param type MOVIE or TV
     * @return Number of new items added
     */
    private int discoverNewPopularItems(TmdbType type) {
        logger.info("Discovering new popular {} (first {} pages)", type, popularPages);

        int newCount = 0;

        for (int page = 1; page <= popularPages; page++) {
            try {
                if (type == TmdbType.MOVIE) {
                    TmdbPagedResponseDto<TmdbMovieDto> response = tmdbClient.getPopularMovies(page);
                    throttle();

                    if (response != null && response.results() != null) {
                        for (TmdbMovieDto movieDto : response.results()) {
                            if (!itemRepository.existsByTmdbIdAndTmdbType(movieDto.id(), TmdbType.MOVIE)) {
                                persistNewMovie(movieDto);
                                newCount++;
                            }
                        }
                    }
                } else {
                    TmdbPagedResponseDto<TmdbTvShowDto> response = tmdbClient.getPopularTvShows(page);
                    throttle();

                    if (response != null && response.results() != null) {
                        for (TmdbTvShowDto tvDto : response.results()) {
                            if (!itemRepository.existsByTmdbIdAndTmdbType(tvDto.id(), TmdbType.TV)) {
                                persistNewTvShow(tvDto);
                                newCount++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error discovering new {} at page {}: {}", type, page, e.getMessage());
            }
        }

        return newCount;
    }

    // ===================== PRIVATE HELPERS =====================

    /**
     * Re-fetches details and credits from TMDB and updates an existing item.
     * Each update runs in its own transaction.
     */
    @Transactional
    protected void updateExistingItem(Item item, TmdbType type) {
        if (type == TmdbType.MOVIE) {
            TmdbMovieDto details = tmdbClient.getMovieDetails(item.getTmdbId());
            throttle();

            if (details != null) {
                TmdbItemMapper.updateItemFromMovie(item, details);

                // Update genres
                if (details.genres() != null) {
                    Set<Genre> genres = new HashSet<>();
                    for (TmdbGenreDto g : details.genres()) {
                        genres.add(TmdbGenreMapper.findOrCreate(g, genreRepository));
                    }
                    item.setGenres(genres);
                }
            }

            TmdbCreditsDto credits = tmdbClient.getMovieCredits(item.getTmdbId());
            throttle();

            if (credits != null) {
                item.setActors(mapActors(credits));
                item.setDirectors(mapDirectors(credits));
            }
        } else {
            TmdbTvShowDto details = tmdbClient.getTvShowDetails(item.getTmdbId());
            throttle();

            if (details != null) {
                TmdbItemMapper.updateItemFromTvShow(item, details);

                if (details.genres() != null) {
                    Set<Genre> genres = new HashSet<>();
                    for (TmdbGenreDto g : details.genres()) {
                        genres.add(TmdbGenreMapper.findOrCreate(g, genreRepository));
                    }
                    item.setGenres(genres);
                }
            }

            TmdbCreditsDto credits = tmdbClient.getTvShowCredits(item.getTmdbId());
            throttle();

            if (credits != null) {
                item.setActors(mapActors(credits));
                item.setDirectors(mapDirectors(credits));
            }
        }

        itemRepository.save(item);
        logger.debug("Updated {} '{}' (tmdbId={})", type, item.getTitle(), item.getTmdbId());
    }

    /**
     * Persists a new movie discovered during sync.
     */
    @Transactional
    protected void persistNewMovie(TmdbMovieDto movieDto) {
        Item item = TmdbItemMapper.fromMovie(movieDto);

        TmdbMovieDto details = tmdbClient.getMovieDetails(movieDto.id());
        throttle();
        if (details != null && details.genres() != null) {
            Set<Genre> genres = new HashSet<>();
            for (TmdbGenreDto g : details.genres()) {
                genres.add(TmdbGenreMapper.findOrCreate(g, genreRepository));
            }
            item.setGenres(genres);
        }

        TmdbCreditsDto credits = tmdbClient.getMovieCredits(movieDto.id());
        throttle();
        if (credits != null) {
            item.setActors(mapActors(credits));
            item.setDirectors(mapDirectors(credits));
        }

        itemRepository.save(item);
        logger.debug("Added new movie: '{}' (tmdbId={})", movieDto.title(), movieDto.id());
    }

    /**
     * Persists a new TV show discovered during sync.
     */
    @Transactional
    protected void persistNewTvShow(TmdbTvShowDto tvDto) {
        Item item = TmdbItemMapper.fromTvShow(tvDto);

        TmdbTvShowDto details = tmdbClient.getTvShowDetails(tvDto.id());
        throttle();
        if (details != null && details.genres() != null) {
            Set<Genre> genres = new HashSet<>();
            for (TmdbGenreDto g : details.genres()) {
                genres.add(TmdbGenreMapper.findOrCreate(g, genreRepository));
            }
            item.setGenres(genres);
        }

        TmdbCreditsDto credits = tmdbClient.getTvShowCredits(tvDto.id());
        throttle();
        if (credits != null) {
            item.setActors(mapActors(credits));
            item.setDirectors(mapDirectors(credits));
        }

        itemRepository.save(item);
        logger.debug("Added new TV show: '{}' (tmdbId={})", tvDto.name(), tvDto.id());
    }

    private Set<Actor> mapActors(TmdbCreditsDto credits) {
        Set<Actor> actors = new HashSet<>();
        if (credits.cast() != null) {
            credits.cast().stream()
                    .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                    .limit(MAX_ACTORS_PER_ITEM)
                    .forEach(c -> actors.add(TmdbPersonMapper.findOrCreateActor(c, actorRepository)));
        }
        return actors;
    }

    private Set<Director> mapDirectors(TmdbCreditsDto credits) {
        Set<Director> directors = new HashSet<>();
        if (credits.crew() != null) {
            credits.crew().stream()
                    .filter(c -> "Director".equalsIgnoreCase(c.job()))
                    .forEach(c -> directors.add(
                            TmdbPersonMapper.findOrCreateDirector(c, directorRepository)));
        }
        return directors;
    }

    private void throttle() {
        try {
            Thread.sleep(requestDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Throttle sleep interrupted");
        }
    }
}
```

### Sync Timing

| Scenario | API Calls | Time (~25 req/s) |
|---|---|---|
| Changes sync (100 changed items in our DB) | 100 × 2 + change pages | ~10 seconds |
| New popular discovery (5 pages × 2 types) | 10 + ~20 × 2 (details+credits for new) | ~5 seconds |
| **Typical daily sync total** | ~250 calls | **~15 seconds** |

### Non-Blocking Guarantee

The synchronizer is designed to **never block user operations**:

1. **Per-item `@Transactional`**: Each item update is its own transaction. Lock duration: milliseconds per row.
2. **No table-level locks**: Only row-level updates via JPA `save()`.
3. **Error isolation**: If updating one item fails (e.g., TMDB returns 404 for a deleted movie), the sync continues with the next item.
4. **Background execution**: `@Scheduled` runs in a separate thread from the web request handling threads.

---

## Error Handling & Resilience

### Exception Types

```java
package org.tvl.tvlooker.domain.exception;

/**
 * Thrown when a TMDB API call fails.
 */
public class TmdbApiException extends RuntimeException {
    private final int statusCode;
    
    public TmdbApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}
```

### Error Handling Strategy

| HTTP Status | Action | Retry? |
|---|---|---|
| **200** | Success | — |
| **401** | Invalid API key → log ERROR, abort sync | No |
| **404** | Item deleted from TMDB → skip item, log WARN | No |
| **429** | Rate limited → wait `Retry-After` seconds, then retry | Yes (max 3) |
| **500, 502, 503** | TMDB server error → retry with exponential backoff | Yes (max 3) |
| **Connection timeout** | Network issue → retry | Yes (max 3) |

### Retry Logic (Future Enhancement)

For the initial implementation, errors are **logged and skipped** (per-item error isolation). A future enhancement could add retry with exponential backoff:

```
Retry 1: wait 1 second
Retry 2: wait 2 seconds  
Retry 3: wait 4 seconds
After 3 retries: skip item, log ERROR
```

This can be implemented later with Spring Retry (`@Retryable`) or Resilience4j.

---

## Schema Changes Required

### Genre Entity — Add `tmdbId`

The current `Genre` entity has no `tmdbId` field. Since TMDB genres have stable numeric IDs, we need to add one for reliable mapping:

```java
@Entity
@Table(name = "genres")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_id_pk", updatable = false, nullable = false)
    private Long id;

    // NEW FIELD
    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId;

    @Column(name = "name", nullable = false)
    private String name;
}
```

**Why?** Matching genres by `name` alone is fragile across locales (e.g., "Action" vs "Acción"). TMDB genre IDs are stable and universal.

**Migration Impact:** Since the table uses `ddl-auto=update` in development, Hibernate will add the column automatically. No manual migration needed for dev.

### Item Entity — Add `updatedAt` (Optional)

Consider adding an `updatedAt` timestamp to track when an item was last synced:

```java
@Column(name = "updated_at")
private Timestamp updatedAt;
```

This is useful for:
- Knowing how fresh the data is
- Debugging sync issues
- Future: only sync items not updated in the last N days

---

## Testing Strategy

### Unit Tests

#### TmdbClient Tests
- Mock `RestClient` responses
- Verify correct URL construction with parameters
- Verify correct DTO deserialization
- Test error scenarios (4xx, 5xx responses)

#### Mapper Tests
- Test `TmdbItemMapper.fromMovie()` with known DTO → verify all entity fields
- Test `TmdbItemMapper.fromTvShow()` with known DTO
- Test `TmdbItemMapper.updateItemFromMovie()` → verify only mutable fields change
- Test date parsing edge cases (null, blank, invalid format)
- Test `TmdbGenreMapper.findOrCreate()` when genre exists vs. when it doesn't
- Test `TmdbPersonMapper` find-or-create for actors and directors

#### TmdbDataCollector Tests
- Mock `TmdbClient` and all repositories
- Verify `collectAll()` calls genres, movies, and TV shows in order
- Verify pagination stops at `maxPages` or `totalPages` (whichever is smaller)
- Verify items already in DB are skipped (`existsByTmdbIdAndTmdbType` returns true)
- Verify per-item error isolation (one failure doesn't stop the batch)
- Verify genres, actors, and directors are correctly associated

#### TmdbDataSynchronizer Tests
- Mock `TmdbClient` and all repositories
- Verify `syncChanges()` only updates items that exist in our DB
- Verify `discoverNewPopularItems()` only inserts items NOT in our DB
- Verify `lastSyncDate` updates after successful sync
- Verify error isolation per item

### Integration Tests (H2)

```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "tmdb.collector.run-on-startup=false",
    "tmdb.sync.enabled=false"
})
class TmdbDataCollectorIntegrationTest {
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private GenreRepository genreRepository;
    
    // Use @MockBean to mock TmdbClient (no real API calls)
    @MockBean
    private TmdbClient tmdbClient;
    
    @Test
    void shouldPersistMovieWithGenresAndCredits() {
        // Given: Mock TMDB responses
        // When: Collector runs
        // Then: Item saved with correct genres, actors, directors
    }
    
    @Test  
    void shouldSkipExistingItems() {
        // Given: Item already in DB
        // When: Collector encounters same tmdbId
        // Then: Item is skipped, not duplicated
    }
}
```

### Test Configuration

Add to `application-test.properties`:

```properties
# Disable TMDB sync during tests
tmdb.sync.enabled=false
tmdb.collector.run-on-startup=false
tmdb.api.key=test-api-key
```

---

## Implementation Roadmap

### Issue #1: Configuration + RestClient + TmdbClient

**Scope:**
- Create `TmdbConfig` class with RestClient bean and `@EnableScheduling`
- Add TMDB properties to `application.properties` and `application-test.properties`
- Create `TmdbClient` class with all API methods
- Unit tests for `TmdbClient`

**Acceptance Criteria:**
- RestClient configured with Bearer token authentication
- All TMDB endpoints accessible via `TmdbClient`
- API key managed via environment variable
- Tests verify URL construction and DTO deserialization

---

### Issue #2: TMDB DTO Records + Mapper Classes

**Scope:**
- Create all DTO record classes in `infrastructure.tmdb.dto`
- Create all mapper classes in `infrastructure.tmdb.mapper`
- Add `tmdbId` field to `Genre` entity
- Unit tests for all mappers

**Acceptance Criteria:**
- DTOs correctly deserialize all TMDB JSON fields
- Mappers handle null/blank dates, missing genres, etc.
- `Genre.tmdbId` added and used for find-or-create
- Mapper tests cover all edge cases

---

### Issue #3: Spring Data JPA Repositories

**Scope:**
- Create `ItemRepository`, `GenreRepository`, `ActorRepository`, `DirectorRepository`
- Add custom query methods (`findByTmdbId`, `existsByTmdbIdAndTmdbType`, etc.)
- Integration tests with H2

**Acceptance Criteria:**
- All repositories work with existing JPA entities
- `findByTmdbId` queries return correct results
- `existsByTmdbIdAndTmdbType` performs efficiently
- Tests verify CRUD operations and custom queries

---

### Issue #4: TmdbDataCollector (Bulk Load)

**Scope:**
- Create `TmdbDataCollector` service class
- Implement `collectGenres()`, `collectPopularMovies()`, `collectPopularTvShows()`
- Add `ApplicationRunner` trigger with `tmdb.collector.run-on-startup` flag
- Implement rate limiting (throttle delays)
- Implement idempotency (skip existing items)
- Unit tests + integration tests

**Acceptance Criteria:**
- Collector fetches genres, movies, and TV shows from TMDB
- Each item persisted with genres, actors (top 10), and directors
- Existing items are skipped (no duplicates)
- Individual item failures don't abort the batch
- Logging shows progress (page X/Y, collected/skipped counts)
- Rate limit respected (~25 req/s)
- Configurable via properties (max pages, delay, run-on-startup)

---

### Issue #5: TmdbDataSynchronizer (Scheduled Sync)

**Scope:**
- Create `TmdbDataSynchronizer` service class
- Implement changes-based sync for existing items
- Implement new popular items discovery
- Add `@Scheduled` with configurable interval
- Ensure non-blocking behavior (per-item transactions)
- Unit tests + integration tests

**Acceptance Criteria:**
- Synchronizer updates popularity, vote average, genres, actors, directors
- Only updates items already in our DB (doesn't sync the entire TMDB catalog)
- Discovers and inserts new popular items
- Per-item transactions: no long locks, users unaffected
- Error isolation: one failure doesn't abort the sync
- Configurable interval and enabled flag
- Logging shows sync results

---

### Issue #6: Integration Testing + End-to-End Validation

**Scope:**
- End-to-end integration tests with mocked TMDB API
- Performance benchmarks (verify sync completes in reasonable time)
- Edge case testing (empty responses, API errors, concurrent access)
- Documentation updates (README, QUICKSTART)

**Acceptance Criteria:**
- Full pipeline tested: API call → DTO → Mapper → Repository → Database
- Error scenarios handled gracefully
- No data corruption with concurrent user operations
- Documentation updated with TMDB setup instructions

---

## Constraints & Assumptions

### TMDB API Constraints

- **Rate limit:** ~40 requests/second. We target ~25 req/s to stay safely under.
- **Pagination:** Max 500 pages per endpoint, 20 results per page (max 10,000 items per list).
- **Changes endpoint:** Returns changes for the last 14 days only. Daily sync ensures we never miss changes.
- **Authentication:** Requires an API Read Access Token (Bearer token).

### Data Constraints

- **Actors per item:** Limited to top 10 billed actors (by `order` field) to keep data manageable.
- **Directors:** All crew members with `job="Director"` are stored.
- **Genre mapping:** Uses `tmdbId` (requires schema change to add `Genre.tmdbId`).
- **Language:** Titles and overviews are fetched in `es-MX` by default (configurable).

### Non-Functional Requirements

- **Initial load time:** ~3 minutes for 2000 items (50 pages × 2 types).
- **Daily sync time:** ~15 seconds for typical changes.
- **No user impact:** Per-item transactions guarantee no blocking.
- **Idempotent:** Running the collector twice produces the same result.

### Assumptions

- The application has internet access to reach `api.themoviedb.org`.
- A valid TMDB API key is provided via environment variable.
- The database is already created (via Docker Compose, as existing setup).
- `spring.jpa.hibernate.ddl-auto=update` handles schema changes in development.

---

## Summary

This design provides:

✅ **Complete data pipeline** — TMDB API → DTOs → Mappers → Entities → Database  
✅ **Bulk load capability** — Initial population with configurable scope  
✅ **Incremental sync** — Efficient daily updates using TMDB's changes API  
✅ **Non-blocking** — Per-item transactions, no user impact  
✅ **Idempotent** — Safe to re-run without creating duplicates  
✅ **Rate-limit aware** — Configurable throttling  
✅ **Error resilient** — Per-item error isolation  
✅ **Fully configurable** — All parameters via `application.properties`  
✅ **Clean architecture** — Separated concerns (client, DTOs, mappers, repositories, services)  
✅ **Testable** — Each layer independently testable  

---

**Next Steps:**
1. Review and approve this design document
2. Add `tmdbId` column to `Genre` entity
3. Begin implementation with Issue #1 (Configuration + TmdbClient)
4. Sequential implementation through Issue #6

