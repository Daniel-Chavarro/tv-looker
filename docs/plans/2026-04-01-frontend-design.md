# TV Looker Frontend Design

**Date:** 2026-04-01  
**Status:** Approved  
**Type:** Frontend Application Design

## Overview

This document outlines the design for the TV Looker frontend - a React-based web application for a movie and TV series recommendation engine. The frontend will consume the existing Spring Boot REST API and provide a clean, minimal user interface for browsing content, managing favorite lists, writing reviews, and receiving personalized recommendations.

## Goals

- Build a modern, responsive frontend for both regular users and administrators
- Provide seamless integration with existing REST API endpoints
- Enable personalized movie/TV recommendations
- Support user authentication, favorite lists, and reviews
- Maintain a clean, information-focused design aesthetic
- Use Docker for easy development setup alongside the backend

## Non-Goals

- Streaming video content (this is a recommendation platform only)
- Backend modifications beyond CORS and authentication endpoints
- Mobile native applications (web-responsive only)
- Admin panel (future enhancement)

## Technical Stack

| Category | Technology | Rationale |
|----------|------------|-----------|
| Build Tool | Vite | Fast dev server, modern tooling, better DX than CRA |
| Framework | React 18 | Component-based, large ecosystem, team familiarity |
| Language | TypeScript | Type safety complements Java backend, prevents runtime errors |
| Routing | React Router v6 | Industry standard, declarative routing |
| State Management | Context API + TanStack Query | Built-in React + powerful server state management |
| HTTP Client | Axios | Promise-based, interceptors for auth/error handling |
| Styling | Tailwind CSS | Rapid development, clean minimal aesthetic, utility-first |
| Deployment | Vercel/Netlify (Frontend), Existing (Backend) | Separate deployments, easy CI/CD, static hosting |

## Architecture

### High-Level Architecture

```
┌─────────────────┐         ┌──────────────────┐
│  React Frontend │ ◄─────► │  Spring Boot API │
│   (Vite + TS)   │  HTTP   │   (Port 8080)    │
│   Port 5173     │  CORS   │                  │
└─────────────────┘         └──────────────────┘
        │                            │
        │                            │
   Static Host              ┌────────▼────────┐
  (Vercel/Netlify)          │   PostgreSQL    │
                            │   (Docker)      │
                            └─────────────────┘
```

### Project Structure

```
tv-looker/
├── src/                        # Existing Spring Boot backend
├── frontend/                   # New React application
│   ├── src/
│   │   ├── main.tsx                 # App entry point
│   │   ├── App.tsx                  # Root component with router
│   │   ├── api/                     # API client & endpoints
│   │   │   ├── client.ts            # Axios instance
│   │   │   ├── items.ts
│   │   │   ├── recommendations.ts
│   │   │   ├── reviews.ts
│   │   │   ├── lists.ts
│   │   │   └── auth.ts
│   │   ├── components/              # Reusable UI components
│   │   │   ├── common/              # Buttons, inputs, cards
│   │   │   ├── layout/              # Header, footer, sidebar
│   │   │   └── features/            # Feature-specific components
│   │   ├── pages/                   # Route-level pages
│   │   │   ├── Home.tsx
│   │   │   ├── ItemDetail.tsx
│   │   │   ├── Recommendations.tsx
│   │   │   ├── MyLists.tsx
│   │   │   ├── Profile.tsx
│   │   │   └── Auth/
│   │   ├── contexts/                # React Context providers
│   │   │   └── AuthContext.tsx
│   │   ├── hooks/                   # Custom React hooks
│   │   │   ├── useAuth.ts
│   │   │   ├── useItems.ts
│   │   │   └── useRecommendations.ts
│   │   ├── types/                   # TypeScript type definitions
│   │   │   ├── item.ts
│   │   │   ├── user.ts
│   │   │   ├── review.ts
│   │   │   └── api.ts
│   │   └── utils/                   # Helper functions
│   ├── public/                      # Static assets
│   ├── index.html
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── tsconfig.json
│   ├── package.json
│   └── Dockerfile
├── docker-compose.yml          # Database + Frontend
├── pom.xml
└── README.md
```

**Architectural Principles:**
- **Separation of concerns**: API layer separate from UI components
- **Type safety**: Mirror backend DTOs as TypeScript interfaces
- **Feature-based organization**: Group related functionality together
- **Custom hooks**: Encapsulate TanStack Query logic for reusability

## Core Features

### Public Pages (Unauthenticated)
1. **Landing/Home Page** - Browse popular items, trending recommendations
2. **Item Detail Page** - View movie/series details, cast, genres, reviews (read-only)
3. **Login/Register Pages** - Session-based authentication

