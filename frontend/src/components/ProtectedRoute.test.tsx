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
        </Routes>
      </MemoryRouter>,
      { withRouter: false }
    );

    expect(screen.getByText('Protected content')).toBeInTheDocument();
  });

  it('shows forbidden state for missing authority', () => {
    authMocks.isAuthenticated = true;
    authMocks.user = { authority: 'USER' };

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route
            path="/admin"
            element={
              <ProtectedRoute requiredAuthority="ADMIN">
                <div>Admin content</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>,
      { withRouter: false }
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Forbidden');
    expect(screen.queryByText('Admin content')).not.toBeInTheDocument();
  });
});
