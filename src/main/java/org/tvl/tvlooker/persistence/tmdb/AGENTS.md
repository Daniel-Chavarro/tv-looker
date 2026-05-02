# TMDB Persistence Boundary Knowledge

## OVERVIEW
External TMDB contract boundary: REST client, media-type enum, response DTO records/classes, and mappers that normalize TMDB payloads before service persistence.

## STRUCTURE
```text
persistence/tmdb/
|-- TmdbClient.java
|-- TmdbMediaType.java
|-- dto/
`-- mapper/
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| HTTP transport | `TmdbClient.java` | Uses Spring `RestClient` bean from `TmdbConfig` |
| Media type paths | `TmdbMediaType.java` | Movie/TV path mapping for changes endpoints |
| External DTOs | `dto/Tmdb*.java` | Shape mirrors TMDB API responses |
| Mapping | `mapper/TmdbItemMapper.java` and peers | Converts TMDB DTOs to internal entities/domain shapes |
| Service usage | `../../service/tmdb/` | Fetch, collection, sync, persistence workflows |
| Config | `../../config/TmdbConfig.java` | Base URL, bearer token, JSON headers |

## CONVENTIONS
- Keep TMDB DTOs close to the API shape; normalize in mappers/services, not by mutating transport DTOs.
- `TmdbClient` is the shared transport boundary; avoid direct `RestClient` use outside this package/config.
- TMDB API auth uses v4 bearer token from `TMDB_API_KEY`.
- Paths should rely on `TmdbMediaType` where movie/TV endpoint names differ.

## ANTI-PATTERNS
- Do not add hardcoded API keys or real tokens; use env vars.
- Do not spread TMDB URL construction into service classes.
- Do not use deprecated `TmdbClient` methods for new code; prefer the newer generic/batch paths already used by services.
- Do not assume TMDB payloads always contain credits/genres; persistence services explicitly guard missing data.

## COMMANDS
```bash
mvn test -Dtest=TmdbDataFetcherTest
mvn test -Dtest=TmdbItemPersistenceServiceTest
mvn test -Dtest=TmdbDataSynchronizerServiceTest
```

## NOTES
- This package is infrastructure, not domain. Keep business decisions in `service/tmdb/` or domain strategy code.
- API details are easiest to verify through service tests because most TMDB client use is mocked one layer up.
