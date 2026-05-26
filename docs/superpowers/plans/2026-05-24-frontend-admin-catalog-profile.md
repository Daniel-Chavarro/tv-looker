# Frontend Admin, Catalog, And Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add admin TMDB operations, catalog search/pagination, profile navigation, and regular-user self-profile access.

**Architecture:** Keep frontend work in the existing Vite/React structure: API wrappers in `frontend/src/api`, React Query hooks in `frontend/src/hooks`, route pages in `frontend/src/pages`, and route guards in `frontend/src/components`. Add one backend self-profile controller at `/api/v1/me`; leave `/api/v1/users/**` admin-only and leave item filtering Specifications to a separate backend PR.

**Tech Stack:** Java 21, Spring Boot 4, Spring Security JWT resource server, Maven, React 19, Vite, React Router, TanStack Query, Axios, Vitest, Testing Library, Tailwind v4.

**React best-practice constraints:** Use React Query for request deduplication and cache invalidation. Keep search input local and submit explicit query state to avoid request waterfalls. Use `startTransition` for non-urgent catalog query/page state updates. Do not add `useMemo` or `useCallback` for simple derived values. Do not define components inside components. Import direct modules rather than creating new barrel exports unless the repo already needs them.

---

## File Structure

- Create: `src/main/java/org/tvl/tvlooker/api/controller/SelfProfileController.java` for authenticated `/api/v1/me` GET/PATCH.
- Modify: `src/test/java/org/tvl/tvlooker/api/controller/UserControllerTest.java` to add self-profile controller unit tests.
- Modify: `src/test/java/org/tvl/tvlooker/api/controller/SecurityIntegrationTest.java` to verify `/api/v1/me` security and existing admin restrictions.
- Modify: `frontend/src/api/users.ts` to add self-profile calls without removing admin user API wrappers.
- Modify: `frontend/src/hooks/useUsers.ts` to add `useCurrentUser` and `useUpdateCurrentUser`.
- Modify: `frontend/src/pages/Profile.tsx` to use self-profile hooks and remove admin delete dependency from the profile UI.
- Modify: `frontend/src/pages/Profile.test.tsx` to assert self-profile hook usage and update errors.
- Create: `frontend/src/api/tmdbAdmin.ts` for TMDB admin status/collect/sync calls.
- Create: `frontend/src/hooks/useTmdbAdmin.ts` for React Query status and mutations.
- Create: `frontend/src/pages/AdminTmdb.tsx` for admin operations UI.
- Create: `frontend/src/pages/AdminTmdb.test.tsx` for status, success, and error behavior.
- Modify: `frontend/src/components/ProtectedRoute.tsx` to support admin-only routes.
- Modify: `frontend/src/components/ProtectedRoute.test.tsx` to cover loading, authenticated, unauthenticated, and forbidden states.
- Modify: `frontend/src/components/layout/Header.tsx` to add Profile and Admin links.
- Modify: `frontend/src/components/layout/Header.test.tsx` to cover role-gated navigation.
- Modify: `frontend/src/App.tsx` to add `/admin/tmdb` route.
- Create: `frontend/src/components/features/PaginationControls.tsx` for reusable catalog pagination.
- Create: `frontend/src/components/features/PaginationControls.test.tsx` for page controls.
- Modify: `frontend/src/api/items.ts` to replace `pageSize` with `size` and add placeholder query params.
- Modify: `frontend/src/hooks/useItems.ts` to use the same item query params.
- Modify: `frontend/src/pages/Home.tsx` to add catalog search, filters, and pagination.
- Create: `frontend/src/pages/Home.test.tsx` for query params and pagination reset behavior.

## Task 1: Backend Self-Profile API

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/api/controller/SelfProfileController.java`
- Modify: `src/test/java/org/tvl/tvlooker/api/controller/UserControllerTest.java`
- Modify: `src/test/java/org/tvl/tvlooker/api/controller/SecurityIntegrationTest.java`

- [ ] **Step 1: Add failing unit tests for `/api/v1/me` controller behavior**

Append this nested test class to `src/test/java/org/tvl/tvlooker/api/controller/UserControllerTest.java`. Add the required import `org.tvl.tvlooker.api.controller.SelfProfileController;` only if the compiler does not resolve the class in the same package automatically.

```java
@Nested
class SelfProfile {

    @Test
    void givenAuthenticatedPrincipal_whenGetCurrentUser_thenReturnsCurrentUser() throws Exception {
        SelfProfileController controller = new SelfProfileController(userService);
        MockMvc selfProfileMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(userService.getById(testUserId)).thenReturn(testUser);

        selfProfileMvc.perform(get("/api/v1/me")
                        .principal(() -> testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testUserId.toString())))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.name", is("Test User")))
                .andExpect(jsonPath("$.authority", is("USER")))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(userService, times(1)).getById(testUserId);
    }

    @Test
    void givenAuthenticatedPrincipal_whenPatchCurrentUser_thenUpdatesCurrentUserOnly() throws Exception {
        SelfProfileController controller = new SelfProfileController(userService);
        MockMvc selfProfileMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        String requestBody = """
                {
                    "email": "updated@example.com",
                    "name": "Updated User"
                }
                """;
        User updatedUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("updated@example.com")
                .name("Updated User")
                .authority(UserAuthority.USER)
                .createdAt(testUser.getCreatedAt())
                .build();

        when(userService.update(eq(testUserId), any(User.class))).thenReturn(updatedUser);

        selfProfileMvc.perform(patch("/api/v1/me")
                        .principal(() -> testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testUserId.toString())))
                .andExpect(jsonPath("$.email", is("updated@example.com")))
                .andExpect(jsonPath("$.name", is("Updated User")));

        verify(userService, times(1)).update(eq(testUserId), any(User.class));
    }
}
```

- [ ] **Step 2: Run controller tests and verify they fail because the controller does not exist**

Run: `mvn test -Dtest=UserControllerTest`

Expected: FAIL with a compilation error referencing `SelfProfileController`.

- [ ] **Step 3: Implement the self-profile controller**

Create `src/main/java/org/tvl/tvlooker/api/controller/SelfProfileController.java`:

```java
package org.tvl.tvlooker.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.mapper.UserMapper;
import org.tvl.tvlooker.api.dto.request.UpdateUserRequest;
import org.tvl.tvlooker.api.dto.response.UserResponse;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.service.UserService;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class SelfProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        User user = userService.getById(currentUserId(principal));
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    @PatchMapping
    public ResponseEntity<UserResponse> updateCurrentUser(
            Principal principal,
            @Valid @RequestBody UpdateUserRequest request) {
        User user = UserMapper.fromUpdateRequest(request);
        User updated = userService.update(currentUserId(principal), user);
        return ResponseEntity.ok(UserMapper.toResponse(updated));
    }

    private UUID currentUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
