import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { renderWithProviders } from '../test/render';
import { Login } from '../pages/auth/Login';

const authMocks = vi.hoisted(() => ({
  isAuthenticated: false,
  isLoading: false,
}));

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => authMocks,
}));

describe('ProtectedRoute', () => {
  beforeEach(() => {
    authMocks.isAuthenticated = false;
    authMocks.isLoading = false;
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
});
