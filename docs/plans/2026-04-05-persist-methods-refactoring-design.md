# Persist Methods Refactoring Design

**Date:** 2026-04-05  
**Author:** TV Looker Team  
**Status:** Approved

## Objective

Mover todos los métodos de persistencia desde `TmdbDataCollectorService` y `TmdbDataSynchronizerService` hacia `TmdbItemPersistenceService`, cumpliendo con el principio de responsabilidad única (SRP).

## Problem

Actualmente hay lógica de persistencia duplicada en múltiples servicios:
- `TmdbDataCollectorService` tiene `persistMoviesBatch()`, `persistTvShowsBatch()`, `persistGenreList()`
- `TmdbDataSynchronizerService` tiene `discoverNewMovies()`, `discoverNewTvShows()`, `updateExistingItem()`

Esto viola SRP y causa código duplicado.

## Solution

### TmdbItemPersistenceService - Nuevos métodos

```java
// Batch persist
public void persistMovies(List<TmdbMovieDetailsDto> movieDetails)
public void persistTvShows(List<TmdbTvShowDetailsDto> tvShowDetails)

// Individual persist with discovery
public int discoverAndPersistNewMovies(List<TmdbMovieDto> movies)
public int discoverAndPersistNewTvShows(List<TmdbTvShowDto> tvShows)

// Update existing
public void updateItem(ItemEntity item, Object details)

// Genres
public void persistGenres(TmdbGenreListDto genreList)
```

### TmdbDataCollectorService cambios

- Eliminar `persistMoviesBatch()` - ahora llama a `persistenceService.persistMovies()`
- Eliminar `persistTvShowsBatch()` - ahora llama a `persistenceService.persistTvShows()`
- Eliminar `persistGenreList()` - ahora llama a `persistenceService.persistGenres()`

### TmdbDataSynchronizerService cambios

- Eliminar `discoverNewMovies()` - ahora llama a `persistenceService.discoverAndPersistNewMovies()`
- Eliminar `discoverNewTvShows()` - ahora llama a `persistenceService.discoverAndPersistNewTvShows()`
- Eliminar `updateExistingItem()` - ahora llama a `persistenceService.updateItem()`

## SOLID Principles Applied

- **SRP**: TmdbItemPersistenceService maneja solo persistencia
- **DRY**: Elimina duplicación de lógica
- ** DIP**: Servicios dependen de abstracciones del persistence service

## Migration Path

1. Añadir nuevos métodos a TmdbItemPersistenceService
2. Actualizar TmdbDataCollectorService para usar los nuevos métodos
3. Actualizar TmdbDataSynchronizerService para usar los nuevos métodos
4. Verificar tests existentes