```

- [ ] **Step 4: Run controller tests and verify they pass**

Run: `mvn test -Dtest=UserControllerTest`

Expected: PASS.

- [ ] **Step 5: Add security integration tests for self-profile access**

Modify `src/test/java/org/tvl/tvlooker/api/controller/SecurityIntegrationTest.java` imports to include `patch`:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
```

Add these tests before `adminToken()`:

```java
@Test
void givenNoToken_whenGetCurrentUserProfile_thenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized());
}

@Test
void givenUserJwt_whenGetCurrentUserProfile_thenPassesSecurityAuthorization() throws Exception {
    int status = mockMvc.perform(get("/api/v1/me")
                    .with(jwt()
                            .jwt(token -> token.subject(UUID.randomUUID().toString()))
                            .authorities(new SimpleGrantedAuthority("USER"))))
            .andReturn()
            .getResponse()
            .getStatus();

    assertThat(status).isNotIn(401, 403);
}

@Test
void givenUserJwt_whenPatchCurrentUserProfile_thenPassesSecurityAuthorization() throws Exception {
    int status = mockMvc.perform(patch("/api/v1/me")
                    .with(jwt()
                            .jwt(token -> token.subject(UUID.randomUUID().toString()))
                            .authorities(new SimpleGrantedAuthority("USER")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andReturn()
            .getResponse()
            .getStatus();

    assertThat(status).isNotIn(401, 403);
}
```

- [ ] **Step 6: Run security integration tests**

Run: `mvn test -Dtest=SecurityIntegrationTest`

Expected: PASS. The new authenticated `/api/v1/me` tests may return 404 because the random test user does not exist, but they must not return 401 or 403.

- [ ] **Step 7: Commit backend self-profile API**

Run:

```bash
git add src/main/java/org/tvl/tvlooker/api/controller/SelfProfileController.java src/test/java/org/tvl/tvlooker/api/controller/UserControllerTest.java src/test/java/org/tvl/tvlooker/api/controller/SecurityIntegrationTest.java
git commit -m "feat: add self profile endpoint"
```

Expected: one commit with only self-profile backend files. Do not stage unrelated changes already present in `src/main/java/org/tvl/tvlooker/api/controller/ItemController.java` unless they are intentionally part of the worker's implementation.

## Task 2: Frontend Self-Profile API And Page

**Files:**
- Modify: `frontend/src/api/users.ts`
- Modify: `frontend/src/hooks/useUsers.ts`
- Modify: `frontend/src/pages/Profile.tsx`
- Modify: `frontend/src/pages/Profile.test.tsx`

- [ ] **Step 1: Update profile tests to expect self-profile hooks**

Replace `frontend/src/pages/Profile.test.tsx` with:

```tsx
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Profile } from './Profile';
import { renderWithProviders } from '../test/render';

const profileState = vi.hoisted(() => ({
  authUser: {
    id: '123e4567-e89b-12d3-a456-426614174000',
    username: 'mira',
    email: 'mira@example.com',
    authority: 'USER' as const,
  },
  userData: {
    id: '123e4567-e89b-12d3-a456-426614174000',
    username: 'mira',
    email: 'mira@example.com',
    authority: 'USER' as const,
    name: 'Mira',
    createdAt: '2024-01-01T00:00:00Z',
  },
  isLoading: false,
  error: null as Error | null,
  refetch: vi.fn(),
  logout: vi.fn(),
  updateCurrentUser: { mutateAsync: vi.fn(), isPending: false },
}));

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({ user: profileState.authUser, logout: profileState.logout }),
}));

vi.mock('../hooks/useUsers', () => ({
  useCurrentUser: () => ({
    data: profileState.userData,
    isLoading: profileState.isLoading,
    error: profileState.error,
    refetch: profileState.refetch,
  }),
  useUpdateCurrentUser: () => profileState.updateCurrentUser,
}));

describe('Profile', () => {
  beforeEach(() => {
    profileState.isLoading = false;
    profileState.error = null;
    profileState.refetch.mockReset();
    profileState.logout.mockReset();
    profileState.updateCurrentUser.mutateAsync.mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('shows an accessible alert when updating the profile fails', async () => {
    const user = userEvent.setup();
    profileState.updateCurrentUser.mutateAsync.mockRejectedValueOnce(new Error('update failed'));
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    renderWithProviders(<Profile />);

    await user.click(screen.getByRole('button', { name: 'Edit Profile' }));
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Failed to update profile. Please try again.');
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
  });

  it('does not render admin-only account deletion controls', () => {
    renderWithProviders(<Profile />);

    expect(screen.queryByRole('button', { name: 'Delete Account' })).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run the profile test and verify it fails**

Run from `frontend/`: `npm run test:run -- src/pages/Profile.test.tsx`

Expected: FAIL because `useCurrentUser` and `useUpdateCurrentUser` do not exist and the page still renders delete controls.

- [ ] **Step 3: Add self-profile API wrappers**

Update `frontend/src/api/users.ts` to:

```ts
import { apiClient } from './client';
import type { User, UpdateUserRequest } from '../types';

export const usersApi = {
  getUser: async (id: string): Promise<User> => {
    const response = await apiClient.get<User>(`/users/${id}`);
    return response.data;
  },

  updateUser: async (id: string, data: UpdateUserRequest): Promise<User> => {
    const response = await apiClient.patch<User>(`/users/${id}`, data);
    return response.data;
  },

  deleteUser: async (id: string): Promise<void> => {
    await apiClient.delete(`/users/${id}`);
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await apiClient.get<User>('/me');
    return response.data;
  },

  updateCurrentUser: async (data: UpdateUserRequest): Promise<User> => {
    const response = await apiClient.patch<User>('/me', data);
    return response.data;
  },
};
```

- [ ] **Step 4: Add self-profile hooks**

Update `frontend/src/hooks/useUsers.ts` to:

```ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../api/users';
import type { UpdateUserRequest } from '../types';