### Authenticated User Pages
1. **Personalized Recommendations** (`/recommendations`) - AI-powered suggestions
2. **My Lists** (`/lists`) - View/create/edit favorite lists, add/remove items
3. **My Reviews** (`/reviews`) - View user's own reviews, edit/delete
4. **Profile** (`/profile`) - View/edit user information

### Key User Flows

**Flow 1: Discovering & Saving Content**
```
Browse Items → View Detail → Add to Favorite List → See in "My Lists"
                          ↓
                    Write Review & Rating
```

**Flow 2: Getting Recommendations**
```
Login → Navigate to Recommendations → View Personalized Items → 
Click Item → View Details → Add to List or Review
```

**Flow 3: Managing Lists**
```
My Lists → Create New List → Browse Items → Add to List → 
View List → Remove Items or Delete List
```

## State Management

### State Categories

**1. Server State (TanStack Query)**
- All data from REST API (items, recommendations, reviews, lists)
- Automatic caching, background refetching, optimistic updates
- Custom hooks per resource: `useItems()`, `useRecommendations()`, `useReviews()`

**2. Authentication State (Context API)**
- Current user session (user object, logged-in status)
- Shared via `AuthContext`, consumed with `useAuth()` hook
- Persisted via cookies (Spring Session handles backend)

**3. Local UI State (Component State)**
- Form inputs, modals, dropdowns
- Kept local with `useState` - no global state needed

### Data Flow Pattern

```
Component
   ↓ (calls hook)
Custom Hook (useItems, useRecommendations)
   ↓ (uses TanStack Query)
TanStack Query (cache, fetch, invalidate)
   ↓ (HTTP request)
API Client (axios instance)
   ↓ (REST call)
Spring Boot Controller
```

### Example: Fetching Recommendations

```typescript
// hooks/useRecommendations.ts
export function useRecommendations(userId: string, limit = 10) {
  return useQuery({
    queryKey: ['recommendations', userId, limit],
    queryFn: () => recommendationsApi.getForUser(userId, limit),
    enabled: !!userId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// In component:
const { data, isLoading, error } = useRecommendations(user.id);
```

### Authentication Flow

```
User submits login → POST /api/v1/auth/login → 
Backend sets session cookie → Frontend stores user in AuthContext → 
All subsequent requests include cookie automatically → 
Logout clears context and calls POST /api/v1/auth/logout
```

## API Integration

### API Client Setup

```typescript
// api/client.ts
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  withCredentials: true, // Include cookies
  headers: {
    'Content-Type': 'application/json',
  },
});

// Error handling interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Redirect to login
    }
    return Promise.reject(error);
  }
);
```

### TypeScript Type Definitions

Mirror backend DTOs:

```typescript
// types/item.ts
export interface Item {
  id: number;
  title: string;
  type: 'MOVIE' | 'SERIES';
  releaseDate: string;
  synopsis: string;
  posterUrl?: string;
  tmdbId: number;
  genres: Genre[];
  actors: Actor[];
  directors: Director[];
  averageRating?: number;
}

// types/review.ts
export interface Review {
  id: number;
  userId: string;
  itemId: number;
  rating: number;
  content: string;
  createdAt: string;
  updatedAt: string;
}

// types/list.ts
export interface FavoriteList {
  id: number;
  userId: string;
  name: string;
  description?: string;
  items: Item[];
  createdAt: string;
}
```

### API Endpoint Mapping

| Backend Controller | Frontend API Module | Key Methods |
|--------------------|---------------------|-------------|
| `ItemController` | `api/items.ts` | `getAll()`, `getById(id)` |
| `RecommendationController` | `api/recommendations.ts` | `getForUser(userId, limit)` |
| `ReviewController` | `api/reviews.ts` | `getAll()`, `create()`, `update()`, `delete()` |
| `ListFavoriteController` | `api/lists.ts` | `getAll()`, `create()`, `addItem()`, `removeItem()` |
| `UserController` | `api/users.ts` | `getById()`, `update()`, `delete()` |
| `AuthController` (new) | `api/auth.ts` | `login()`, `register()`, `logout()`, `me()` |

## UI Design & Components

### Design System

**Color Palette** (Clean & Minimal):
- Primary: Blue/Indigo for actions and links
- Neutral: Grays for text and backgrounds
- Success/Error: Green/Red for feedback
- Light theme default (dark mode potential future enhancement)

**Typography**:
- Sans-serif font family (Inter, system-ui)
- 16px base font size
- Clear hierarchy with headings and whitespace

### Core Component Library

**Layout Components**:
- `Header` - Logo, navigation, user menu, search
- `Footer` - Links, copyright
- `Sidebar` - Optional filtering (genres, ratings)
- `Container` - Max-width content wrapper

