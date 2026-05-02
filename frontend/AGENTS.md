# Frontend Knowledge

## OVERVIEW
Standalone Vite/React 19 SPA with manual `BrowserRouter`, React Query hooks, Axios API modules, Tailwind v4, and no frontend CI coverage.

## STRUCTURE
```text
frontend/
|-- index.html              # Vite HTML entry
|-- src/main.tsx            # React root mount
|-- src/App.tsx             # Provider stack + route table
|-- src/api/                # Axios-backed endpoint modules
|-- src/contexts/           # Auth context
|-- src/hooks/              # React Query hooks over api modules
|-- src/pages/              # Route views
|-- src/components/         # layout, common primitives, feature cards/grids
|-- src/types/              # shared domain/API types
`-- src/utils/              # validators/formatters
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| App routes | `src/App.tsx` | Manual `BrowserRouter`; not file-based routing |
| Auth state | `src/contexts/AuthContext.tsx` | Stores auth state and localStorage token |
| Route guard | `src/components/ProtectedRoute.tsx` | Redirects unauthenticated users to `/login` |
| HTTP client | `src/api/client.ts` | Axios base URL, bearer header, 401 redirect |
| Endpoint wrappers | `src/api/*.ts` | One module per backend resource |
| Data hooks | `src/hooks/use*.ts` | React Query keys and invalidation live here |
| Page UI | `src/pages/*.tsx` | Detail pages are state/modals/action heavy |
| Shared UI | `src/components/common/` | Button/Card/Input/Modal/Loader/Error/Rating |
| Types | `src/types/index.ts` | Barrel for API/hooks/context imports |

## CONVENTIONS
- Run all frontend commands from `frontend/`; no root npm workspace exists.
- `npm run build` is `tsc && vite build`; output is `dist`.
- `npm run lint` uses `eslint src --ext ts,tsx`; no separate ESLint config file was found.
- TypeScript is strict, `noEmit`, ES2022, `moduleResolution: bundler`.
- API modules import the shared `apiClient`; hooks wrap API modules with React Query.
- Tailwind v4 is wired through both `@tailwindcss/vite` and `@tailwindcss/postcss`; CSS imports `tailwindcss` directly.

## ANTI-PATTERNS
- `src/api/client.ts` hardcodes `baseURL: '/api'`, while backend routes are `/api/v1/**`; verify proxy/prefix before assuming calls work.
- `src/App.tsx` routes `/login` and `/register` to placeholder divs even though `src/pages/auth/Login.tsx` and `Register.tsx` exist.
- Header/Footer links include routes like `/my-lists`, `/my-reviews`, and `/search`; `App.tsx` uses `/lists`, `/reviews`, and has no `/search`.
- Some pages use raw `<a href>` instead of React Router `Link`, causing full reloads.
- `AuthContext.tsx` stores `response.id` as the token; confirm backend auth response before touching auth flow.
- `react-router.config.ts` says `ssr: true`, but actual app uses `BrowserRouter`; don't run React Router framework commands unless migrating.
- `Dockerfile` is stale: copies `build` and runs `npm run start`, but Vite outputs `dist` and package has no `start`.
- `frontend/README.md` is template documentation; avoid copying its deployment assumptions.

## COMMANDS
```bash
npm run dev
npm run build
npm run lint
npm run format
npm run preview
```

## NOTES
- There is no `npm test` script and no Vitest/Jest/Cypress/Playwright config detected.
- CI does not run frontend build or lint; run local checks for any frontend changes.