export const useUser = (id: string) => {
  return useQuery({
    queryKey: ['user', id],
    queryFn: () => usersApi.getUser(id),
    staleTime: 5 * 60 * 1000,
    enabled: !!id,
  });
};

export const useUpdateUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) =>
      usersApi.updateUser(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['user', variables.id] });
    },
  });
};

export const useDeleteUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => usersApi.deleteUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user'] });
    },
  });
};

export const useCurrentUser = () => {
  return useQuery({
    queryKey: ['me'],
    queryFn: usersApi.getCurrentUser,
    staleTime: 5 * 60 * 1000,
  });
};

export const useUpdateCurrentUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: usersApi.updateCurrentUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['me'] });
    },
  });
};
```

- [ ] **Step 5: Update Profile to use `/me` hooks and remove delete UI**

Replace `frontend/src/pages/Profile.tsx` with:

```tsx
import { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useCurrentUser, useUpdateCurrentUser } from '../hooks/useUsers';
import { PageLoader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Modal } from '../components/common/Modal';

export function Profile() {
  const { logout } = useAuth();
  const { data, isLoading, error, refetch } = useCurrentUser();
  const updateCurrentUser = useUpdateCurrentUser();

  const [showEditModal, setShowEditModal] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [updateError, setUpdateError] = useState<string | null>(null);

  const handleEdit = () => {
    if (!data) return;
    setUpdateError(null);
    setName(data.name || '');
    setEmail(data.email);
    setShowEditModal(true);
  };

  const closeEditModal = () => {
    setUpdateError(null);
    setShowEditModal(false);
  };

  const handleUpdateProfile = async () => {
    setUpdateError(null);
    try {
      await updateCurrentUser.mutateAsync({
        name: name.trim() || undefined,
        email: email.trim() || undefined,
      });
      closeEditModal();
    } catch (err) {
      setUpdateError('Failed to update profile. Please try again.');
      console.error('Failed to update profile:', err);
    }
  };

  if (isLoading) {
    return <PageLoader message="Loading profile..." />;
  }

  if (error || !data) {
    return (
      <ErrorMessage
        message="Failed to load profile. Please try again."
        retry={() => refetch()}
      />
    );
  }

  const user = data;
  const joinDate = new Date(user.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-bold text-neutral-100 tracking-tight">
            Profile
          </h1>
          <Button variant="secondary" onClick={handleEdit}>
            Edit Profile
          </Button>
        </div>

        <div className="bg-neutral-900/40 border border-neutral-800 rounded-sm p-6">
          <div className="flex items-center gap-6 mb-8">
            <div className="w-20 h-20 rounded-full bg-neutral-800 flex items-center justify-center">
              <span className="text-amber-500 text-3xl font-bold">
                {user.username[0].toUpperCase()}
              </span>
            </div>
            <div>
              <h2 className="text-2xl font-semibold text-neutral-100">
                {user.name || user.username}
              </h2>
              <p className="text-neutral-500">@{user.username}</p>
            </div>
          </div>

          <div className="space-y-6">
            <div>
              <label className="block text-sm text-neutral-500 tracking-wider uppercase mb-1">
                Username
              </label>
              <p className="text-neutral-200">{user.username}</p>
            </div>

            <div>
              <label className="block text-sm text-neutral-500 tracking-wider uppercase mb-1">
                Email
              </label>
              <p className="text-neutral-200">{user.email}</p>
            </div>

            {user.name && (
              <div>
                <label className="block text-sm text-neutral-500 tracking-wider uppercase mb-1">
                  Name
                </label>
                <p className="text-neutral-200">{user.name}</p>
              </div>
            )}

            <div>
              <label className="block text-sm text-neutral-500 tracking-wider uppercase mb-1">
                Member Since
              </label>
              <p className="text-neutral-200">{joinDate}</p>
            </div>
          </div>
        </div>

        <div className="mt-8">
          <Button
            variant="ghost"
            onClick={() => logout()}
            className="text-neutral-400"
          >
            Sign Out
          </Button>
        </div>
      </div>

      <Modal isOpen={showEditModal} onClose={closeEditModal} title="Edit Profile">
        <div className="space-y-4">
          <Input
            label="Name (optional)"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Enter your name"
          />
          <Input
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Enter your email"
          />
          {updateError && (
            <div
              role="alert"
              className="rounded-sm border border-red-900/30 bg-red-950/20 px-4 py-3 text-sm text-red-300"
            >
              {updateError}
            </div>
          )}
          <div className="flex gap-3 pt-4">
            <Button
              variant="secondary"
              onClick={closeEditModal}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              onClick={handleUpdateProfile}
              disabled={!email.trim() || updateCurrentUser.isPending}
              isLoading={updateCurrentUser.isPending}
              className="flex-1"
            >
              Save
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 6: Run profile tests**

Run from `frontend/`: `npm run test:run -- src/pages/Profile.test.tsx`

Expected: PASS.

- [ ] **Step 7: Commit frontend self-profile work**

Run:

```bash
git add frontend/src/api/users.ts frontend/src/hooks/useUsers.ts frontend/src/pages/Profile.tsx frontend/src/pages/Profile.test.tsx
git commit -m "feat(frontend): use self profile API"
```

Expected: one frontend profile commit.

## Task 3: Admin Route Guard And Header Navigation

**Files:**
- Modify: `frontend/src/components/ProtectedRoute.tsx`
- Modify: `frontend/src/components/ProtectedRoute.test.tsx`
- Modify: `frontend/src/components/layout/Header.tsx`
- Modify: `frontend/src/components/layout/Header.test.tsx`

- [ ] **Step 1: Replace route guard tests**

Replace `frontend/src/components/ProtectedRoute.test.tsx` with:

```tsx
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { renderWithProviders } from '../test/render';
import { Login } from '../pages/auth/Login';

const authMocks = vi.hoisted(() => ({
  isAuthenticated: false,
  isLoading: false,
  user: null as { authority: 'USER' | 'ADMIN' } | null,
}));

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => authMocks,
}));

describe('ProtectedRoute', () => {
  beforeEach(() => {
    authMocks.isAuthenticated = false;
    authMocks.isLoading = false;
    authMocks.user = null;
  });

  it('redirects unauthenticated users to the login route', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/lists']}>
        <Routes>
          <Route
            path="/lists"
            element={
              <ProtectedRoute>
                <div>Protected content</div>
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<Login />} />
        </Routes>
      </MemoryRouter>,
      { withRouter: false }
    );

    expect(await screen.findByRole('heading', { name: 'Sign In' })).toBeInTheDocument();
    expect(screen.queryByText('Protected content')).not.toBeInTheDocument();
  });

  it('renders protected content for authenticated users', () => {
    authMocks.isAuthenticated = true;
    authMocks.user = { authority: 'USER' };

    renderWithProviders(
      <ProtectedRoute>
        <div>Protected content</div>
      </ProtectedRoute>
    );

    expect(screen.getByText('Protected content')).toBeInTheDocument();
  });

  it('shows a forbidden state when authority is missing', () => {
    authMocks.isAuthenticated = true;
    authMocks.user = { authority: 'USER' };

    renderWithProviders(
      <ProtectedRoute requiredAuthority="ADMIN">
        <div>Admin content</div>
      </ProtectedRoute>
    );

    expect(screen.getByRole('heading', { name: 'Forbidden' })).toBeInTheDocument();
    expect(screen.queryByText('Admin content')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run route guard tests and verify they fail**

Run from `frontend/`: `npm run test:run -- src/components/ProtectedRoute.test.tsx`

Expected: FAIL because `requiredAuthority` is not implemented.

- [ ] **Step 3: Implement authority support in ProtectedRoute**

Replace `frontend/src/components/ProtectedRoute.tsx` with:

```tsx
import { Navigate, useLocation } from 'react-router-dom';
import { type ReactNode } from 'react';
import { useAuth } from '../hooks/useAuth';
import type { UserAuthority } from '../types';

interface ProtectedRouteProps {
  children: ReactNode;
  requiredAuthority?: UserAuthority;
}

export function ProtectedRoute({ children, requiredAuthority }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requiredAuthority && user?.authority !== requiredAuthority) {
    return (
      <div className="container mx-auto px-4 py-16">
        <div className="mx-auto max-w-xl rounded-sm border border-neutral-800 bg-neutral-900/50 p-8 text-center">
          <h1 className="text-2xl font-bold text-neutral-100">Forbidden</h1>
          <p className="mt-3 text-neutral-400">
            You do not have permission to view this page.
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
```

- [ ] **Step 4: Replace header tests**

Replace `frontend/src/components/layout/Header.test.tsx` with:

```tsx
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { Header } from './Header';
import { renderWithProviders } from '../../test/render';

const authState = vi.hoisted(() => ({
  isAuthenticated: false,
  user: null as { username: string; authority: 'USER' | 'ADMIN' } | null,
  logout: vi.fn(),
}));

vi.mock('../../contexts/AuthContext', () => ({
  useAuth: () => authState,
}));

describe('Header', () => {
  beforeEach(() => {
    authState.isAuthenticated = false;
    authState.user = null;
    authState.logout.mockReset();
  });

  it('links only to live routes for anonymous users', () => {
    renderWithProviders(<Header />);

    expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('button', { name: 'Login' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Register' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Profile' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Admin' })).not.toBeInTheDocument();
  });

  it('shows profile but not admin navigation for regular users', () => {
    authState.isAuthenticated = true;
    authState.user = { username: 'alex', authority: 'USER' };

    renderWithProviders(<Header />);

    expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Recommendations' })).toHaveAttribute('href', '/recommendations');
    expect(screen.getByRole('link', { name: 'My Lists' })).toHaveAttribute('href', '/lists');
    expect(screen.getByRole('link', { name: 'My Reviews' })).toHaveAttribute('href', '/reviews');
    expect(screen.getByRole('link', { name: 'Profile' })).toHaveAttribute('href', '/profile');
    expect(screen.queryByRole('link', { name: 'Admin' })).not.toBeInTheDocument();
  });

  it('shows admin navigation for admin users', () => {
    authState.isAuthenticated = true;
    authState.user = { username: 'admin', authority: 'ADMIN' };

    renderWithProviders(<Header />);

    expect(screen.getByRole('link', { name: 'Admin' })).toHaveAttribute('href', '/admin/tmdb');
  });
});
```

- [ ] **Step 5: Update Header navigation**

In `frontend/src/components/layout/Header.tsx`, add these links inside the authenticated nav fragment after `My Reviews`:

```tsx
<Link
  to="/profile"
  className="px-4 py-2 text-neutral-400 hover:text-amber-400 hover:bg-neutral-800/50 rounded-sm transition-colors duration-200 text-sm font-medium"
>
  Profile
</Link>
{user?.authority === 'ADMIN' && (
  <Link
    to="/admin/tmdb"
    className="px-4 py-2 text-neutral-400 hover:text-amber-400 hover:bg-neutral-800/50 rounded-sm transition-colors duration-200 text-sm font-medium"
  >
    Admin
  </Link>
)}
```

- [ ] **Step 6: Run route/header tests**

Run from `frontend/`: `npm run test:run -- src/components/ProtectedRoute.test.tsx src/components/layout/Header.test.tsx`

Expected: PASS.

- [ ] **Step 7: Commit guard and header work**

Run:

```bash
git add frontend/src/components/ProtectedRoute.tsx frontend/src/components/ProtectedRoute.test.tsx frontend/src/components/layout/Header.tsx frontend/src/components/layout/Header.test.tsx
git commit -m "feat(frontend): add role gated navigation"
```

Expected: one frontend access-control commit.

## Task 4: TMDB Admin API, Hooks, Page, And Route

**Files:**
- Create: `frontend/src/api/tmdbAdmin.ts`
- Create: `frontend/src/hooks/useTmdbAdmin.ts`
- Create: `frontend/src/pages/AdminTmdb.tsx`
- Create: `frontend/src/pages/AdminTmdb.test.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Write admin page tests**

Create `frontend/src/pages/AdminTmdb.test.tsx`:

```tsx
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdminTmdb } from './AdminTmdb';
import { renderWithProviders } from '../test/render';

const tmdbState = vi.hoisted(() => ({
  status: {
    collectorRunning: false,
    lastSyncDate: '2026-05-01',
    syncEnabled: true,
    timestamp: '2026-05-24T10:00:00Z',
  },
  isLoading: false,
  error: null as Error | null,
  refetch: vi.fn(),
  collect: { mutateAsync: vi.fn(), isPending: false },
  sync: { mutateAsync: vi.fn(), isPending: false },
}));

vi.mock('../hooks/useTmdbAdmin', () => ({
  useTmdbStatus: () => ({
    data: tmdbState.status,
    isLoading: tmdbState.isLoading,
    error: tmdbState.error,
    refetch: tmdbState.refetch,
  }),
  useCollectTmdb: () => tmdbState.collect,
  useSyncTmdb: () => tmdbState.sync,
}));

describe('AdminTmdb', () => {
  beforeEach(() => {
    tmdbState.isLoading = false;
    tmdbState.error = null;
    tmdbState.refetch.mockReset();
    tmdbState.collect.mutateAsync.mockReset();
    tmdbState.sync.mutateAsync.mockReset();
    tmdbState.collect.isPending = false;
    tmdbState.sync.isPending = false;
  });

  it('renders TMDB status fields', () => {
    renderWithProviders(<AdminTmdb />);

    expect(screen.getByRole('heading', { name: 'TMDB Admin' })).toBeInTheDocument();
    expect(screen.getByText('Collector idle')).toBeInTheDocument();
    expect(screen.getByText('Sync enabled')).toBeInTheDocument();
    expect(screen.getByText('2026-05-01')).toBeInTheDocument();
  });

  it('runs collection and refreshes status', async () => {
    const user = userEvent.setup();
    tmdbState.collect.mutateAsync.mockResolvedValueOnce({
      status: 'started',
      message: 'TMDB data collection initiated',
      timestamp: '2026-05-24T10:01:00Z',
    });

    renderWithProviders(<AdminTmdb />);

    await user.click(screen.getByRole('button', { name: 'Collect TMDB Data' }));

    expect(tmdbState.collect.mutateAsync).toHaveBeenCalledTimes(1);
    expect(tmdbState.refetch).toHaveBeenCalledTimes(1);
    expect(await screen.findByRole('status')).toHaveTextContent('TMDB data collection initiated');
  });

  it('shows operation errors inline', async () => {
    const user = userEvent.setup();
    tmdbState.sync.mutateAsync.mockRejectedValueOnce(new Error('sync failed'));

    renderWithProviders(<AdminTmdb />);

    await user.click(screen.getByRole('button', { name: 'Sync TMDB Data' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Failed to sync TMDB data. Please try again.');
  });
});
```

- [ ] **Step 2: Run admin page tests and verify they fail**

Run from `frontend/`: `npm run test:run -- src/pages/AdminTmdb.test.tsx`

Expected: FAIL because the page and hooks do not exist.

- [ ] **Step 3: Add TMDB admin API module**

Create `frontend/src/api/tmdbAdmin.ts`:

```ts
import { apiClient } from './client';
import type { TmdbOperationResult, TmdbStatusResponse } from '../types';

export const tmdbAdminApi = {
  getStatus: async (): Promise<TmdbStatusResponse> => {
    const response = await apiClient.get<TmdbStatusResponse>('/admin/tmdb/status');
    return response.data;
  },

  collect: async (): Promise<TmdbOperationResult> => {
    const response = await apiClient.post<TmdbOperationResult>('/admin/tmdb/collect');
    return response.data;
  },

  sync: async (): Promise<TmdbOperationResult> => {
    const response = await apiClient.post<TmdbOperationResult>('/admin/tmdb/sync');
    return response.data;
  },
};
```

- [ ] **Step 4: Add TMDB admin hooks**

Create `frontend/src/hooks/useTmdbAdmin.ts`:

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { tmdbAdminApi } from '../api/tmdbAdmin';

export const useTmdbStatus = () => {
  return useQuery({
    queryKey: ['tmdb-admin', 'status'],
    queryFn: tmdbAdminApi.getStatus,
    staleTime: 30 * 1000,
  });
};

export const useCollectTmdb = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: tmdbAdminApi.collect,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tmdb-admin', 'status'] });
    },
  });
};

export const useSyncTmdb = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: tmdbAdminApi.sync,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tmdb-admin', 'status'] });
    },
  });
};
```

- [ ] **Step 5: Add TMDB admin page**

Create `frontend/src/pages/AdminTmdb.tsx`:

```tsx
import { useState } from 'react';
import { Button } from '../components/common/Button';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { PageLoader } from '../components/common/Loader';
import { useCollectTmdb, useSyncTmdb, useTmdbStatus } from '../hooks/useTmdbAdmin';

