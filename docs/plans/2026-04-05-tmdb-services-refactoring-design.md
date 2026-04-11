# Tmdb Services Refactoring Design

**Date:** 2026-04-05  
**Author:** TV Looker Team  
**Status:** Approved

## Objective

Refactorizar `TmdbDataSynchronizerService` y `TmdbDataCollectorService` para que utilicen `TmdbDataFetcher` como puerta de entrada única a la API de TMDB, centralizando el rate limiting y maximizando el rendimiento mediante paralelismo.

## Problem

1. **Rate limiting descentralizado**: 
   - `TmdbDataFetcher` tiene su propio `RateLimiter` (35 req/s)
   - `TmdbDataSynchronizerService` llama directamente a `TmdbClient` sin rate limiting
   - `TmdbDataCollectorService` mezcla llamadas directas y a través del fetcher

2. **Código duplicado**: Ambos servicios tienen lógica similar para fetching
3. **Rendimiento subóptimo**: El sincronizador procesa cambios secuencialmente

## Solution

### Arquitectura Propuesta

```
TmdbDataSynchronizerService ──→ TmdbDataFetcher (rate limiting) ──→ TmdbClient
TmdbDataCollectorService    ──→ TmdbDataFetcher (rate limiting) ──→ TmdbClient
```

### TmdbDataFetcher - Nuevos métodos

```java
public CompletableFuture<TmdbPagedResponseDto<TmdbChangesDto>> fetchChangesAsync(
    TmdbMediaType type, LocalDate startDate, LocalDate endDate, int page)
```

### TmdbDataSynchronizerService - Refactoring

**Cambios:**
1. Inyectar `TmdbDataFetcher` en lugar de solo `TmdbClient`
2. Reemplazar todas las llamadas directas a `tmdbClient.getMovieDetails()`, `getMovieCredits()`, etc.
3. Usar `getDetailsWithCredits()` del fetcher (más eficiente - un request en vez de dos)
4. Implementar procesamiento paralelo de cambios dentro de cada página

**Método syncChanges refactorizado:**
```java
private int syncChanges(TmdbType type, LocalDate startDate, LocalDate endDate) {
    // Fetch página de cambios
    TmdbPagedResponseDto<TmdbChangesDto> changes = fetcher.fetchChangesAsync(...)
    
    // Filtrar solo los que existen en nuestra DB
    List<Long> existingIds = filterExistingIds(changes.results(), type)
    
    // Fetch detalles en paralelo
    List<CompletableFuture> futures = existingIds.stream()
        .map(id -> fetcher.fetchDetailsWithCreditsAsync(type, id))
    
    // Procesar resultados
    CompletableFuture.allOf(futures.toArray()).join()
}
```

### TmdbDataCollectorService - Refactoring

**Cambios:**
1. Reemplazar llamadas directas a `tmdbClient.getGenres()` con `dataFetcher.fetchGenresAsync()`
2. Reemplazar llamadas directas a `tmdbClient.getPopular()` con `dataFetcher.fetchPopularAsync()`
3. Eliminar inyección de `TmdbClient` (ya no es necesario directamente)

## SOLID Principles Applied

- **SRP**: El fetcher maneja solo fetching + rate limiting
- **DIP**: Servicios dependen de abstracciones (interfaz del fetcher)
- **OCP**: Nuevos tipos de media pueden añadirse sin modificar código existente

## Performance

- **Antes**: Sincronizador procesa cambios secuencialmente (1 req por vez)
- **Después**: Múltiples cambios se procesan en paralelo (hasta 20+ threads)
- **Estimación**: 1000 cambios → ~28 segundos (vs varios minutos antes)

## Migration Path

1. Añadir `fetchChangesAsync()` a `TmdbDataFetcher`
2. Añadir `fetchDetailsWithCreditsAsync()` genérico a `TmdbDataFetcher`
3. Modificar `TmdbDataSynchronizerService` para usar fetcher
4. Modificar `TmdbDataCollectorService` para usar fetcher exclusivamente
5. Verificar tests existentes
