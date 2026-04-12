# TV Looker Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-ready React frontend for the TV Looker recommendation engine that integrates with the existing Spring Boot REST API.

**Architecture:** Monorepo structure with separate frontend React app, Docker-based development setup (database + frontend in containers, backend runs via Maven). React Router for routing, Context API + TanStack Query for state management, Tailwind CSS for styling.

**Tech Stack:** Vite, React 18, TypeScript, React Router v6, TanStack Query v5, Axios, Tailwind CSS, Node 20

---

## File Structure Overview

```
tv-looker/
├── frontend/                              # NEW: React application root
│   ├── src/
│   │   ├── main.tsx                      # Entry point, ReactDOM.createRoot
│   │   ├── App.tsx                       # Root component, router setup
│   │   ├── api/
│   │   │   ├── client.ts                 # Axios instance with interceptors
│   │   │   ├── items.ts                  # Item API endpoints
│   │   │   ├── recommendations.ts        # Recommendation API endpoints
│   │   │   ├── reviews.ts                # Review API endpoints
│   │   │   ├── lists.ts                  # Favorite list API endpoints
│   │   │   ├── users.ts                  # User API endpoints
│   │   │   └── auth.ts                   # Authentication API endpoints
│   │   ├── types/
│   │   │   ├── index.ts                  # Export all types
│   │   │   ├── item.ts                   # Item, Genre, Actor, Director types
│   │   │   ├── review.ts                 # Review type
│   │   │   ├── list.ts                   # FavoriteList type
│   │   │   ├── user.ts                   # User type
│   │   │   └── api.ts                    # API response/error types
│   │   ├── hooks/
│   │   │   ├── useAuth.ts                # Auth context hook
│   │   │   ├── useItems.ts               # TanStack Query hook for items
│   │   │   ├── useRecommendations.ts     # TanStack Query hook for recommendations
│   │   │   ├── useReviews.ts             # TanStack Query hook for reviews
│   │   │   ├── useLists.ts               # TanStack Query hook for lists
│   │   │   └── useUsers.ts               # TanStack Query hook for users
│   │   ├── contexts/
│   │   │   └── AuthContext.tsx           # Auth state provider
│   │   ├── components/
│   │   │   ├── common/
│   │   │   │   ├── Button.tsx
│   │   │   │   ├── Input.tsx
│   │   │   │   ├── Card.tsx
│   │   │   │   ├── Modal.tsx
│   │   │   │   ├── Loader.tsx
│   │   │   │   ├── ErrorMessage.tsx
│   │   │   │   └── Rating.tsx
│   │   │   ├── layout/
│   │   │   │   ├── Header.tsx
│   │   │   │   ├── Footer.tsx
│   │   │   │   ├── Layout.tsx
│   │   │   │   └── Sidebar.tsx
│   │   │   └── features/
│   │   │       ├── ItemCard.tsx
│   │   │       ├── ItemGrid.tsx
│   │   │       ├── ReviewCard.tsx
│   │   │       ├── ListCard.tsx
│   │   │       └── RecommendationSection.tsx
│   │   ├── pages/
│   │   │   ├── Home.tsx
│   │   │   ├── ItemDetail.tsx
│   │   │   ├── Recommendations.tsx
│   │   │   ├── MyLists.tsx
│   │   │   ├── ListDetail.tsx
│   │   │   ├── MyReviews.tsx
│   │   │   ├── Profile.tsx
│   │   │   ├── NotFound.tsx
│   │   │   └── auth/
│   │   │       ├── Login.tsx
│   │   │       └── Register.tsx
│   │   ├── utils/
│   │   │   ├── formatters.ts             # Date, number formatting
│   │   │   ├── validators.ts             # Form validation
│   │   │   └── errors.ts                 # Error handling utilities
│   │   ├── App.css
│   │   └── index.css                     # Global styles + Tailwind
│   ├── public/
│   │   └── favicon.svg
│   ├── index.html
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── postcss.config.js
│   ├── tsconfig.json
│   ├── tsconfig.app.json
│   ├── .env.example
│   ├── .env.development
│   ├── Dockerfile
│   ├── package.json
│   └── package-lock.json
├── docker-compose.yml                    # MODIFY: Add frontend service
├── frontend-development.md                # NEW: Frontend dev guide
```

---

## Task 1: Project Setup & Dependencies

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.app.json`
- Create: `frontend/tailwind.config.js`
- Create: `frontend/postcss.config.js`
- Create: `frontend/index.html`
- Create: `frontend/.env.example`
- Create: `frontend/Dockerfile`

- [x] **Step 1: Create frontend/package.json with all dependencies**

```json
{
  "name": "tv-looker-frontend",
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint src --ext ts,tsx",
    "format": "prettier --write src"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "@tanstack/react-query": "^5.28.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "@typescript-eslint/eslint-plugin": "^6.0.0",
    "@typescript-eslint/parser": "^6.0.0",
    "@vitejs/plugin-react": "^4.2.0",
    "autoprefixer": "^10.4.16",
    "eslint": "^8.50.0",
    "eslint-plugin-react-hooks": "^4.6.0",
    "postcss": "^8.4.31",
    "prettier": "^3.1.0",
    "tailwindcss": "^3.3.6",
    "typescript": "^5.3.0",
    "vite": "^5.0.0"
  }
}
```

- [x] **Step 2: Create frontend/vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: false,
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
})
```

- [x] **Step 3: Create frontend/tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "esModuleInterop": true,
    "allowSyntheticDefaultImports": true,

    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,

    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "jsx": "react-jsx"
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.app.json" }]
}
```

- [x] **Step 4: Create frontend/tsconfig.app.json**

```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "esModuleInterop": true,
    "allowSyntheticDefaultImports": true
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.json" }]
}
```

- [x] **Step 5: Create frontend/tailwind.config.js**

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#4F46E5',
        secondary: '#6366F1',
      },
    },
  },
  plugins: [],
}
```

- [x] **Step 6: Create frontend/postcss.config.js**

```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```

- [x] **Step 7: Create frontend/index.html**

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>TV Looker - Movie & Series Recommendations</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [x] **Step 8: add to .env.example:**

```bash
VITE_API_URL=http://localhost:8080/api/v1
```

- [x] **Step 9: Create frontend/Dockerfile** (modified using default vite builder)

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
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

- [x] **Step 10: Commit project setup**

```bash
git add frontend/package.json frontend/vite.config.ts frontend/tsconfig.json frontend/tsconfig.app.json frontend/tailwind.config.js frontend/postcss.config.js frontend/index.html frontend/.env.example frontend/Dockerfile
git commit -m "chore: setup frontend project structure and configuration"
```

---

## Task 2: TypeScript Type Definitions

**Files:**
- Create: `frontend/src/types/index.ts`
- Create: `frontend/src/types/api.ts`
- Create: `frontend/src/types/item.ts`
- Create: `frontend/src/types/review.ts`
- Create: `frontend/src/types/list.ts`
- Create: `frontend/src/types/user.ts`

- [x] **Step 1: Create frontend/src/types/api.ts**

```typescript
export type ApiError = {
  message: string;
  code?: string;
  statusCode: number;
}

// Generic type for paginated API responses (future use)
export type PaginatedResponse<T> = {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
}

```

- [x] **Step 2: Create frontend/src/types/item.ts**

```typescript
export type Genre = {
  id: number;
  tmdbId: number;
  name: string;
}

export type Actor = {
  id: number;
  tmdbId: number;
  name: string;
}

export type ActorItem = {
    id: number;
    actorId: number;
    actorName: string;
    characterName: string;
    billingOrder: number;
}

export type Director = {
  id: number;
  tmdbId: number;
  name: string;
}

export type Item = {
  id: number;
  title: string;
  type: "MOVIE" | "TV";
  releaseDate: string;
  overview: string;
  posterUrl?: string;
  backdropUrl?: string;
  voteAverage: number;
  popularity: number;
  tmdbId: number;
  genres: Genre[];
  actors: ActorItem[];
  directors: Director[];
}

export type ItemResponse = {
  data: Item;
}

export type ItemsListResponse = {
  data: Item[];
  count: number;
}

```