**Common UI Components**:
- `Button` - Primary, secondary, ghost variants
- `Card` - For items, lists
- `Input` / `TextArea` - Form controls
- `Modal` - Confirmations, forms
- `Rating` - Star/numeric display and input
- `Loader` - Loading states
- `ErrorMessage` - Error display

**Feature Components**:
- `ItemCard` - Movie/series card with poster, title, rating
- `ItemGrid` - Responsive grid of ItemCards
- `ReviewCard` - Review display with user, rating, content
- `ListCard` - Favorite list with preview items
- `RecommendationSection` - Personalized recommendations

### Responsive Design

- Mobile-first approach with Tailwind breakpoints
- Grid layouts: 1 column (mobile) → 2-3 (tablet) → 4-6 (desktop)
- Hamburger menu for mobile navigation

## Routing & Navigation

### Route Structure

```typescript
<BrowserRouter>
  <Routes>
    <Route path="/" element={<Layout />}>
      {/* Public Routes */}
      <Route index element={<HomePage />} />
      <Route path="items/:id" element={<ItemDetailPage />} />
      <Route path="login" element={<LoginPage />} />
      <Route path="register" element={<RegisterPage />} />
      
      {/* Protected Routes */}
      <Route element={<ProtectedRoute />}>
        <Route path="recommendations" element={<RecommendationsPage />} />
        <Route path="lists" element={<MyListsPage />} />
        <Route path="lists/:id" element={<ListDetailPage />} />
        <Route path="reviews" element={<MyReviewsPage />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>
      
      <Route path="*" element={<NotFoundPage />} />
    </Route>
  </Routes>
</BrowserRouter>
```

### URL Patterns

| Route | Purpose | Auth Required |
|-------|---------|---------------|
| `/` | Home/browse items | No |
| `/items/:id` | Item detail page | No |
| `/login` | Login form | No |
| `/register` | Registration form | No |
| `/recommendations` | Personalized recommendations | Yes |
| `/lists` | User's favorite lists | Yes |
| `/lists/:id` | Specific list detail | Yes |
| `/reviews` | User's reviews | Yes |
| `/profile` | User profile & settings | Yes |

### Navigation Menu

**Unauthenticated:**
- Home
- Browse Items
- Login / Register

**Authenticated:**
- Home
- Recommendations
- My Lists
- My Reviews
- Profile (dropdown)
- Logout (dropdown)

## Error Handling & Loading States

### Error Handling Strategy

**API Error Handling**:
- Axios interceptor catches all HTTP errors
- User-friendly messages (not raw error codes)
- Status-specific handling:
  - `401 Unauthorized` → Redirect to login
  - `403 Forbidden` → "Access denied" message
  - `404 Not Found` → "Item not found" message
  - `500 Server Error` → "Something went wrong, try again"

**Component-Level Errors**:
```typescript
const { data, isLoading, error } = useItems();

if (error) {
  return <ErrorMessage message="Failed to load items. Please try again." />;
}
```

**Form Validation**:
- Client-side validation before submission
- Display inline validation errors
- Show backend validation errors from API response

### Loading States

**Page-Level Loading**:
- Skeleton screens or spinners during initial data load
- Maintains layout to prevent content shift

**Action Loading**:
- Disable buttons and show spinner during mutations
- Optimistic updates where appropriate

**TanStack Query Patterns**:
```typescript
const { data, isLoading, isFetching, isError } = useQuery(...);

// isLoading: Initial fetch (show skeleton)
// isFetching: Background refetch (subtle indicator)
// isError: Show error UI
```

### User Feedback

- **Toast notifications** for success/error messages
- **Confirmation modals** for destructive actions (delete review, delete list)
- **Empty states** with helpful messaging ("No lists yet. Create your first one!")

## Development & Deployment

### Project Structure

```
tv-looker/
├── src/                        # Existing Spring Boot backend
├── frontend/                   # New React app
│   ├── src/
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile
├── docker-compose.yml          # Database + Frontend
├── pom.xml
└── README.md
```

### Docker Setup

**docker-compose.yml** (Updated):
```yaml
services:
  postgres:
    image: postgres:latest
    container_name: postgres-tv-looker
    restart: always
    environment:
      POSTGRES_USER: tv-looker-admin
      POSTGRES_PASSWORD: this-is-a-super-secure-password123
      POSTGRES_DB: tv-looker-db
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      target: development
    container_name: frontend-tv-looker
    restart: always
    ports:
      - "5173:5173"
    environment:
      VITE_API_URL: http://localhost:8080/api/v1
    volumes:
      - ./frontend:/app
      - /app/node_modules

volumes:
  postgres-data:
```

