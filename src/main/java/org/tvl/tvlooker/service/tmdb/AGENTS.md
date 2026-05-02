# TMDB Service Knowledge

## OVERVIEW
High-side-effect service layer for TMDB ingestion: async collection, scheduled sync, rate-limited fetches, entity caching, and transactional upserts.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Bulk collection | `TmdbDataCollectorService.java` | Full/genre/movie/TV collection workflows |
| Scheduled sync | `TmdbDataSynchronizerService.java` | `@Scheduled`, `@Profile("!test")`, property-gated |
| Fetch orchestration | `TmdbDataFetcher.java` | Uses `tmdbTaskExecutor` and Guava `RateLimiter` |
| Item persistence | `TmdbItemPersistenceService.java` | Maps TMDB details to JPA entities |
| Entity cache/upsert | `EntityCacheService.java` | Batch find-or-create for actors/directors/genres |
| Transactional saves | `EntitySaveHelper.java` | `REQUIRES_NEW`, save-and-flush helpers |

## CONVENTIONS
- External transport lives in sibling package `persistence/tmdb/`; services should use `TmdbClient` through `TmdbDataFetcher` rather than making ad hoc HTTP calls.
- TMDB request rate is property-driven by `tmdb.api.rate-limit`; `TmdbDataFetcher` rejects values <= 0 or > 40.
- `tmdbTaskExecutor` is the dedicated executor for TMDB API work; don't route high-volume fetches through the general executor casually.
- Collection entrypoints are async where controller responses return `started`; genre collection and sync endpoints may be synchronous.
- Tests live under `src/test/java/org/tvl/tvlooker/service/tmdb/` and mock external TMDB access.

## ANTI-PATTERNS
- Do not enable startup collection by default; `tmdb.collector.run-on-startup=false` is intentional.
- Do not remove `@Profile("!test")` from the synchronizer; tests should not run scheduled TMDB sync.
- Do not bypass `EntityCacheService` for actors/directors/genres; it handles duplicate/race cases.
- Do not ignore TMDB's 40 requests/second hard limit; default safety margin is 35.
- Do not treat `tmdb.sync.interval-ms` comments blindly; current main value is 604800000 ms (7 days).

## COMMANDS
```bash
mvn test -Dtest=TmdbDataFetcherTest
mvn test -Dtest=TmdbDataCollectorServiceTest
mvn test -Dtest=TmdbDataSynchronizerServiceTest
mvn test -Dtest=TmdbAdminControllerTest
```

## NOTES
- `TmdbDataCollectorService` uses a single in-progress gate; only one collection should run at a time.
- Tests use `application-test.properties`; H2 mode and test config avoid real Postgres, but `TMDB_API_KEY` may still need a placeholder in CI/env.