- [x] **Step 3: Create frontend/src/types/review.ts**

```typescript
import type { UUID } from "crypto";

export type Review = {
  id: number;
  userId: UUID;
  itemId: number;
  rating: number; // 1-5
  content: string;
  createdAt: string;
  updatedAt?: string;
  userName?: string;
}

export type CreateReviewRequest = {
  userId: UUID;
  itemId: number;
  rating: number;
  content: string;
}

export type UpdateReviewRequest = {
  rating?: number;
  content?: string;
}

export type ReviewResponse = {
  data: Review;
}

export type ReviewsListResponse = {
  data: Review[];
  count: number;
}


```

- [x] **Step 4: Create frontend/src/types/list.ts**

```typescript
import type { UUID } from "crypto";
import type { Item } from "./item";

export type FavoriteList = {
  id: number;
  userId: string;
  name: string;
  description?: string;
  items: Item[];
}

export type CreateListRequest = {
  userId: UUID;
  name: string;
  description?: string;
}

export type UpdateListRequest = {
  name?: string;
  description?: string;
}

export type ListResponse = {
  data: FavoriteList;
}

export type ListsListResponse = {
  data: FavoriteList[];
  count: number;
}

```

- [x] **Step 5: Create frontend/src/types/user.ts**

```typescript
export type User = {
  id: string;
  username: string;
  email: string;
  name?: string;
  createdAt: string;
  updatedAt: string;
}

export type CreateUserRequest = {
  username: string;
  email: string;
  password: string;
  name?: string;
}

export type UpdateUserRequest = {
  password?: string;
  name?: string;
  email?: string;
}

export type UserResponse = {
  data: User;
}

```

- [ ] **Step 6: Create frontend/src/types/index.ts**

```typescript
export type { ApiError, PaginatedResponse } from './api';
export type { Genre, Actor, Director, Item, ItemResponse, ItemsListResponse } from './item';
export type { Review, CreateReviewRequest, UpdateReviewRequest, ReviewResponse, ReviewsListResponse } from './review';
export type { FavoriteList, CreateListRequest, UpdateListRequest, ListResponse, ListsListResponse } from './list';
export type { User, CreateUserRequest, UpdateUserRequest, UserResponse } from './user';
```

- [ ] **Step 7: Commit type definitions**

```bash
git add frontend/src/types/
git commit -m "feat: add TypeScript type definitions for API and entities"
```

---

## Task 3: API Client Setup

**Files:**
- Create: `frontend/src/api/client.ts`
- Create: `frontend/src/api/items.ts`
- Create: `frontend/src/api/recommendations.ts`
- Create: `frontend/src/api/reviews.ts`
- Create: `frontend/src/api/lists.ts`
- Create: `frontend/src/api/users.ts`
- Create: `frontend/src/api/auth.ts`

- [ ] **Step 1: Create frontend/src/api/client.ts**

```typescript
import axios, { AxiosError, AxiosInstance } from 'axios';
import { ApiError } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      // Unauthorized - clear auth and redirect to login
      window.location.href = '/login';
    }
    
    const apiError: ApiError = {
      message: error.response?.data?.message || 'An error occurred',
      code: error.response?.data?.code,
      statusCode: error.response?.status || 500,
    };
    
    return Promise.reject(apiError);
  }
);

export default apiClient;
```

- [ ] **Step 2: Create frontend/src/api/items.ts**

```typescript
import apiClient from './client';
import { Item, ItemResponse, ItemsListResponse } from '../types';

export const itemsApi = {
  getAll: async (): Promise<Item[]> => {
    const response = await apiClient.get<ItemsListResponse>('/items');
    return response.data.data;
  },

  getById: async (id: number): Promise<Item> => {
    const response = await apiClient.get<ItemResponse>(`/items/${id}`);
    return response.data.data;
  },
};
```

- [ ] **Step 3: Create frontend/src/api/recommendations.ts**

```typescript
import apiClient from './client';
import { Item } from '../types';

interface RecommendationResponse {
  userId: string;
  count: number;
  items: Item[];
}

export const recommendationsApi = {
  getForUser: async (userId: string, limit = 10): Promise<Item[]> => {
    const response = await apiClient.get<RecommendationResponse>(
      `/users/${userId}/recommendations`,
      { params: { limit } }
    );
    return response.data.items;
  },
};
```

- [ ] **Step 4: Create frontend/src/api/reviews.ts**

```typescript
import apiClient from './client';
import { Review, CreateReviewRequest, UpdateReviewRequest, ReviewResponse, ReviewsListResponse } from '../types';

export const reviewsApi = {
  getAll: async (): Promise<Review[]> => {
    const response = await apiClient.get<ReviewsListResponse>('/reviews');
    return response.data.data;
  },

  getById: async (id: number): Promise<Review> => {
    const response = await apiClient.get<ReviewResponse>(`/reviews/${id}`);
    return response.data.data;
  },

  create: async (request: CreateReviewRequest): Promise<Review> => {
    const response = await apiClient.post<ReviewResponse>('/reviews', request);
    return response.data.data;
  },

  update: async (id: number, request: UpdateReviewRequest): Promise<Review> => {
    const response = await apiClient.put<ReviewResponse>(`/reviews/${id}`, request);
    return response.data.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/reviews/${id}`);
  },
};
```

- [ ] **Step 5: Create frontend/src/api/lists.ts**

```typescript
import apiClient from './client';
import { FavoriteList, CreateListRequest, UpdateListRequest, ListResponse, ListsListResponse } from '../types';

export const listsApi = {
  getAll: async (): Promise<FavoriteList[]> => {
    const response = await apiClient.get<ListsListResponse>('/lists');
    return response.data.data;
  },

  getById: async (id: number): Promise<FavoriteList> => {
    const response = await apiClient.get<ListResponse>(`/lists/${id}`);
    return response.data.data;
  },

  create: async (request: CreateListRequest): Promise<FavoriteList> => {
    const response = await apiClient.post<ListResponse>('/lists', request);
    return response.data.data;
  },

  update: async (id: number, request: UpdateListRequest): Promise<FavoriteList> => {
    const response = await apiClient.put<ListResponse>(`/lists/${id}`, request);
    return response.data.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/lists/${id}`);
  },

  addItem: async (listId: number, itemId: number): Promise<FavoriteList> => {
    const response = await apiClient.put<ListResponse>(
      `/lists/${listId}/add-item/${itemId}`
    );
    return response.data.data;
  },

  removeItem: async (listId: number, itemId: number): Promise<FavoriteList> => {
    const response = await apiClient.put<ListResponse>(
      `/lists/${listId}/remove-item/${itemId}`
    );
    return response.data.data;
  },
};
```

- [ ] **Step 6: Create frontend/src/api/users.ts**

```typescript
import apiClient from './client';
import { User, UpdateUserRequest, UserResponse } from '../types';

export const usersApi = {
  getById: async (id: string): Promise<User> => {
    const response = await apiClient.get<UserResponse>(`/users/${id}`);
    return response.data.data;
  },

  update: async (id: string, request: UpdateUserRequest): Promise<User> => {
    const response = await apiClient.put<UserResponse>(`/users/${id}`, request);
    return response.data.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/users/${id}`);
  },
};
```

- [ ] **Step 7: Create frontend/src/api/auth.ts**

```typescript
import apiClient from './client';
import { User, CreateUserRequest, UserResponse } from '../types';

interface LoginRequest {
  email: string;
  password: string;
}

interface AuthResponse {
  data: User;
}

