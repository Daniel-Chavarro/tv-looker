# Frontend Knowledge

## Overview
- Standalone Vite/React 19 SPA in `frontend/`; run npm commands here, not at repo root.
- Manual `BrowserRouter` routes live in `src/App.tsx`; do not use React Router framework/file-route commands unless deliberately migrating.
- Data flow is Axios API module (`src/api/*`) -> React Query hook (`src/hooks/use*.ts`) -> route page (`src/pages/*`).

## Where to look
| Task | Location | Notes |
|------|----------|-------|
| Routes/providers | `src/App.tsx` | Routes include `/login`, `/register`, `/profile`, `/lists`, `/reviews`, `/recommendations`, `/admin/tmdb` |
| Auth/session | `src/contexts/AuthContext.tsx`, `src/api/client.ts` | Token/user in localStorage; 401 clears session and redirects to `/login` |
| Route guard | `src/components/ProtectedRoute.tsx` | `requiredAuthority="ADMIN"` renders inline Forbidden for logged-in non-admins |
| Header nav | `src/components/layout/Header.tsx` | Profile link for auth users; Admin link only for `ADMIN` |
| Catalog | `src/pages/Home.tsx`, `src/api/items.ts`, `src/components/features/PaginationControls.tsx` | Search submits explicitly; filters requery immediately; fixed `size=20` |
| Profile | `src/pages/Profile.tsx`, `src/api/users.ts` | Uses `/me` self-profile APIs; edits `name` and `email`; no delete-account UI |
| TMDB admin | `src/pages/AdminTmdb.tsx`, `src/api/tmdbAdmin.ts`, `src/hooks/useTmdbAdmin.ts` | Admin status + collect/sync actions |
| Tests | `src/**/*.test.*`, `src/test/` | Vitest + jsdom; `src/test/render.tsx` wraps QueryClient/MemoryRouter |

## Commands
```bash
npm run dev       # Vite dev server; proxies /api to http://localhost:8080
npm run test:run  # Vitest once
npm test          # Vitest watch mode
npm run build     # tsc && vite build, output dist/
npm run lint      # eslint src --ext ts,tsx
npm run format    # prettier --write src
```

## Project-specific gotchas
- `src/api/client.ts` uses `baseURL: '/api/v1'`; Vite dev proxy handles `/api` locally, but deployment still needs backend/proxy support for `/api/v1/**`.
- Backend item pagination response is project `PageResponse`: `content`, `actualPage`, `totalPages`, `totalItems`, `isLast`; query params use `page` and `size`, not `pageSize`.
- `useItems` query key includes the full params object; create new params objects instead of mutating them in place.
- TMDB admin status cache key is `['admin', 'tmdb', 'status']`; collect/sync mutations invalidate it.
- Tailwind v4 is wired through `@tailwindcss/vite` and PostCSS; CSS imports `tailwindcss` directly.
- `Dockerfile` and `frontend/README.md` are stale template guidance; trust `package.json`, `vite.config.ts`, and source code first.
- GitHub CI does not run frontend checks; run local `test:run`, `build`, and `lint` for frontend changes.
