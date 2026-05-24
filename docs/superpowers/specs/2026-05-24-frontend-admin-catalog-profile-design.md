# Frontend Admin, Catalog, And Profile Design

## Context

TV Looker has a Spring Boot backend under `/api/v1/**` and a standalone Vite/React frontend under `frontend/`. The frontend already has authentication state, protected routes, catalog item cards, a profile page, and API modules, but several user-facing features are incomplete.

This design covers a focused feature pass for admin TMDB operations, catalog search and pagination, profile navigation, and regular-user profile access. It deliberately avoids broader admin user CRUD and backend item filtering Specifications, which should be handled in separate follow-up work.

## Goals

- Add role-gated frontend navigation for authenticated profile access.
- Add role-gated frontend navigation and routing for admin TMDB operations.
- Build an admin TMDB page for collect, sync, and status using existing backend endpoints.
- Add Home page search and placeholder filter controls that pass query params to the backend.
- Use backend pagination with fixed `size=20` and one-indexed `page` request params.
- Reset pagination to page `1` whenever a new search or filter change is submitted.
- Add a regular-user self-profile backend API so profile view/update does not depend on admin-only user CRUD endpoints.

## Non-Goals

- Do not implement TMDB search.
- Do not add broad admin user-management pages.
- Do not implement backend item filtering Specifications in this pass.
- Do not add user-selectable page size.
- Do not redesign the full frontend visual system.
- Do not make regular users call general-purpose `/api/v1/users/**` endpoints for profile data.

## Approved Approach

Use a focused frontend feature pass plus one backend self-profile API.

The frontend will add the missing UI, API modules, hooks, route guards, and tests around existing patterns. The backend will add narrowly scoped `/api/v1/me` endpoints for authenticated users to view and update their own profile. Existing admin TMDB endpoints and admin-only user CRUD remain protected by admin authority.

## Architecture

Frontend changes should follow the current structure:

- API modules live in `frontend/src/api`.
- React Query hooks live in `frontend/src/hooks`.
- Route pages live in `frontend/src/pages`.
- Shared UI stays in `frontend/src/components`.

Add a `tmdbAdminApi` module for:

- `GET /admin/tmdb/status`
- `POST /admin/tmdb/collect`
- `POST /admin/tmdb/sync`

Because the shared Axios client already uses `/api/v1` as its base URL, these paths are relative to `/api/v1`.

Add an admin TMDB page at `frontend/src/pages/AdminTmdb.tsx` and protect it with an admin-only route guard. Header navigation should show an `Admin` link only when `authUser.authority === 'ADMIN'`. Header navigation should show `Profile` for authenticated users.

Update the catalog item API request type to use `size` instead of `pageSize`, matching Spring pagination request params. The frontend will rely on the backend Java `PageResponse` contract matching the frontend `PaginatedResponse` shape.

Backend changes are limited to self-profile endpoints under a non-admin path such as `/api/v1/me`. Existing `/api/v1/users/**` routes remain admin-only.

## Data Flow

The Home page owns catalog query state in React state:

- `searchInput`: current text in the input.
- `search`: submitted search value used for the API request.
- `type`: optional item type filter.
- `genreId`: optional genre filter value.
- `year`: optional year filter value.
- `rating`: optional rating filter value.
- `page`: current one-indexed page.
- `size`: fixed at `20`.

Submitting a new search resets `page` to `1`. Changing any filter also resets `page` to `1`. The item hook calls `GET /api/v1/items` with `page`, `size`, and only the active optional query params.

The frontend does not filter items locally. Search and filter controls only shape the backend request. Backend item filtering through Specifications is expected in a later PR.

The item API expects `PaginatedResponse<Item>` directly from the backend. The UI reads:

- `content`
- `actualPage`
- `totalPages`
- `totalItems`
- `isLast`

The Profile page should switch from `/users/{id}` to self-profile APIs:

- `GET /api/v1/me`
- `PATCH /api/v1/me`

The self-profile backend implementation must derive the user from the authenticated principal or JWT context, not from a user ID path variable.

The TMDB admin page fetches status on load and after collect or sync operations. Collect and sync buttons call their POST endpoints, show action-level loading feedback, display success or error messages, and refresh status after completion.

## Components And UX

The Home page should become a single catalog panel rather than duplicating the same data into separate Featured and Trending sections. It should include:

- A prominent search input and submit button.
- Lightweight controls for type, genre, year, and rating.
- Existing grid/card styling through `ItemGrid` and `ItemCard` where possible.
- Loading, error, and empty states consistent with current behavior.
- Pagination controls below the grid.

Pagination should show current page, total pages, total item count, previous/next buttons, and compact page-number buttons when useful. Page size remains fixed at `20`.

The Header should add:

- `Profile` for authenticated users.
- `Admin` only for users with `authority === 'ADMIN'`.

The TMDB admin page should be operational and simple:

- Status card for collector running state, sync enabled state, last sync date, and status timestamp.
- `Collect TMDB Data` action.
- `Sync TMDB Data` action.
- Inline success and error messages.
- Disabled/loading states while requests are in flight.

The Profile page should keep the current modal edit pattern and accessibility basics from shared inputs and buttons. It should no longer depend on admin user APIs for normal user profile viewing or editing.

## Error Handling And Access Control

Admin access is enforced in both frontend and backend:

- Frontend navigation and admin routes check `authority === 'ADMIN'`.
- Backend `/api/v1/admin/tmdb/**` remains protected by admin authority.

The admin route must guard direct URL access. Unauthenticated users follow the normal login redirect. Authenticated non-admin users should receive a simple forbidden state.

API failures should surface close to the affected action:

- Home item fetch failures show the existing page or grid error state.
- Search and filter changes reset to page `1` to avoid stale high-page empty states.
- TMDB operation failures show inline error text and keep the page usable.
- TMDB operation successes show the backend response message and refresh status.
- Profile load and update failures use the existing profile page and modal feedback patterns.

The self-profile backend API must not accept a user ID in the route. It must use the authenticated user identity, preventing regular users from selecting another user's profile by changing a URL.

## Testing

Frontend tests should cover:

- Header shows `Profile` for authenticated users.
- Header shows the admin link only for `ADMIN` users.
- Admin route blocks non-admin access.
- Home sends `page`, `size`, and active query params.
- Home resets page to `1` after a new search or filter change.
- Pagination controls render current page and update page state.
- TMDB admin page calls status, collect, and sync endpoints with loading, success, and error states.
- Profile uses self-profile APIs instead of `/users/{id}`.

Backend tests should cover:

- Authenticated regular users can `GET /api/v1/me`.
- Authenticated regular users can `PATCH /api/v1/me`.
- Unauthenticated users cannot access `/api/v1/me`.
- Regular users still cannot access admin user CRUD.
- Admin TMDB endpoints remain admin-only.

Verification commands:

- Backend: `mvn test`
- Frontend: from `frontend/`, `npm run test:run`, `npm run build`, and `npm run lint`

## Scope Boundaries

This feature pass should be shippable without backend item Specifications. The frontend can send search and filter query params now, and the later backend Specifications PR can make those params affect results.

Broader admin functionality, including user CRUD screens, should be designed and implemented separately so the TMDB admin panel does not expand into a general admin dashboard in this PR.