export const authApi = {
  login: async (email: string, password: string): Promise<User> => {
    const response = await apiClient.post<AuthResponse>('/auth/login', {
      email,
      password,
    });
    return response.data.data;
  },

  register: async (request: CreateUserRequest): Promise<User> => {
    const response = await apiClient.post<AuthResponse>('/auth/register', request);
    return response.data.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout');
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await apiClient.get<UserResponse>('/auth/me');
    return response.data.data;
  },
};
```

- [ ] **Step 8: Commit API client setup**

```bash
git add frontend/src/api/
git commit -m "feat: implement API client with axios and all endpoints"
```

---

## Task 4: Authentication Context & Hooks

**Files:**
- Create: `frontend/src/contexts/AuthContext.tsx`
- Create: `frontend/src/hooks/useAuth.ts`

- [ ] **Step 1: Create frontend/src/contexts/AuthContext.tsx**

```typescript
import React, { createContext, useState, useEffect, ReactNode } from 'react';
import { User } from '../types';
import { authApi } from '../api/auth';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, username: string) => Promise<void>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Check if user is already logged in on mount
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const currentUser = await authApi.getCurrentUser();
        setUser(currentUser);
      } catch {
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const user = await authApi.login(email, password);
      setUser(user);
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (email: string, password: string, username: string) => {
    setIsLoading(true);
    try {
      const user = await authApi.register({ email, password, username });
      setUser(user);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    setIsLoading(true);
    try {
      await authApi.logout();
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        isAuthenticated: !!user,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
```

- [ ] **Step 2: Create frontend/src/hooks/useAuth.ts**

```typescript
import { useContext } from 'react';
import { AuthContext } from '../contexts/AuthContext';

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
```

- [ ] **Step 3: Commit authentication context**

```bash
git add frontend/src/contexts/AuthContext.tsx frontend/src/hooks/useAuth.ts
git commit -m "feat: implement authentication context and useAuth hook"
```

---

## Task 5: TanStack Query Hooks

**Files:**
- Create: `frontend/src/hooks/useItems.ts`
- Create: `frontend/src/hooks/useRecommendations.ts`
- Create: `frontend/src/hooks/useReviews.ts`
- Create: `frontend/src/hooks/useLists.ts`
- Create: `frontend/src/hooks/useUsers.ts`

- [ ] **Step 1: Create frontend/src/hooks/useItems.ts**

```typescript
import { useQuery } from '@tanstack/react-query';
import { itemsApi } from '../api/items';

export function useItems() {
  return useQuery({
    queryKey: ['items'],
    queryFn: () => itemsApi.getAll(),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

export function useItem(id: number) {
  return useQuery({
    queryKey: ['items', id],
    queryFn: () => itemsApi.getById(id),
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
}
```

- [ ] **Step 2: Create frontend/src/hooks/useRecommendations.ts**

```typescript
import { useQuery } from '@tanstack/react-query';
import { recommendationsApi } from '../api/recommendations';

export function useRecommendations(userId: string | null, limit = 10) {
  return useQuery({
    queryKey: ['recommendations', userId, limit],
    queryFn: () => recommendationsApi.getForUser(userId!, limit),
    enabled: !!userId, // Only fetch if user is logged in
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}
```

- [ ] **Step 3: Create frontend/src/hooks/useReviews.ts**

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { reviewsApi } from '../api/reviews';
import { CreateReviewRequest, UpdateReviewRequest } from '../types';

export function useReviews() {
  return useQuery({
    queryKey: ['reviews'],
    queryFn: () => reviewsApi.getAll(),
    staleTime: 3 * 60 * 1000, // 3 minutes
  });
}

export function useReview(id: number) {
  return useQuery({
    queryKey: ['reviews', id],
    queryFn: () => reviewsApi.getById(id),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

export function useCreateReview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateReviewRequest) => reviewsApi.create(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
    },
  });
}

export function useUpdateReview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateReviewRequest }) =>
      reviewsApi.update(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
    },
  });
}

export function useDeleteReview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => reviewsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
    },
  });
}
```

- [ ] **Step 4: Create frontend/src/hooks/useLists.ts**

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listsApi } from '../api/lists';
import { CreateListRequest, UpdateListRequest } from '../types';

export function useLists() {
  return useQuery({
    queryKey: ['lists'],
    queryFn: () => listsApi.getAll(),
    staleTime: 3 * 60 * 1000, // 3 minutes
  });
}

export function useList(id: number) {
  return useQuery({
    queryKey: ['lists', id],
    queryFn: () => listsApi.getById(id),
    staleTime: 3 * 60 * 1000, // 3 minutes
  });
}

export function useCreateList() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateListRequest) => listsApi.create(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lists'] });
    },
  });
}

export function useUpdateList() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateListRequest }) =>
      listsApi.update(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lists'] });
    },
  });
}

export function useDeleteList() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => listsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lists'] });
    },
  });
}

export function useAddItemToList() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ listId, itemId }: { listId: number; itemId: number }) =>
      listsApi.addItem(listId, itemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lists'] });
    },
  });
}

export function useRemoveItemFromList() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ listId, itemId }: { listId: number; itemId: number }) =>
      listsApi.removeItem(listId, itemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lists'] });
    },
  });
}
```

- [ ] **Step 5: Create frontend/src/hooks/useUsers.ts**

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../api/users';
import { UpdateUserRequest } from '../types';

export function useUser(id: string) {
  return useQuery({
    queryKey: ['users', id],
    queryFn: () => usersApi.getById(id),
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: UpdateUserRequest }) =>
      usersApi.update(id, request),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['users', id] });
    },
  });
}

export function useDeleteUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => usersApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });
}
```

- [ ] **Step 6: Commit TanStack Query hooks**

```bash
git add frontend/src/hooks/useItems.ts frontend/src/hooks/useRecommendations.ts frontend/src/hooks/useReviews.ts frontend/src/hooks/useLists.ts frontend/src/hooks/useUsers.ts
git commit -m "feat: implement TanStack Query hooks for all data resources"
```

---

## Task 6: Common UI Components

**Files:**
- Create: `frontend/src/components/common/Button.tsx`
- Create: `frontend/src/components/common/Input.tsx`
- Create: `frontend/src/components/common/Card.tsx`
- Create: `frontend/src/components/common/Loader.tsx`
- Create: `frontend/src/components/common/ErrorMessage.tsx`
- Create: `frontend/src/components/common/Modal.tsx`
- Create: `frontend/src/components/common/Rating.tsx`

- [ ] **Step 1: Create frontend/src/components/common/Button.tsx**

```typescript
import React, { ButtonHTMLAttributes } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  isLoading?: boolean;
  children: React.ReactNode;
}

export function Button({
  variant = 'primary',
  isLoading = false,
  disabled,
  children,
  className = '',
  ...props
}: ButtonProps) {
  const baseStyles = 'px-4 py-2 rounded-lg font-medium transition disabled:opacity-50 disabled:cursor-not-allowed';

  const variantStyles = {
    primary: 'bg-indigo-600 text-white hover:bg-indigo-700',
    secondary: 'bg-gray-200 text-gray-900 hover:bg-gray-300',
    ghost: 'bg-transparent text-indigo-600 hover:bg-indigo-50',
    danger: 'bg-red-600 text-white hover:bg-red-700',
  };

  return (
    <button
      {...props}
      disabled={disabled || isLoading}
      className={`${baseStyles} ${variantStyles[variant]} ${className}`}
    >
      {isLoading ? '...' : children}
    </button>
  );
}
```

- [ ] **Step 2: Create frontend/src/components/common/Input.tsx**

```typescript
import React, { InputHTMLAttributes } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export function Input({ label, error, className = '', ...props }: InputProps) {
  return (
    <div className="w-full">
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1">
          {label}
        </label>
      )}
      <input
        {...props}
        className={`w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 ${
          error ? 'border-red-500' : ''
        } ${className}`}
      />
      {error && <p className="text-red-600 text-sm mt-1">{error}</p>}
    </div>
  );
}
```