**frontend/Dockerfile**:
```dockerfile
# Development
FROM node:20-alpine as development
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

# Production build
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production serve
FROM nginx:alpine as production
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Development Workflow

**Quick Start:**
```bash
# Start database + frontend (Docker)
docker-compose up -d

# Run backend with Maven (separate terminal)
mvn spring-boot:run

# Access URLs:
# Frontend: http://localhost:5173
# Backend:  http://localhost:8080
# Database: localhost:5432
```

**Alternative (No Docker for Frontend):**
```bash
# Start database only
docker-compose up postgres -d

# Run backend
mvn spring-boot:run

# Run frontend (in frontend/ directory)
cd frontend && npm run dev
```

### Environment Variables

```bash
# .env.development
VITE_API_URL=http://localhost:8080/api/v1

# .env.production
VITE_API_URL=https://your-backend-domain.com/api/v1
```

### Backend Changes Required

1. **CORS Configuration** - Allow `http://localhost:5173` origin:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "https://your-frontend-domain.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true);
    }
}
```

2. **Authentication Endpoints** - Create `AuthController`:
   - `POST /api/v1/auth/login` - Login endpoint
   - `POST /api/v1/auth/register` - Registration endpoint
   - `POST /api/v1/auth/logout` - Logout endpoint
   - `GET /api/v1/auth/me` - Get current user info

3. **Spring Session Configuration** - Ensure cookie settings work cross-origin

### Deployment Strategy

**Frontend (MVP/Development):**
- Deploy to Vercel/Netlify (easiest for static sites)
- Connect GitHub repository for auto-deploy
- Set `VITE_API_URL` environment variable

**Backend:**
- Continue existing deployment strategy
- Add production frontend domain to CORS config

**Future (Production):**
- Both services dockerized with docker-compose
- Deploy to cloud (AWS ECS, DigitalOcean, Railway)

## Testing Considerations

### Manual Testing Checklist
- Registration and login flow
- Browse items and view details
- Get personalized recommendations
- Create/edit/delete lists
- Write/edit/delete reviews
- Add/remove items from lists
- Profile editing

### Future Automated Testing (Post-MVP)
- **Unit Tests**: Vitest for utility functions
- **Component Tests**: React Testing Library
- **E2E Tests**: Playwright for critical user flows

## Authentication Design

### Session-Based Authentication (MVP)

**Implementation:**
- Spring Session (already in `pom.xml`) handles server-side sessions
- Session stored in PostgreSQL via JDBC
- Frontend receives session cookie automatically
- Cookie sent with every request via `withCredentials: true`

**Frontend Flow:**
```typescript
// Login
const response = await authApi.login(email, password);
// Backend sets cookie automatically

// Subsequent requests
const items = await itemsApi.getAll();
// Cookie included automatically

// Logout
await authApi.logout();
// Backend clears session
```

### Future Enhancement: JWT + OAuth

- Design allows easy migration to JWT tokens
- Store token in `httpOnly` cookie or `localStorage`
- Add OAuth providers (Google, GitHub) via Spring Security OAuth2

## Security Considerations

- **CSRF Protection**: Spring Security CSRF tokens for state-changing operations
- **XSS Prevention**: React automatically escapes content, use `dangerouslySetInnerHTML` sparingly
- **Input Validation**: Client-side + server-side validation
- **Secure Cookies**: `httpOnly`, `secure`, `sameSite` flags in production
- **Rate Limiting**: Backend should implement rate limiting for auth endpoints

## Future Enhancements

### Post-MVP Features
- Admin panel for content/user management
- Dark mode toggle
- Search and advanced filtering (by genre, rating, year)
- Social features (follow users, share lists)
- Email notifications for recommendations
- PWA capabilities (offline support, install prompt)

### Technical Improvements
- Automated testing suite (unit, integration, E2E)
- Performance monitoring (Web Vitals)
- Analytics integration
- Internationalization (i18n)
- Image optimization and lazy loading

## Success Metrics

- **User Engagement**: Time spent on recommendations page, items added to lists
- **Performance**: < 2s page load time, < 100ms API response time
- **Quality**: < 5% error rate on API calls
- **User Satisfaction**: Feedback from testing with real users

## Open Questions

- Should we implement search in MVP or defer to post-MVP?
- Do we need pagination for item lists, or is infinite scroll preferred?
- What's the maximum number of items allowed per favorite list?

## Conclusion

This design provides a comprehensive blueprint for building the TV Looker frontend as a modern, type-safe React application that integrates seamlessly with the existing Spring Boot backend. The architecture prioritizes developer experience, maintainability, and user experience while keeping deployment simple and scalable.

The design balances MVP speed (session-based auth, essential features only) with future extensibility (JWT/OAuth migration path, admin panel, advanced features). The Docker-based development setup ensures team members can start contributing quickly without complex environment configuration.
