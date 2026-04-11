# TmdbClient Refactoring Design

**Date:** 2026-04-05  
**Author:** TV Looker Team  
**Status:** Approved

## Objective

Refactorizar `TmdbClient` para eliminar código duplicado aplicando generics y principios SOLID (especialmente DIP - Dependency Inversion Principle).

## Problem

La clase `TmdbClient` tiene métodos con patrones idénticos para Movies y TV Shows:

- `getPopularMovies(int page)` / `getPopularTvShows(int page)`
- `getMovieDetailsWithCredits(long id)` / `getTvShowDetailsWithCredits(long id)`
- `getMovieChanges(...)` / `getTvShowChanges(...)`

Cada par sigue la misma estructura pero con diferente endpoint path y tipo de respuesta.

## Solution

### 1. New Enums

```java
package org.tvl.tvlooker.persistence.tmdb;

public enum TmdbMediaType {
    MOVIE("movie"),
    TV("tv");

    private final String path;
    
    public String getPath() { return path; }
    public String getGenresEndpoint() { return path + "/genre/list"; }
}
```

### 2. New Interfaces

```java
package org.tvl.tvlooker.persistence.tmdb.dto;

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

```java
public interface TmdbMediaDetails extends TmdbMediaItem {
    TmdbCreditsDto credits();
}
```

### 3. Modified DTOs

**TmdbMovieDto:**
- Implementa `TmdbMediaItem` y `TmdbMediaDetails`
- Hereda métodos tal cual (title, releaseDate, etc.)

**TmdbTvShowDto:**
- Implementa `TmdbMediaItem` y `TmdbMediaDetails`
- Agrega método default `title()` que retorna `name()`
- Agrega método default `releaseDate()` que retorna `firstAirDate()`

### 4. Refactored TmdbClient

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

### 5. Backwards Compatibility

Methods to be REMOVED (not deprecated, per user request):
- `getPopularMovies(int)`
- `getPopularTvShows(int)`
- `getMovieDetailsWithCredits(long)`
- `getTvShowDetailsWithCredits(long)`
- `getMovieChanges(...)`
- `getTvShowChanges(...)`
- `getMovieGenres()`
- `getTvGenres()`
- `getMovieDetails(long)` (already deprecated)
- `getMovieCredits(long)` (already deprecated)
- `getTvShowDetails(long)` (already deprecated)
- `getTvShowCredits(long)` (already deprecated)

## SOLID Principles Applied

- **SRP**: Cada interfaz tiene una responsabilidad (representar items vs details)
- **OCP**: Extender comportamiento sin modificar código existente (nuevos tipos pueden implementar interfaces)
- **LSP**: DTOs existentes son subtipos válidos
- **ISP**: Interfaces pequenas y enfocadas
- **DIP**: Cliente depende de abstracciones (`TmdbMediaItem`), no de implementaciones concretas

## Testing Strategy

1. Crear test unitario para cada método genérico
2. Verificar que Movies y TV Shows funcionan correctamente
3. Mantener tests existentes - deberían fallar si algo se rompe (verificar backwards compatibility)

## Migration Path

1. Crear interfaces `TmdbMediaItem`, `TmdbMediaDetails`
2. Crear enum `TmdbMediaType`
3. Modificar DTOs para implementar interfaces
4. Agregar métodos genéricos a `TmdbClient`
5. Eliminar métodos duplicados old
6. Ejecutar tests existentes
