# Agent Notes

## Repo Shape
- This repo has two independent apps: a Spring Boot/Maven backend at the root and a Vite/React frontend in `frontend/`. There is no root JS workspace manifest.
- Backend entrypoint is `src/main/java/org/tvl/tvlooker/TvLookerApplication.java`; REST controllers are under `/api/v1/**` and OpenAPI is scoped to that path.
- Frontend entrypoint is `frontend/index.html` -> `frontend/src/main.tsx` -> `frontend/src/App.tsx`. Routes are manual `BrowserRouter` routes, not React Router file-based routing.

## Commands That Matter
- Backend dev: `mvn spring-boot:run` from the repo root after Postgres is available.
- Backend focused tests: `mvn test -Dtest=ClassName` or `mvn test -Dtest=ClassName#methodName`.
- Backend merge gate: `mvn clean verify`. CI and the PR template require this; it runs tests plus Checkstyle and PMD bound to Maven `verify`.
- Backend package: `mvn package -DskipTests` matches CI artifact packaging after verification.
- Frontend dev/build from `frontend/`: `npm run dev`, `npm run build`, `npm run lint`, `npm run format`. There is no `npm test` script.

## Environment And Services
- Local Postgres is `docker-compose up -d` from the repo root; `docker-compose.yml` uses `postgres:17`, container `postgres-tv-looker`, port `5432`, and the `postgres-data` volume.
- Compose and Spring both require `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`; backend TMDB features require `TMDB_API_KEY`. Use `.env.example` for variable names, but never commit real `.env` values.
- Tests use `src/test/resources/application-test.properties` with H2 in PostgreSQL mode and `ddl-auto=create-drop`; do not require local Postgres for normal Maven tests.
- `scripts/setup.*` checks prerequisites, starts Docker Compose, and resolves Maven deps. `scripts/cleanup.*` has a destructive volume wipe option; treat option 3 / `down -v` as data loss.

## Frontend Gotchas
- `frontend/src/api/client.ts` hardcodes Axios `baseURL: '/api'`, while backend controllers are under `/api/v1/**`. Verify or fix API prefix/proxy wiring before assuming frontend calls work.
- `frontend/src/App.tsx` currently routes `/login` and `/register` to placeholder `<div>` elements even though auth page components exist.
- `frontend/react-router.config.ts` says `ssr: true`, but the actual app uses `BrowserRouter`; do not switch to React Router framework commands unless deliberately migrating.
- `frontend/Dockerfile` is stale against current scripts: it copies `/app/build` and runs `npm run start`, but Vite outputs `dist` and `package.json` has no `start` script.
- Tailwind is wired with Tailwind v4 plugins in both `vite.config.ts` (`@tailwindcss/vite`) and `postcss.config.js` (`@tailwindcss/postcss`). Do not add an older Tailwind setup without checking this first.

## Backend Architecture Notes
- `@EnableAsync` is on the Spring Boot app. `config/AsyncConfiguration.java` defines separate general and TMDB executors.
- TMDB ingestion/sync is in `service/tmdb/`; scheduled sync is excluded from the `test` profile. Startup collection and sync are disabled by default in `application.properties`.
- Recommendation wiring is centralized in `config/RecommendationConfig.java`; enabled strategies and aggregation weights are properties in `application.properties`.

## CI And Workflow
- GitHub CI is backend-only: `mvn validate`, `mvn clean verify`, then `mvn package -DskipTests`. Frontend changes need local `frontend` verification because CI does not run npm scripts.
- PR titles are checked against semantic types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `style`.
- `/oc` or `/opencode` in GitHub comments triggers `.github/workflows/opencode.yml`.

## Low-Signal Docs To Avoid Copying
- `frontend/README.md` is mostly the generic React Router template and conflicts with this repo's Vite scripts/output.
- `HELP.md` is generic Spring Initializr help.
- Broad skill docs under `.agents/skills/` are tool instructions, not repo-specific project rules.
