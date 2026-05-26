# PROJECT KNOWLEDGE BASE

**Generated:** 2026-05-02
**Commit:** 17260ec
**Branch:** feat/frontend

## OVERVIEW
TV Looker is a two-app repo: Spring Boot 4/Maven backend at the root, Vite/React 19 frontend in `frontend/`. The backend owns `/api/v1/**`; the frontend is a standalone SPA shell, not a root JS workspace.

## STRUCTURE
```text
tv-looker/
|-- src/main/java/org/tvl/tvlooker/  # Spring backend packages
|   |-- api/                         # controllers, API DTOs, REST errors
|   |-- config/                      # security, async, OpenAPI, TMDB, recommendations
|   |-- domain/                      # entities/domain DTOs/mappers + recommendation engine
|   |-- persistence/                 # JPA repos + TMDB transport DTO/client
|   |-- service/                     # use cases, auth/JWT, TMDB orchestration
|   `-- utils/                       # startup admin seeding
|-- src/test/                        # Java tests with H2 PostgreSQL mode
|-- frontend/                        # independent Vite/React app
|-- docs/                            # design/planning docs; not always authoritative
|-- scripts/                         # setup/cleanup helpers
`-- docker-compose.yml               # local Postgres only
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Backend entrypoint | `src/main/java/org/tvl/tvlooker/TvLookerApplication.java` | `@SpringBootApplication`, `@EnableAsync` |
| REST endpoints | `src/main/java/org/tvl/tvlooker/api/controller/` | Controllers are under `/api/v1/**` |
| REST DTOs | `src/main/java/org/tvl/tvlooker/api/dto/` | `request/`, `response/`, `mapper/` |
| API errors | `src/main/java/org/tvl/tvlooker/api/exception/GlobalExceptionHandler.java` | Domain exceptions normalized here |
| Auth/security | `config/SecurityConfig.java`, `service/AuthService.java`, `service/JwtService.java` | JWT secret must be at least 32 bytes |
| Domain model | `domain/model/{entity,dto,mapper,enums}/` | Entities and domain mapping live together |
| Recommendations | `domain/motor/`, `domain/strategy/`, `config/RecommendationConfig.java` | Child AGENTS.md in `domain/motor/` |
| TMDB ingestion | `service/tmdb/`, `persistence/tmdb/`, `config/TmdbConfig.java` | Child AGENTS.md files in both TMDB areas |
| JPA repositories | `persistence/repository/` | Includes TMDB lookup helpers and ownership queries |
| Tests | `src/test/java/org/tvl/tvlooker/` | Mirrors backend packages; H2 config in test resources |
| Frontend app | `frontend/` | Child AGENTS.md owns SPA/API/router gotchas |
| CI | `.github/workflows/ci.yml` | Backend-only Maven pipeline |
| Local setup | `docker-compose.yml`, `scripts/setup.*`, `.env.example` | Compose requires env DB creds |

## CODE MAP
| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `TvLookerApplication` | class | `src/main/java/org/tvl/tvlooker/` | Backend bootstrap |
| `SecurityConfig` | config | `config/` | JWT, route authorization, password encoder |
| `AsyncConfiguration` | config | `config/` | General executor + `tmdbTaskExecutor` |
| `RecommendationConfig` | config | `config/` | Strategy/aggregation bean wiring and weights |
| `GlobalExceptionHandler` | advice | `api/exception/` | REST error translation |
| `HybridRecommendationEngine` | domain service | `domain/motor/` | Strategy execution + aggregation |
| `RecommendationContext` | context | `domain/motor/utils/` | Shared cached recommendation inputs |
| `TmdbClient` | integration client | `persistence/tmdb/` | Only TMDB transport boundary |
| `TmdbDataFetcher` | service | `service/tmdb/` | Rate-limited async TMDB fetches |
| `EntityCacheService` | service | `service/tmdb/` | Batch find-or-create for TMDB entities |
| `apiClient` | frontend client | `frontend/src/api/client.ts` | Axios transport, auth header, 401 redirect |
| `AuthProvider` | frontend context | `frontend/src/contexts/AuthContext.tsx` | Frontend auth state |

## CONVENTIONS
- Backend is Java 21, Spring Boot 4.0.2, Maven. Use root Maven commands; there is no root JS workspace.
- Backend route contract is `/api/v1/**`; OpenAPI is scoped with `springdoc.paths-to-match=/api/v1/**`.
- API DTOs are separate from domain DTOs: `api/dto/*` for HTTP, `domain/model/*` for internal domain/persistence mapping.
- Entity/domain mappers use `domain/model/mapper/*EntityMapper`; API mappers use `api/dto/mapper/*Mapper`.
- Maven profiles set `spring.profiles.active`: default profile for normal package/run, `test` profile when tests are not skipped.
- Tests use H2 in PostgreSQL mode and `ddl-auto=create-drop`; normal Maven tests do not need local Postgres.
- Checkstyle and PMD run at `verify`, not at plain `test`.
- Frontend verification is local-only; GitHub CI does not run npm scripts.

## ANTI-PATTERNS (THIS PROJECT)
- Do not commit real `.env` values. `.env.example` lists names only.
- Do not assume frontend calls work until `/api` vs `/api/v1` prefix/proxy wiring is checked.
- Do not use `frontend/README.md` as authoritative deployment guidance; it is mostly a React Router template.
- Do not trust `frontend/Dockerfile` as current: it copies `build` and runs `npm run start`, but Vite outputs `dist` and no `start` script exists.
- Do not switch to React Router framework/file-routing commands unless deliberately migrating; the app uses `BrowserRouter` manually.
- Do not add old Tailwind v3-style setup without checking existing Tailwind v4 Vite/PostCSS wiring.
- Treat `scripts/cleanup.*` option 3 / `docker-compose down -v` as data loss.
- Treat `docs/plans/**` and `docs/superpowers/**` as design history unless current code confirms them.

## UNIQUE STYLES
- Recommendation strategy wiring is property-driven in `RecommendationConfig`; current default aggregation is `constant`.
- TMDB sync is both profile-gated (`!test`) and property-gated; collector startup is disabled by default.
- TMDB has two executor paths: general async and dedicated `tmdbTaskExecutor`.
- `SecurityConfig` permits public GETs for catalog endpoints but protects user/list/review interactions.
- Pagination defaults are configured in Spring properties: max 100, default 50, one-indexed parameters.

## COMMANDS
```bash
# Backend
mvn spring-boot:run
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
mvn clean verify
mvn package -DskipTests

# Local services
docker-compose up -d

# Frontend (run from frontend/)
npm run dev
npm run build
npm run lint
npm run format
```

## NOTES
- CI runs `mvn validate`, `mvn clean verify`, then `mvn package -DskipTests`; artifact is `target/*.jar`.
- PR titles must start with semantic type: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, or `style`.
- `/oc` or `/opencode` in GitHub comments triggers `.github/workflows/opencode.yml`.
- LSP was unavailable during generation (`jdtls` and `typescript-language-server` not installed); code map uses direct search/AST results.
- Existing root README still contains stale setup examples; prefer live config files and this knowledge base.