export function AdminTmdb() {
  const { data: status, isLoading, error, refetch } = useTmdbStatus();
  const collectTmdb = useCollectTmdb();
  const syncTmdb = useSyncTmdb();
  const [operationMessage, setOperationMessage] = useState<string | null>(null);
  const [operationError, setOperationError] = useState<string | null>(null);

  const handleCollect = async () => {
    setOperationError(null);
    setOperationMessage(null);
    try {
      const result = await collectTmdb.mutateAsync();
      setOperationMessage(result.message || 'TMDB collection started.');
      await refetch();
    } catch (err) {
      setOperationError('Failed to collect TMDB data. Please try again.');
      console.error('Failed to collect TMDB data:', err);
    }
  };

  const handleSync = async () => {
    setOperationError(null);
    setOperationMessage(null);
    try {
      const result = await syncTmdb.mutateAsync();
      setOperationMessage(result.message || 'TMDB synchronization completed.');
      await refetch();
    } catch (err) {
      setOperationError('Failed to sync TMDB data. Please try again.');
      console.error('Failed to sync TMDB data:', err);
    }
  };

  if (isLoading) {
    return <PageLoader message="Loading TMDB status..." />;
  }

  if (error || !status) {
    return (
      <ErrorMessage
        message="Failed to load TMDB status. Please try again."
        retry={() => refetch()}
      />
    );
  }

  const isBusy = collectTmdb.isPending || syncTmdb.isPending;

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mx-auto max-w-4xl space-y-8">
        <div>
          <p className="text-sm uppercase tracking-[0.3em] text-amber-500">Admin</p>
          <h1 className="mt-2 text-3xl font-bold text-neutral-100">TMDB Admin</h1>
          <p className="mt-3 text-neutral-400">
            Trigger collection and synchronization jobs for TMDB-backed catalog data.
          </p>
        </div>

        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-sm border border-neutral-800 bg-neutral-900/50 p-4">
            <p className="text-xs uppercase tracking-widest text-neutral-500">Collector</p>
            <p className="mt-2 text-lg font-semibold text-neutral-100">
              {status.collectorRunning ? 'Collector running' : 'Collector idle'}
            </p>
          </div>
          <div className="rounded-sm border border-neutral-800 bg-neutral-900/50 p-4">
            <p className="text-xs uppercase tracking-widest text-neutral-500">Sync</p>
            <p className="mt-2 text-lg font-semibold text-neutral-100">
              {status.syncEnabled ? 'Sync enabled' : 'Sync disabled'}
            </p>
          </div>
          <div className="rounded-sm border border-neutral-800 bg-neutral-900/50 p-4">
            <p className="text-xs uppercase tracking-widest text-neutral-500">Last Sync</p>
            <p className="mt-2 text-lg font-semibold text-neutral-100">
              {status.lastSyncDate || 'Never'}
            </p>
          </div>
          <div className="rounded-sm border border-neutral-800 bg-neutral-900/50 p-4">
            <p className="text-xs uppercase tracking-widest text-neutral-500">Checked</p>
            <p className="mt-2 text-sm font-semibold text-neutral-100">
              {new Date(status.timestamp).toLocaleString()}
            </p>
          </div>
        </section>

        <section className="rounded-sm border border-neutral-800 bg-neutral-900/40 p-6">
          <h2 className="text-xl font-semibold text-neutral-100">Operations</h2>
          <div className="mt-5 flex flex-col gap-3 sm:flex-row">
            <Button onClick={handleCollect} isLoading={collectTmdb.isPending} disabled={isBusy}>
              Collect TMDB Data
            </Button>
            <Button variant="secondary" onClick={handleSync} isLoading={syncTmdb.isPending} disabled={isBusy}>
              Sync TMDB Data
            </Button>
          </div>

          {operationMessage && (
            <div role="status" className="mt-5 rounded-sm border border-emerald-900/40 bg-emerald-950/20 px-4 py-3 text-sm text-emerald-300">
              {operationMessage}
            </div>
          )}

          {operationError && (
            <div role="alert" className="mt-5 rounded-sm border border-red-900/30 bg-red-950/20 px-4 py-3 text-sm text-red-300">
              {operationError}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
```

- [ ] **Step 6: Add admin route**

Modify `frontend/src/App.tsx` imports:

```tsx
import { AdminTmdb } from './pages/AdminTmdb';
```

Add this route after the `/profile` route:

```tsx
<Route
  path="/admin/tmdb"
  element={
    <ProtectedRoute requiredAuthority="ADMIN">
      <AdminTmdb />
    </ProtectedRoute>
  }
/>
```

- [ ] **Step 7: Run admin page tests**

Run from `frontend/`: `npm run test:run -- src/pages/AdminTmdb.test.tsx`

Expected: PASS.

- [ ] **Step 8: Commit TMDB admin frontend work**

Run:

```bash
git add frontend/src/api/tmdbAdmin.ts frontend/src/hooks/useTmdbAdmin.ts frontend/src/pages/AdminTmdb.tsx frontend/src/pages/AdminTmdb.test.tsx frontend/src/App.tsx
git commit -m "feat(frontend): add tmdb admin panel"
```

Expected: one frontend admin commit.

## Task 5: Catalog Pagination Component And Item Query Params

**Files:**
- Create: `frontend/src/components/features/PaginationControls.tsx`
- Create: `frontend/src/components/features/PaginationControls.test.tsx`
- Modify: `frontend/src/api/items.ts`
- Modify: `frontend/src/hooks/useItems.ts`

- [ ] **Step 1: Write pagination component tests**

Create `frontend/src/components/features/PaginationControls.test.tsx`:

```tsx
import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PaginationControls } from './PaginationControls';
import { renderWithProviders } from '../../test/render';

describe('PaginationControls', () => {
  it('renders current page and total item count', () => {
    renderWithProviders(
      <PaginationControls
        currentPage={2}
        totalPages={5}
        totalItems={87}
        isLast={false}
        onPageChange={vi.fn()}
      />
    );

    expect(screen.getByText('Page 2 of 5')).toBeInTheDocument();
    expect(screen.getByText('87 total items')).toBeInTheDocument();
  });

  it('moves to previous and next pages', async () => {
    const user = userEvent.setup();
    const onPageChange = vi.fn();

    renderWithProviders(
      <PaginationControls
        currentPage={2}
        totalPages={5}
        totalItems={87}
        isLast={false}
        onPageChange={onPageChange}
      />
    );

    await user.click(screen.getByRole('button', { name: 'Previous page' }));
    await user.click(screen.getByRole('button', { name: 'Next page' }));

    expect(onPageChange).toHaveBeenNthCalledWith(1, 1);
    expect(onPageChange).toHaveBeenNthCalledWith(2, 3);
  });

  it('disables previous on first page and next on last page', () => {
    renderWithProviders(
      <PaginationControls
        currentPage={1}
        totalPages={1}
        totalItems={3}
        isLast
        onPageChange={vi.fn()}
      />
    );

    expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
  });
});
```

- [ ] **Step 2: Run pagination test and verify it fails**

Run from `frontend/`: `npm run test:run -- src/components/features/PaginationControls.test.tsx`

Expected: FAIL because `PaginationControls` does not exist.

- [ ] **Step 3: Implement PaginationControls**

Create `frontend/src/components/features/PaginationControls.tsx`:

```tsx
import { Button } from '../common/Button';

interface PaginationControlsProps {
  currentPage: number;
  totalPages: number;
  totalItems: number;
  isLast: boolean;
  onPageChange: (page: number) => void;
}

function pageWindow(currentPage: number, totalPages: number) {
  const start = Math.max(1, currentPage - 2);
  const end = Math.min(totalPages, currentPage + 2);
  const pages: number[] = [];

  for (let page = start; page <= end; page += 1) {
    pages.push(page);
  }

  return pages;
}

export function PaginationControls({
  currentPage,
  totalPages,
  totalItems,
  isLast,
  onPageChange,
}: PaginationControlsProps) {
  if (totalPages <= 0) {
    return null;
  }

  const pages = pageWindow(currentPage, totalPages);
  const isFirst = currentPage <= 1;

  return (
    <nav className="mt-8 flex flex-col items-center gap-4" aria-label="Pagination">
      <div className="text-center text-sm text-neutral-400">
        <p>Page {currentPage} of {totalPages}</p>
        <p>{totalItems} total items</p>
      </div>

      <div className="flex flex-wrap items-center justify-center gap-2">
        <Button
          type="button"
          variant="secondary"
          className="px-4 py-2 text-xs"
          disabled={isFirst}
          aria-label="Previous page"
          onClick={() => onPageChange(currentPage - 1)}
        >
          Previous
        </Button>

        {pages.map((page) => (
          <Button
            key={page}
            type="button"
            variant={page === currentPage ? 'primary' : 'ghost'}
            className="px-4 py-2 text-xs"
            aria-label={`Page ${page}`}
            aria-current={page === currentPage ? 'page' : undefined}
            onClick={() => onPageChange(page)}
          >
            {page}
          </Button>
        ))}

        <Button
          type="button"
          variant="secondary"
          className="px-4 py-2 text-xs"
          disabled={isLast || currentPage >= totalPages}
          aria-label="Next page"
          onClick={() => onPageChange(currentPage + 1)}
        >
          Next
        </Button>
      </div>
    </nav>
  );
}
```

- [ ] **Step 4: Update item API params**

Replace `frontend/src/api/items.ts` with:

```ts
import { apiClient } from './client';
import type { Item } from '../types';
import type { PaginatedResponse } from '../types';

export type ItemQueryParams = {
  type?: 'MOVIE' | 'TV';
  genreId?: number;
  search?: string;
  year?: number;
  rating?: number;
  page?: number;
  size?: number;
};

export const itemsApi = {
  getItem: async (id: number): Promise<Item> => {
    const response = await apiClient.get<Item>(`/items/${id}`);
    return response.data;
  },

  getItems: async (params?: ItemQueryParams): Promise<PaginatedResponse<Item>> => {
    const response = await apiClient.get<PaginatedResponse<Item>>('/items', { params });
    return response.data;
  },
};
```

- [ ] **Step 5: Update item hook params**

Replace `frontend/src/hooks/useItems.ts` with:

```ts
import { useQuery } from '@tanstack/react-query';
import { itemsApi, type ItemQueryParams } from '../api/items';

export const useItems = (params?: ItemQueryParams) => {
  return useQuery({
    queryKey: ['items', params],
    queryFn: () => itemsApi.getItems(params),
    staleTime: 5 * 60 * 1000,
  });
};

export const useItem = (id: number) => {
  return useQuery({
    queryKey: ['item', id],
    queryFn: () => itemsApi.getItem(id),
    staleTime: 5 * 60 * 1000,
    enabled: !!id,
  });
};
```

- [ ] **Step 6: Run pagination tests**

Run from `frontend/`: `npm run test:run -- src/components/features/PaginationControls.test.tsx`

Expected: PASS.

- [ ] **Step 7: Commit catalog API and pagination component**

Run:

```bash
git add frontend/src/components/features/PaginationControls.tsx frontend/src/components/features/PaginationControls.test.tsx frontend/src/api/items.ts frontend/src/hooks/useItems.ts
git commit -m "feat(frontend): add catalog pagination controls"
```

Expected: one frontend catalog support commit.

## Task 6: Home Catalog Search, Filters, And Pagination

**Files:**
- Modify: `frontend/src/pages/Home.tsx`
- Create: `frontend/src/pages/Home.test.tsx`

- [ ] **Step 1: Write Home tests for query params and page reset**

Create `frontend/src/pages/Home.test.tsx`:

```tsx
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Home } from './Home';
import { renderWithProviders } from '../test/render';

const itemQueryState = vi.hoisted(() => ({
  params: [] as unknown[],
}));

vi.mock('../hooks/useItems', () => ({
  useItems: (params: unknown) => {
    itemQueryState.params.push(params);
    return {
      data: {
        content: [],
        actualPage: 1,
        totalPages: 3,
        totalItems: 41,
        isLast: false,
      },
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    };
  },
}));

describe('Home', () => {
  beforeEach(() => {
    itemQueryState.params = [];
  });

  it('requests the first page with fixed size by default', () => {
    renderWithProviders(<Home />);

    expect(itemQueryState.params.at(-1)).toEqual({ page: 1, size: 20 });
    expect(screen.getByText('41 total items')).toBeInTheDocument();
  });

  it('submits search and filter params', async () => {
    const user = userEvent.setup();

    renderWithProviders(<Home />);

    await user.type(screen.getByLabelText('Search catalog'), 'matrix');
    await user.selectOptions(screen.getByLabelText('Type'), 'MOVIE');
    await user.type(screen.getByLabelText('Year'), '1999');
    await user.type(screen.getByLabelText('Minimum rating'), '8');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    expect(itemQueryState.params.at(-1)).toEqual({
      page: 1,
      size: 20,
      search: 'matrix',
      type: 'MOVIE',
      year: 1999,
      rating: 8,
    });
  });

  it('resets to page one when submitting a new search', async () => {
    const user = userEvent.setup();

    renderWithProviders(<Home />);

    await user.click(screen.getByRole('button', { name: 'Next page' }));
    await user.type(screen.getByLabelText('Search catalog'), 'arrival');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    expect(itemQueryState.params.at(-1)).toMatchObject({ page: 1, search: 'arrival' });
  });
});
```

- [ ] **Step 2: Run Home test and verify it fails**

Run from `frontend/`: `npm run test:run -- src/pages/Home.test.tsx`

Expected: FAIL because Home still uses `pageSize` and has no controls.

- [ ] **Step 3: Implement Home catalog controls**

Replace `frontend/src/pages/Home.tsx` with:

```tsx
import { FormEvent, startTransition, useState } from 'react';
import { useItems } from '../hooks/useItems';
import { ItemGrid } from '../components/features/ItemGrid';
import { PageLoader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { PaginationControls } from '../components/features/PaginationControls';

const PAGE_SIZE = 20;

export function Home() {
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [type, setType] = useState<'MOVIE' | 'TV' | ''>('');
  const [genreIdInput, setGenreIdInput] = useState('');
  const [yearInput, setYearInput] = useState('');
  const [ratingInput, setRatingInput] = useState('');
  const [page, setPage] = useState(1);

  const genreId = genreIdInput.trim() ? Number(genreIdInput) : undefined;
  const year = yearInput.trim() ? Number(yearInput) : undefined;
  const rating = ratingInput.trim() ? Number(ratingInput) : undefined;

  const { data, isLoading, error, refetch } = useItems({
    page,
    size: PAGE_SIZE,
    ...(search ? { search } : {}),
    ...(type ? { type } : {}),
    ...(genreId ? { genreId } : {}),
    ...(year ? { year } : {}),
    ...(rating ? { rating } : {}),
  });

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    startTransition(() => {
      setSearch(searchInput.trim());
      setPage(1);
    });
  };

  const handleFilterChange = (nextType: 'MOVIE' | 'TV' | '') => {
    startTransition(() => {
      setType(nextType);
      setPage(1);
    });
  };

  const handlePageChange = (nextPage: number) => {
    startTransition(() => {
      setPage(nextPage);
    });
  };

  if (isLoading) {
    return <PageLoader message="Loading items..." />;
  }

  if (error) {
    return (
      <ErrorMessage
        message="Failed to load items. Please try again."
        retry={() => refetch()}
      />
    );
  }

  const items = data?.content ?? [];

  return (
    <div className="container mx-auto px-4 py-8">
      <section className="mb-8 rounded-sm border border-neutral-800 bg-neutral-900/40 p-5">
        <div className="mb-5">
          <p className="text-sm uppercase tracking-[0.3em] text-amber-500">Catalog</p>
          <h1 className="mt-2 text-3xl font-bold text-neutral-100">Find your next watch</h1>
        </div>

        <form onSubmit={handleSubmit} className="grid gap-4 lg:grid-cols-[1fr_auto]">
          <Input
            id="catalog-search"
            label="Search catalog"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search by title"
          />
          <Button type="submit" className="self-end">
            Search
          </Button>
        </form>

        <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <label className="block">
            <span className="block mb-2 text-xs tracking-widest uppercase text-neutral-400">Type</span>
            <select
              aria-label="Type"
              value={type}
              onChange={(event) => handleFilterChange(event.target.value as 'MOVIE' | 'TV' | '')}
              className="w-full rounded-sm border border-neutral-800 bg-neutral-900/80 px-4 py-3 text-sm text-neutral-100 focus:outline-none focus:ring-2 focus:ring-amber-500/30"
            >
              <option value="">Any type</option>
              <option value="MOVIE">Movies</option>
              <option value="TV">TV shows</option>
            </select>
          </label>
          <Input
            id="catalog-genre"
            label="Genre ID"
            type="number"
            min="1"
            value={genreIdInput}
            onChange={(event) => {
              setGenreIdInput(event.target.value);
              startTransition(() => setPage(1));
            }}
          />
          <Input
            id="catalog-year"
            label="Year"
            type="number"
            min="1900"
            value={yearInput}
            onChange={(event) => {
              setYearInput(event.target.value);
              startTransition(() => setPage(1));
            }}
          />
          <Input
            id="catalog-rating"
            label="Minimum rating"
            type="number"
            min="0"
            max="10"
            step="0.1"
            value={ratingInput}
            onChange={(event) => {
              setRatingInput(event.target.value);
              startTransition(() => setPage(1));
            }}
          />
        </div>
      </section>

      <section>
        <h2 className="text-2xl font-semibold text-neutral-200 tracking-wide mb-6">
          Results
        </h2>
        <ItemGrid items={items} emptyMessage="No items found" />
        {data && (
          <PaginationControls
            currentPage={data.actualPage}
            totalPages={data.totalPages}
            totalItems={data.totalItems}
            isLast={data.isLast}
            onPageChange={handlePageChange}
          />
        )}
      </section>
    </div>
  );
}
```

- [ ] **Step 4: Run Home tests**

Run from `frontend/`: `npm run test:run -- src/pages/Home.test.tsx`

Expected: PASS.

- [ ] **Step 5: Run related frontend tests**

Run from `frontend/`: `npm run test:run -- src/pages/Home.test.tsx src/components/features/PaginationControls.test.tsx`

Expected: PASS.

- [ ] **Step 6: Commit Home catalog UI**

Run:

```bash
git add frontend/src/pages/Home.tsx frontend/src/pages/Home.test.tsx
git commit -m "feat(frontend): add catalog search and pagination"
```

Expected: one Home page commit.

## Task 7: Final Verification And Cleanup

**Files:**
- Inspect: all files touched in prior tasks.

- [ ] **Step 1: Run backend tests**

Run: `mvn test`

Expected: PASS.

- [ ] **Step 2: Run frontend tests**

Run from `frontend/`: `npm run test:run`

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run from `frontend/`: `npm run build`

Expected: PASS.

- [ ] **Step 4: Run frontend lint**

Run from `frontend/`: `npm run lint`

Expected: PASS.

- [ ] **Step 5: Inspect git status**

Run: `git status --short`

Expected: only intentional files are modified. If `src/main/java/org/tvl/tvlooker/api/controller/ItemController.java` still appears as modified from work that predates this plan, do not revert or stage it unless the current worker intentionally owns that change.

- [ ] **Step 6: Inspect final diff**

Run: `git diff --stat`

Expected: changes are limited to self-profile API, frontend admin/catalog/profile files, tests, and plan/spec documents already approved.

- [ ] **Step 7: Confirm no verification fixes remain uncommitted**

Run: `git status --short`

Expected: no uncommitted files from this plan remain. If verification exposed additional implementation issues, stop and create a focused follow-up task instead of batching unknown fixes into this plan.

## Self-Review

- Spec coverage: Task 1 covers `/api/v1/me`; Task 2 moves Profile to self-profile APIs; Task 3 covers profile/admin navigation and admin route guard; Task 4 covers TMDB collect/sync/status; Tasks 5 and 6 cover fixed `size=20`, one-indexed `page`, query params, and reset behavior; Task 7 covers verification.
- Placeholder scan: The plan contains no placeholder sections or unresolved implementation instructions.
- Type consistency: Frontend item params use `size`, not `pageSize`; profile hooks use `useCurrentUser` and `useUpdateCurrentUser`; admin route uses `requiredAuthority="ADMIN"`; TMDB operation/status types match `frontend/src/types/tmdb.ts`.
- React best-practice coverage: React Query owns network state; Home uses explicit submitted search state; `startTransition` is limited to non-urgent catalog query/page updates; no unnecessary memo hooks or inline component definitions are introduced.