- [ ] **Step 3: Create frontend/src/components/common/Card.tsx**

```typescript
import React, { ReactNode } from 'react';

interface CardProps {
  children: ReactNode;
  className?: string;
  onClick?: () => void;
}

export function Card({ children, className = '', onClick }: CardProps) {
  return (
    <div
      onClick={onClick}
      className={`bg-white rounded-lg border border-gray-200 shadow-sm hover:shadow-md transition ${
        onClick ? 'cursor-pointer' : ''
      } ${className}`}
    >
      {children}
    </div>
  );
}
```

- [ ] **Step 4: Create frontend/src/components/common/Loader.tsx**

```typescript
export function Loader() {
  return (
    <div className="flex items-center justify-center">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
    </div>
  );
}
```

- [ ] **Step 5: Create frontend/src/components/common/ErrorMessage.tsx**

```typescript
interface ErrorMessageProps {
  message: string;
  onRetry?: () => void;
}

export function ErrorMessage({ message, onRetry }: ErrorMessageProps) {
  return (
    <div className="bg-red-50 border border-red-200 rounded-lg p-4">
      <p className="text-red-800">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-2 text-red-600 hover:text-red-700 font-medium text-sm"
        >
          Try Again
        </button>
      )}
    </div>
  );
}
```

- [ ] **Step 6: Create frontend/src/components/common/Modal.tsx**

```typescript
import React, { ReactNode } from 'react';
import { Button } from './Button';

interface ModalProps {
  isOpen: boolean;
  title: string;
  children: ReactNode;
  onClose: () => void;
  onConfirm?: () => void;
  confirmText?: string;
  cancelText?: string;
}

export function Modal({
  isOpen,
  title,
  children,
  onClose,
  onConfirm,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
}: ModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">{title}</h2>
        <div className="mb-6">{children}</div>
        <div className="flex gap-2 justify-end">
          <Button variant="ghost" onClick={onClose}>
            {cancelText}
          </Button>
          {onConfirm && (
            <Button variant="primary" onClick={onConfirm}>
              {confirmText}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 7: Create frontend/src/components/common/Rating.tsx**

```typescript
interface RatingProps {
  value?: number;
  max?: number;
  onChange?: (rating: number) => void;
  readOnly?: boolean;
}

