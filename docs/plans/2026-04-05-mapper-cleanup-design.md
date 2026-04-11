# Mapper Cleanup Design

**Date:** 2026-04-05  
**Author:** TV Looker Team  
**Status:** Approved

## Problem

TmdbItemPersistenceService tiene métodos de mapeo (`mapGenres()`, `mapActors()`, `mapDirectors()`) que:
1. Llaman a los métodos deprecated `findOrCreate` de los mappers
2. Violan SRP - el servicio hace trabajo de mapper
3. Son redundantes porque EntityCacheService ya hace findOrCreate

## Solution

### Responsibilities

- **EntityCacheService**: Busca o crea entidades (findOrCreate batch)
- **TmdbGenreMapper/TmdbCastMemberMapper**: Solo convierte DTO → Entity (sin DB)
- **TmdbItemBuilder**: Construye ItemEntity con ActorItems usando los caches
- **TmdbItemPersistenceService**: Solo orquesta y persiste

### TmdbItemPersistenceService - Refactor

```java
public void persistMovies(List<TmdbMovieDetailsDto> movieDetails) {
    // 1. Extraer datos para cache
    Set<TmdbGenreDto> allGenres = ...;
    List<TmdbCreditsDto.CastMember> allCast = ...;
    List<TmdbCreditsDto.CrewMember> allCrew = ...;
    
    // 2. Delegar a EntityCacheService (findOrCreate batch)
    Map<Long, GenreEntity> genreCache = entityCacheService.findOrCreateGenres(...);
    Map<Long, ActorEntity> actorCache = entityCacheService.findOrCreateActors(...);
    Map<Long, DirectorEntity> directorCache = entityCacheService.findOrCreateDirectors(...);
    
    // 3. Delegar a TmdbItemBuilder (construye ItemEntity con ActorItems)
    List<ItemEntity> items = movieDetails.stream()
        .map(dto -> TmdbItemBuilder.buildFromMovieDetails(dto, genreCache, actorCache, directorCache))
        .toList();
    
    // 4. Solo persistir
    itemRepository.saveAll(items);
}
```

### Remove from TmdbItemPersistenceService

- `mapGenres()` - redundante, EntityCacheService ya lo hace
- `mapActors()` - redundante, EntityCacheService ya lo hace  
- `mapDirectors()` - redundante, EntityCacheService ya lo hace
- `mapActorsToActorItems()` - ya está en TmdbItemBuilder

## Migration Path

1. Refactorizar persistMovies/persistTvShows para usar EntityCacheService + TmdbItemBuilder
2. Eliminar métodos mapGenres/mapActors/mapDirectors/mapActorsToActorItems
3. Limpiar imports innecesarios
4. Verificar compilation y tests