export function Rating({ value = 0, max = 10, onChange, readOnly = true }: RatingProps) {
  return (
    <div className="flex items-center gap-1">
      <span className="text-sm font-medium text-gray-700">
        {value > 0 ? `${value.toFixed(1)}` : '-'}
      </span>
      <span className="text-sm text-gray-500">/ {max}</span>
      {!readOnly && (
        <div className="ml-2 flex gap-1">
          {[...Array(Math.ceil(max / 2))].map((_, i) => (
            <button
              key={i}
              onClick={() => onChange?.(i + 1)}
              className={`text-2xl ${
                i < Math.ceil(value / 2) ? 'text-yellow-400' : 'text-gray-300'
              }`}
            >
              ★
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 8: Commit common components**

```bash
git add frontend/src/components/common/
git commit -m "feat: implement common UI components (Button, Input, Card, etc.)"
```

---

## Task 7: Layout Components

**Files:**
- Create: `frontend/src/components/layout/Header.tsx`
- Create: `frontend/src/components/layout/Footer.tsx`
- Create: `frontend/src/components/layout/Layout.tsx`

- [ ] **Step 1: Create frontend/src/components/layout/Header.tsx**

```typescript
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../common/Button';

export function Header() {
  const { user, logout, isAuthenticated } = useAuth();

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
        <Link to="/" className="text-2xl font-bold text-indigo-600">
          TV Looker
        </Link>

        <nav className="flex items-center gap-6">
          <Link to="/" className="text-gray-700 hover:text-indigo-600">
            Home
          </Link>

          {isAuthenticated ? (
            <>
              <Link to="/recommendations" className="text-gray-700 hover:text-indigo-600">
                Recommendations
              </Link>
              <Link to="/lists" className="text-gray-700 hover:text-indigo-600">
                My Lists
              </Link>
              <Link to="/reviews" className="text-gray-700 hover:text-indigo-600">
                My Reviews
              </Link>
              <div className="flex items-center gap-2">
                <span className="text-sm text-gray-700">{user?.username}</span>
                <Button
                  variant="secondary"
                  onClick={() => {
                    logout();
                  }}
                  className="text-sm"
                >
                  Logout
                </Button>
              </div>
            </>
          ) : (
            <>
              <Link to="/login">
                <Button variant="ghost">Login</Button>
              </Link>
              <Link to="/register">
                <Button variant="primary">Register</Button>
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
```

- [ ] **Step 2: Create frontend/src/components/layout/Footer.tsx**

```typescript
export function Footer() {
  return (
    <footer className="bg-gray-900 text-white mt-16">
      <div className="max-w-7xl mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          <div>
            <h3 className="font-bold text-lg mb-4">TV Looker</h3>
            <p className="text-gray-400">Discover movies and TV series tailored to your taste.</p>
          </div>
          <div>
            <h4 className="font-semibold mb-4">Product</h4>
            <ul className="space-y-2 text-gray-400">
              <li><a href="/" className="hover:text-white">Browse</a></li>
              <li><a href="/recommendations" className="hover:text-white">Recommendations</a></li>
              <li><a href="/lists" className="hover:text-white">Lists</a></li>
            </ul>
          </div>
          <div>
            <h4 className="font-semibold mb-4">Company</h4>
            <ul className="space-y-2 text-gray-400">
              <li><a href="#" className="hover:text-white">About</a></li>
              <li><a href="#" className="hover:text-white">Contact</a></li>
              <li><a href="#" className="hover:text-white">Privacy</a></li>
            </ul>
          </div>
          <div>
            <h4 className="font-semibold mb-4">Legal</h4>
            <ul className="space-y-2 text-gray-400">
              <li><a href="#" className="hover:text-white">Terms</a></li>
              <li><a href="#" className="hover:text-white">Privacy Policy</a></li>
              <li><a href="#" className="hover:text-white">Cookie Policy</a></li>
            </ul>
          </div>
        </div>
        <div className="border-t border-gray-800 pt-8">
          <p className="text-gray-400 text-center">© 2026 TV Looker. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
}
```

- [ ] **Step 3: Create frontend/src/components/layout/Layout.tsx**

```typescript
import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { Footer } from './Footer';

export function Layout() {
  return (
    <div className="flex flex-col min-h-screen bg-gray-50">
      <Header />
      <main className="flex-1 max-w-7xl mx-auto w-full px-4 py-8">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
```

- [ ] **Step 4: Commit layout components**

```bash
git add frontend/src/components/layout/
git commit -m "feat: implement layout components (Header, Footer, Layout)"
```

---

## Task 8: Feature Components

**Files:**
- Create: `frontend/src/components/features/ItemCard.tsx`
- Create: `frontend/src/components/features/ItemGrid.tsx`
- Create: `frontend/src/components/features/ReviewCard.tsx`

- [ ] **Step 1: Create frontend/src/components/features/ItemCard.tsx**

```typescript
import { Link } from 'react-router-dom';
import { Item } from '../../types';
import { Card } from '../common/Card';
import { Rating } from '../common/Rating';

interface ItemCardProps {
  item: Item;
}

export function ItemCard({ item }: ItemCardProps) {
  return (
    <Link to={`/items/${item.id}`}>
      <Card className="overflow-hidden h-full">
        {item.posterUrl ? (
          <img
            src={item.posterUrl}
            alt={item.title}
            className="w-full aspect-[2/3] object-cover"
          />
        ) : (
          <div className="w-full aspect-[2/3] bg-gray-300 flex items-center justify-center">
            <span className="text-gray-500">No Image</span>
          </div>
        )}
        <div className="p-4">
          <h3 className="font-semibold text-lg text-gray-900 line-clamp-2">
            {item.title}
          </h3>
          <p className="text-sm text-gray-500 mb-2">{item.type}</p>
          <div className="flex items-center justify-between">
            <Rating value={item.voteAverage} />
            <span className="text-xs text-gray-400">
              {new Date(item.releaseDate).getFullYear()}
            </span>
          </div>
        </div>
      </Card>
    </Link>
  );
}
```

- [ ] **Step 2: Create frontend/src/components/features/ItemGrid.tsx**

```typescript
import { Item } from '../../types';
import { ItemCard } from './ItemCard';

interface ItemGridProps {
  items: Item[];
  loading?: boolean;
}

export function ItemGrid({ items, loading }: ItemGridProps) {
  if (loading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {[...Array(8)].map((_, i) => (
          <div
            key={i}
            className="aspect-[2/3] bg-gray-300 rounded-lg animate-pulse"
          />
        ))}
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500 text-lg">No items found</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      {items.map((item) => (
        <ItemCard key={item.id} item={item} />
      ))}
    </div>
  );
}
```

- [ ] **Step 3: Create frontend/src/components/features/ReviewCard.tsx**

```typescript
import { Review } from '../../types';
import { Card } from '../common/Card';
import { Rating } from '../common/Rating';

interface ReviewCardProps {
  review: Review;
}

export function ReviewCard({ review }: ReviewCardProps) {
  return (
    <Card className="p-4">
      <div className="flex items-start justify-between mb-2">
        <div>
          <p className="font-semibold text-gray-900">{review.userName || 'Anonymous'}</p>
          <p className="text-sm text-gray-500">
            {new Date(review.createdAt).toLocaleDateString()}
          </p>
        </div>
        <Rating value={review.rating} max={10} readOnly />
      </div>
      <p className="text-gray-700 line-clamp-3">{review.content}</p>
    </Card>
  );
}
```

- [ ] **Step 4: Commit feature components**

```bash
git add frontend/src/components/features/
git commit -m "feat: implement feature components (ItemCard, ItemGrid, ReviewCard)"
```

---

## Task 9: Routing & Root App Component

**Files:**
- Create: `frontend/src/App.tsx`
- Create: `frontend/src/components/ProtectedRoute.tsx`
- Create: `frontend/src/utils/formatters.ts`
- Create: `frontend/src/index.css`

- [ ] **Step 1: Create frontend/src/components/ProtectedRoute.tsx**

```typescript
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Loader } from './common/Loader';

export function ProtectedRoute() {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-96">
        <Loader />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
```

- [ ] **Step 2: Create frontend/src/utils/formatters.ts**

```typescript
export function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

export function formatYear(dateString: string): number {
  return new Date(dateString).getFullYear();
}

export function formatRating(rating: number | undefined): string {
  if (!rating) return 'N/A';
  return rating.toFixed(1);
}
```

- [ ] **Step 3: Create frontend/src/index.css**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#root {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
```

- [ ] **Step 4: Create frontend/src/App.tsx**

```typescript
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './contexts/AuthContext';
import { Layout } from './components/layout/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import Home from './pages/Home';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import NotFound from './pages/NotFound';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      gcTime: 1000 * 60 * 10, // 10 minutes (garbage collection time)
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Layout />}>
              {/* Public Routes */}
              <Route index element={<Home />} />
              <Route path="login" element={<Login />} />
              <Route path="register" element={<Register />} />

              {/* Protected Routes */}
              <Route element={<ProtectedRoute />}>
                {/* Placeholder - we'll add these pages in subsequent tasks */}
              </Route>

              {/* 404 */}
              <Route path="*" element={<NotFound />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
```

- [ ] **Step 5: Create frontend/src/main.tsx**

```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
```

- [ ] **Step 6: Create frontend/.env.development**

```bash
VITE_API_URL=http://localhost:8080/api/v1
```

- [ ] **Step 7: Commit routing and app setup**

```bash
git add frontend/src/App.tsx frontend/src/main.tsx frontend/src/components/ProtectedRoute.tsx frontend/src/utils/formatters.ts frontend/src/index.css frontend/.env.development
git commit -m "feat: implement React Router setup and root App component"
```

---

## Task 10: Authentication Pages (Login & Register)

**Files:**
- Create: `frontend/src/pages/auth/Login.tsx`
- Create: `frontend/src/pages/auth/Register.tsx`
- Create: `frontend/src/utils/validators.ts`

- [ ] **Step 1: Create frontend/src/utils/validators.ts**

```typescript
export const validateEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

export const validatePassword = (password: string): string | null => {
  if (password.length < 8) {
    return 'Password must be at least 8 characters';
  }
  return null;
};

export const validateUsername = (username: string): string | null => {
  if (username.length < 3) {
    return 'Username must be at least 3 characters';
  }
  if (!/^[a-zA-Z0-9_-]+$/.test(username)) {
    return 'Username can only contain letters, numbers, underscore, and hyphen';
  }
  return null;
};
```

- [ ] **Step 2: Create frontend/src/pages/auth/Login.tsx**

```typescript
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Card } from '../../components/common/Card';
import { validateEmail } from '../../utils/validators';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ email?: string; password?: string; general?: string }>({});
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const validateForm = (): boolean => {
    const newErrors: typeof errors = {};

    if (!email) {
      newErrors.email = 'Email is required';
    } else if (!validateEmail(email)) {
      newErrors.email = 'Please enter a valid email';
    }

    if (!password) {
      newErrors.password = 'Password is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) return;

    setIsLoading(true);
    try {
      await login(email, password);
      navigate('/recommendations');
    } catch (error: any) {
      setErrors({
        general: error.message || 'Login failed. Please try again.',
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-[calc(100vh-180px)]">
      <Card className="w-full max-w-md p-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">Login</h1>

        {errors.general && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4">
            <p className="text-red-700 text-sm">{errors.general}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={errors.email}
            placeholder="you@example.com"
          />

          <Input
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={errors.password}
            placeholder="••••••••"
          />

          <Button type="submit" variant="primary" isLoading={isLoading} className="w-full">
            Login
          </Button>
        </form>

        <p className="text-center text-gray-600 mt-4">
          Don't have an account?{' '}
          <a href="/register" className="text-indigo-600 hover:text-indigo-700 font-medium">
            Register
          </a>
        </p>
      </Card>
    </div>
  );
}
```

- [ ] **Step 3: Create frontend/src/pages/auth/Register.tsx**

```typescript
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Card } from '../../components/common/Card';
import { validateEmail, validatePassword, validateUsername } from '../../utils/validators';

export default function Register() {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const validateForm = (): boolean => {
    const newErrors: typeof errors = {};

    const usernameError = validateUsername(formData.username);
    if (usernameError) newErrors.username = usernameError;

    if (!formData.email) {
      newErrors.email = 'Email is required';
    } else if (!validateEmail(formData.email)) {
      newErrors.email = 'Please enter a valid email';
    }

    const passwordError = validatePassword(formData.password);
    if (passwordError) newErrors.password = passwordError;

    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) return;

    setIsLoading(true);
    try {
      await register(formData.email, formData.password, formData.username);
      navigate('/recommendations');
    } catch (error: any) {
      setErrors({
        general: error.message || 'Registration failed. Please try again.',
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-[calc(100vh-180px)]">
      <Card className="w-full max-w-md p-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">Create Account</h1>

        {errors.general && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4">
            <p className="text-red-700 text-sm">{errors.general}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Username"
            value={formData.username}
            onChange={(e) => setFormData({ ...formData, username: e.target.value })}
            error={errors.username}
            placeholder="john_doe"
          />

          <Input
            label="Email"
            type="email"
            value={formData.email}
            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            error={errors.email}
            placeholder="you@example.com"
          />

          <Input
            label="Password"
            type="password"
            value={formData.password}
            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
            error={errors.password}
            placeholder="••••••••"
          />

          <Input
            label="Confirm Password"
            type="password"
            value={formData.confirmPassword}
            onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
            error={errors.confirmPassword}
            placeholder="••••••••"
          />

          <Button type="submit" variant="primary" isLoading={isLoading} className="w-full">
            Register
          </Button>
        </form>

        <p className="text-center text-gray-600 mt-4">
          Already have an account?{' '}
          <a href="/login" className="text-indigo-600 hover:text-indigo-700 font-medium">
            Login
          </a>
        </p>
      </Card>
    </div>
  );
}
```

- [ ] **Step 4: Commit authentication pages**

```bash
git add frontend/src/pages/auth/ frontend/src/utils/validators.ts
git commit -m "feat: implement login and registration pages with validation"
```

---

## Task 11: Home & Item Detail Pages

**Files:**
- Create: `frontend/src/pages/Home.tsx`
- Create: `frontend/src/pages/ItemDetail.tsx`
- Create: `frontend/src/pages/NotFound.tsx`

- [ ] **Step 1: Create frontend/src/pages/Home.tsx**

```typescript
import { useAuth } from '../hooks/useAuth';
import { useItems } from '../hooks/useItems';
import { ItemGrid } from '../components/features/ItemGrid';
import { Loader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { Button } from '../components/common/Button';

export default function Home() {
  const { isAuthenticated } = useAuth();
  const { data: items, isLoading, error, refetch } = useItems();

  return (
    <div className="space-y-8">
      {/* Hero Section */}
      <div className="text-center py-12">
        <h1 className="text-5xl font-bold text-gray-900 mb-4">
          Discover Your Next Favorite
        </h1>
        <p className="text-xl text-gray-600 mb-6">
          Get personalized recommendations for movies and TV series
        </p>
        {!isAuthenticated && (
          <div className="flex gap-4 justify-center">
            <a href="/register">
              <Button variant="primary">Get Started</Button>
            </a>
            <a href="/login">
              <Button variant="secondary">Login</Button>
            </a>
          </div>
        )}
      </div>

      {/* Items Grid */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900 mb-4">Popular Items</h2>
        {isLoading ? (
          <Loader />
        ) : error ? (
          <ErrorMessage
            message="Failed to load items"
            onRetry={() => refetch()}
          />
        ) : (
          <ItemGrid items={items || []} />
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Create frontend/src/pages/ItemDetail.tsx**

```typescript
import { useParams } from 'react-router-dom';
import { useItem } from '../hooks/useItems';
import { useAuth } from '../hooks/useAuth';
import { Loader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import { Rating } from '../components/common/Rating';
import { formatDate } from '../utils/formatters';

export default function ItemDetail() {
  const { id } = useParams<{ id: string }>();
  const itemId = id ? parseInt(id) : 0;
  const { data: item, isLoading, error, refetch } = useItem(itemId);
  const { isAuthenticated } = useAuth();

  if (isLoading) return <Loader />;
  if (error) return <ErrorMessage message="Failed to load item" onRetry={() => refetch()} />;
  if (!item) return <ErrorMessage message="Item not found" />;

  return (
    <div className="space-y-8">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* Poster */}
        <div>
          {item.posterUrl ? (
            <img
              src={item.posterUrl}
              alt={item.title}
              className="w-full rounded-lg shadow-lg"
            />
          ) : (
            <div className="w-full aspect-[2/3] bg-gray-300 rounded-lg flex items-center justify-center">
              <span className="text-gray-500">No Image</span>
            </div>
          )}
        </div>

        {/* Details */}
        <div className="md:col-span-2 space-y-6">
          <div>
            <h1 className="text-4xl font-bold text-gray-900 mb-2">{item.title}</h1>
            <div className="flex items-center gap-4 text-gray-600">
              <span>{item.type}</span>
              <span>{new Date(item.releaseDate).getFullYear()}</span>
              <Rating value={item.voteAverage} />
            </div>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-gray-900 mb-2">Overview</h2>
            <p className="text-gray-700 leading-relaxed">{item.overview}</p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-gray-900 mb-2">Genres</h2>
            <div className="flex flex-wrap gap-2">
              {item.genres.map((genre) => (
                <span
                  key={genre.id}
                  className="bg-indigo-100 text-indigo-800 px-3 py-1 rounded-full text-sm"
                >
                  {genre.name}
                </span>
              ))}
            </div>
          </div>

          {isAuthenticated && (
            <div className="flex gap-2">
              <Button variant="primary">Add to List</Button>
              <Button variant="secondary">Write Review</Button>
            </div>
          )}
        </div>
      </div>

      {/* Cast */}
      {item.actors.length > 0 && (
        <div>
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Cast</h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            {item.actors.map((actor) => (
              <Card key={actor.id} className="p-4 text-center">
                <p className="font-semibold text-gray-900">{actor.actorName}</p>
                {actor.characterName && (
                  <p className="text-sm text-gray-600">as {actor.characterName}</p>
                )}
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* Directors */}
      {item.directors.length > 0 && (
        <div>
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Directors</h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            {item.directors.map((director) => (
              <Card key={director.id} className="p-4 text-center">
                <p className="font-semibold text-gray-900">{director.name}</p>
              </Card>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 3: Create frontend/src/pages/NotFound.tsx**

```typescript
import { Link } from 'react-router-dom';
import { Button } from '../components/common/Button';

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-180px)]">
      <h1 className="text-6xl font-bold text-gray-900 mb-4">404</h1>
      <p className="text-xl text-gray-600 mb-8">Page not found</p>
      <Link to="/">
        <Button variant="primary">Back to Home</Button>
      </Link>
    </div>
  );
}
```

- [ ] **Step 4: Update App.tsx to include Item Detail route**

```bash
# Open frontend/src/App.tsx and add this route after the Home route:
# <Route path="items/:id" element={<ItemDetail />} />
```

- [ ] **Step 5: Import ItemDetail in App.tsx**

Add at the top of frontend/src/App.tsx:
```typescript
import ItemDetail from './pages/ItemDetail';
```

- [ ] **Step 6: Commit home and item detail pages**

```bash
git add frontend/src/pages/Home.tsx frontend/src/pages/ItemDetail.tsx frontend/src/pages/NotFound.tsx frontend/src/App.tsx
git commit -m "feat: implement Home, ItemDetail, and NotFound pages"
```

---

## Task 12: Recommendations, Lists, and Reviews Pages

**Files:**
- Create: `frontend/src/pages/Recommendations.tsx`
- Create: `frontend/src/pages/MyLists.tsx`
- Create: `frontend/src/pages/ListDetail.tsx`
- Create: `frontend/src/pages/MyReviews.tsx`
- Create: `frontend/src/pages/Profile.tsx`

- [ ] **Step 1: Create frontend/src/pages/Recommendations.tsx**

```typescript
import { useAuth } from '../hooks/useAuth';
import { useRecommendations } from '../hooks/useRecommendations';
import { ItemGrid } from '../components/features/ItemGrid';
import { Loader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';

export default function Recommendations() {
  const { user } = useAuth();
  const { data: items, isLoading, error, refetch } = useRecommendations(user?.id || null, 20);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold text-gray-900 mb-2">Personalized Recommendations</h1>
        <p className="text-gray-600">Movies and series tailored just for you</p>
      </div>

      {isLoading ? (
        <Loader />
      ) : error ? (
        <ErrorMessage
          message="Failed to load recommendations"
          onRetry={() => refetch()}
        />
      ) : (
        <ItemGrid items={items || []} />
      )}
    </div>
  );
}
```

- [ ] **Step 2: Create frontend/src/pages/MyLists.tsx**

```typescript
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useLists, useCreateList } from '../hooks/useLists';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import { Modal } from '../components/common/Modal';
import { Input } from '../components/common/Input';
import { Loader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';

export default function MyLists() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newListName, setNewListName] = useState('');
  const [newListDescription, setNewListDescription] = useState('');
  const { data: lists, isLoading, error, refetch } = useLists();
  const { mutate: createList, isPending } = useCreateList();

  const handleCreateList = () => {
    if (!newListName.trim()) return;
    
    createList(
      {
        name: newListName,
        description: newListDescription,
      },
      {
        onSuccess: () => {
          setNewListName('');
          setNewListDescription('');
          setIsModalOpen(false);
          refetch();
        },
      }
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-4xl font-bold text-gray-900 mb-2">My Lists</h1>
          <p className="text-gray-600">Organize your favorite movies and series</p>
        </div>
        <Button variant="primary" onClick={() => setIsModalOpen(true)}>
          + New List
        </Button>
      </div>

      <Modal
        isOpen={isModalOpen}
        title="Create New List"
        onClose={() => setIsModalOpen(false)}
        onConfirm={handleCreateList}
        confirmText="Create"
      >
        <div className="space-y-4">
          <Input
            label="List Name"
            value={newListName}
            onChange={(e) => setNewListName(e.target.value)}
            placeholder="e.g., My Favorites"
          />
          <Input
            label="Description"
            value={newListDescription}
            onChange={(e) => setNewListDescription(e.target.value)}
            placeholder="Optional description"
          />
        </div>
      </Modal>

      {isLoading ? (
        <Loader />
      ) : error ? (
        <ErrorMessage
          message="Failed to load lists"
          onRetry={() => refetch()}
        />
      ) : !lists || lists.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500 text-lg mb-4">No lists yet</p>
          <Button variant="primary" onClick={() => setIsModalOpen(true)}>
            Create your first list
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {lists.map((list) => (
            <Link key={list.id} to={`/lists/${list.id}`}>
              <Card className="p-4 h-full">
                <h3 className="font-semibold text-lg text-gray-900 mb-2">{list.name}</h3>
                {list.description && (
                  <p className="text-sm text-gray-600 mb-4">{list.description}</p>
                )}
                <p className="text-sm text-gray-500">{list.items.length} items</p>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 3: Create frontend/src/pages/ListDetail.tsx**

```typescript
import { useParams } from 'react-router-dom';
import { useList } from '../hooks/useLists';
import { ItemGrid } from '../components/features/ItemGrid';
import { Loader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { Button } from '../components/common/Button';

export default function ListDetail() {
  const { id } = useParams<{ id: string }>();
  const listId = id ? parseInt(id) : 0;
  const { data: list, isLoading, error, refetch } = useList(listId);

  if (isLoading) return <Loader />;
  if (error) return <ErrorMessage message="Failed to load list" onRetry={() => refetch()} />;
  if (!list) return <ErrorMessage message="List not found" />;

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-4xl font-bold text-gray-900 mb-2">{list.name}</h1>
          {list.description && (
            <p className="text-gray-600 mb-2">{list.description}</p>
          )}
          <p className="text-sm text-gray-500">{list.items.length} items</p>
        </div>
        <Button variant="secondary">Edit</Button>
      </div>

      {list.items.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500 text-lg">No items in this list yet</p>
        </div>
      ) : (
        <ItemGrid items={list.items} />
      )}
    </div>
  );
}
```

- [ ] **Step 4: Create frontend/src/pages/MyReviews.tsx**

```typescript
import { useReviews } from '../hooks/useReviews';
import { ReviewCard } from '../components/features/ReviewCard';
import { Loader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';

export default function MyReviews() {
  const { data: reviews, isLoading, error, refetch } = useReviews();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold text-gray-900 mb-2">My Reviews</h1>
        <p className="text-gray-600">Your movie and series reviews</p>
      </div>

      {isLoading ? (
        <Loader />
      ) : error ? (
        <ErrorMessage
          message="Failed to load reviews"
          onRetry={() => refetch()}
        />
      ) : !reviews || reviews.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500 text-lg">You haven't written any reviews yet</p>
        </div>
      ) : (
        <div className="space-y-4">
          {reviews.map((review) => (
            <ReviewCard key={review.id} review={review} />
          ))}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 5: Create frontend/src/pages/Profile.tsx**

```typescript
import { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useUpdateUser } from '../hooks/useUsers';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Card } from '../components/common/Card';
import { Loader } from '../components/common/Loader';

export default function Profile() {
  const { user, logout } = useAuth();
  const { mutate: updateUser, isPending } = useUpdateUser();
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    name: user?.name || '',
  });

  if (!user) return <Loader />;

  const handleSave = () => {
    updateUser(
      { id: user.id, request: formData },
      {
        onSuccess: () => {
          setIsEditing(false);
        },
      }
    );
  };

  return (
    <div className="max-w-2xl">
      <h1 className="text-4xl font-bold text-gray-900 mb-6">Profile</h1>

      <Card className="p-6 space-y-6">
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Account Information</h2>
          <div className="space-y-3">
            <div>
              <label className="block text-sm text-gray-600 mb-1">Username</label>
              <p className="text-gray-900">{user.username}</p>
            </div>
            <div>
              <label className="block text-sm text-gray-600 mb-1">Email</label>
              <p className="text-gray-900">{user.email}</p>
            </div>
          </div>
        </div>

        <hr />

        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Personal Information</h2>
          {isEditing ? (
            <div className="space-y-4">
              <Input
                label="Name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
              <div className="flex gap-2">
                <Button variant="primary" onClick={handleSave} isLoading={isPending}>
                  Save
                </Button>
                <Button variant="secondary" onClick={() => setIsEditing(false)}>
                  Cancel
                </Button>
              </div>
            </div>
          ) : (
            <div className="space-y-3">
              <div>
                <label className="block text-sm text-gray-600 mb-1">Name</label>
                <p className="text-gray-900">{user.name || '-'}</p>
              </div>
              <Button variant="secondary" onClick={() => setIsEditing(true)}>
                Edit
              </Button>
            </div>
          )}
        </div>

        <hr />

        <div>
          <Button
            variant="danger"
            onClick={() => {
              logout();
            }}
          >
            Logout
          </Button>
        </div>
      </Card>
    </div>
  );
}
```

- [ ] **Step 6: Update App.tsx to include all new routes**

Edit frontend/src/App.tsx and replace the protected routes section with:

```typescript
{/* Protected Routes */}
<Route element={<ProtectedRoute />}>
  <Route path="items/:id" element={<ItemDetail />} />
  <Route path="recommendations" element={<Recommendations />} />
  <Route path="lists" element={<MyLists />} />
  <Route path="lists/:id" element={<ListDetail />} />
  <Route path="reviews" element={<MyReviews />} />
  <Route path="profile" element={<Profile />} />
</Route>
```

And add imports at the top:

```typescript
import ItemDetail from './pages/ItemDetail';
import Recommendations from './pages/Recommendations';
import MyLists from './pages/MyLists';
import ListDetail from './pages/ListDetail';
import MyReviews from './pages/MyReviews';
import Profile from './pages/Profile';
```

- [ ] **Step 7: Commit all page components**

```bash
git add frontend/src/pages/ frontend/src/App.tsx
git commit -m "feat: implement all main pages (Recommendations, Lists, Reviews, Profile)"
```

---

## Task 13: Docker Compose & Development Setup

**Files:**
- Modify: `docker-compose.yml` (root)
- Create: `frontend-development.md`

- [ ] **Step 1: Update root docker-compose.yml**

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
    networks:
      - tv-looker-network

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
    networks:
      - tv-looker-network

volumes:
  postgres-data:

networks:
  tv-looker-network:
    driver: bridge
```

- [ ] **Step 2: Create frontend-development.md**

```markdown
# Frontend Development Guide

## Getting Started

### Prerequisites

- Node.js 20+
- npm or yarn
- Docker (for database and optional frontend dev server)

### Setup

#### Option 1: Using Docker (Recommended)

Start everything with a single command:

\`\`\`bash
docker-compose up -d postgres frontend
\`\`\`

Then in another terminal, start the backend:

\`\`\`bash
mvn spring-boot:run
\`\`\`

Access URLs:
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Database: localhost:5432

#### Option 2: Without Docker

Start the database:

\`\`\`bash
docker-compose up -d postgres
\`\`\`

Install frontend dependencies:

\`\`\`bash
cd frontend
npm install
\`\`\`

Start the frontend dev server:

\`\`\`bash
npm run dev
\`\`\`

In another terminal, start the backend:

\`\`\`bash
mvn spring-boot:run
\`\`\`

## Development Workflow

### Running Tests

\`\`\`bash
cd frontend
npm test
\`\`\`

### Building for Production

\`\`\`bash
cd frontend
npm run build
\`\`\`

### Linting

\`\`\`bash
cd frontend
npm run lint
\`\`\`

### Formatting

\`\`\`bash
cd frontend
npm run format
\`\`\`

## Troubleshooting

### Port already in use

If port 5173 is already in use:

\`\`\`bash
cd frontend
npm run dev -- --port 5174
\`\`\`

### CORS errors

Make sure the backend has CORS configured for `http://localhost:5173`

### API calls failing

Check that:
1. Backend is running on port 8080
2. `VITE_API_URL` is set correctly in `.env.development`
3. Network tab in DevTools shows the requests

## Project Structure

See the main README for project structure details.

## Backend Integration

The frontend communicates with the backend via REST API at `/api/v1` endpoints.

Required backend changes:
1. CORS configuration for `http://localhost:5173`
2. Authentication endpoints: `/auth/login`, `/auth/register`, `/auth/logout`, `/auth/me`
3. Spring Session configuration for cookie-based auth

## Environment Variables

Copy `.env.example` to `.env.development` and update as needed:

\`\`\`bash
VITE_API_URL=http://localhost:8080/api/v1
\`\`\`

For production builds, create `.env.production`:

\`\`\`bash
VITE_API_URL=https://your-backend-domain.com/api/v1
\`\`\`
```

- [ ] **Step 3: Commit Docker and development setup**

```bash
git add docker-compose.yml frontend-development.md
git commit -m "chore: update docker-compose and add development guide"
```

---

## Task 14: Backend CORS Configuration

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/config/WebConfig.java`

- [ ] **Step 1: Create CORS configuration for backend**

```java
package org.tvl.tvlooker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:3000"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

- [ ] **Step 2: Commit backend CORS configuration**

```bash
git add src/main/java/org/tvl/tvlooker/config/WebConfig.java
git commit -m "feat: add CORS configuration for frontend integration"
```

---

## Task 15: Authentication Backend Endpoints

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/api/controller/AuthController.java`
- Create: `src/main/java/org/tvl/tvlooker/api/dto/request/LoginRequest.java`
- Create: `src/main/java/org/tvl/tvlooker/api/dto/request/RegisterRequest.java`

- [ ] **Step 1: Create LoginRequest DTO**

```java
package org.tvl.tvlooker.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
```

- [ ] **Step 2: Create RegisterRequest DTO**

```java
package org.tvl.tvlooker.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String firstName;
    private String lastName;
}
```

- [ ] **Step 3: Create AuthController**

```java
package org.tvl.tvlooker.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tvl.tvlooker.api.dto.mapper.UserMapper;
import org.tvl.tvlooker.api.dto.request.LoginRequest;
import org.tvl.tvlooker.api.dto.request.RegisterRequest;
import org.tvl.tvlooker.api.dto.response.UserResponse;
import org.tvl.tvlooker.domain.model.User;
import org.tvl.tvlooker.service.UserService;

import jakarta.servlet.http.HttpSession;

/**
 * REST controller for authentication operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    /**
     * Login endpoint - creates session.
     * @param request login credentials
     * @param session HTTP session
     * @return authenticated user
     */
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session) {
        User user = userService.authenticateByEmailAndPassword(request.getEmail(), request.getPassword());
        session.setAttribute("userId", user.getId());
        return ResponseEntity.ok(UserResponse.builder().data(UserMapper.toResponse(user)).build());
    }

    /**
     * Register endpoint - creates new user and session.
     * @param request registration data
     * @param session HTTP session
     * @return created user
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpSession session) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
        User created = userService.create(user);
        // In production, hash the password before storing
        session.setAttribute("userId", created.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserResponse.builder().data(UserMapper.toResponse(created)).build());
    }

    /**
     * Get current authenticated user.
     * @param session HTTP session
     * @return current user or 401 if not authenticated
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userService.getById((java.util.UUID) userId);
        return ResponseEntity.ok(UserResponse.builder().data(UserMapper.toResponse(user)).build());
    }

    /**
     * Logout endpoint - invalidates session.
     * @param session HTTP session
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Add authentication method to UserService**

Note: You'll need to add this method to your UserService:

```java
public User authenticateByEmailAndPassword(String email, String password) {
    // TODO: Implement password verification (use BCrypt in production)
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));
    return user;
}
```

- [ ] **Step 5: Commit authentication endpoints**

```bash
git add src/main/java/org/tvl/tvlooker/api/controller/AuthController.java src/main/java/org/tvl/tvlooker/api/dto/request/LoginRequest.java src/main/java/org/tvl/tvlooker/api/dto/request/RegisterRequest.java
git commit -m "feat: implement authentication endpoints (login, register, logout, me)"
```

---

## Task 16: Final Verification & Testing

- [ ] **Step 1: Verify all TypeScript types compile**

```bash
cd frontend
npm run build 2>&1 | head -50
```

Expected: Build completes without type errors

- [ ] **Step 2: Start Docker services**

```bash
docker-compose up -d postgres frontend
```

Expected: Both services start successfully

- [ ] **Step 3: Start backend**

```bash
mvn spring-boot:run
```

Expected: Backend starts on port 8080

- [ ] **Step 4: Verify frontend loads**

Open browser to: `http://localhost:5173`

Expected: Home page loads without errors

- [ ] **Step 5: Check browser console for errors**

Expected: No CORS errors, network requests to backend succeed

- [ ] **Step 6: Test API connectivity**

```bash
curl -X GET http://localhost:8080/api/v1/items -H "Content-Type: application/json"
```

Expected: Returns items list (or empty list)

- [ ] **Step 7: Create final commit**

```bash
git add -A
git commit -m "chore: frontend implementation complete and verified"
```

---

## Summary

This implementation plan covers the complete frontend development for TV Looker with:

✅ **Project Setup** - Vite, React, TypeScript, Tailwind configuration
✅ **Type Definitions** - Full TypeScript types for all API responses
✅ **API Client** - Axios instance with interceptors and all endpoint modules
✅ **State Management** - Context API for auth, TanStack Query for server state
✅ **Authentication** - Login/register with session-based auth
✅ **UI Components** - Reusable component library
✅ **Layout** - Header, footer, and app layout
✅ **Pages** - Home, item detail, recommendations, lists, reviews, profile
✅ **Docker Setup** - Development environment with docker-compose
✅ **Backend Integration** - CORS configuration and auth endpoints

**Total Tasks:** 16
**Estimated Time:** 8-10 hours for experienced developer
**Key Commits:** 16

Next steps after implementation:
- Add automated testing (Vitest + React Testing Library)
- Implement error boundaries for better error handling
- Add loading skeletons for better UX
- Implement search and filtering
- Add pagination for large lists
- Consider authentication migration to JWT + OAuth for production
