import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { Header } from './Header';
import { renderWithProviders } from '../../test/render';

const authState = vi.hoisted(() => ({
  isAuthenticated: false,
  user: null as { username: string } | null,
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
    expect(screen.queryByRole('link', { name: 'My Lists' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'My Reviews' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Search' })).not.toBeInTheDocument();
  });

  it('links only to live routes for authenticated users', () => {
    authState.isAuthenticated = true;
    authState.user = { username: 'alex' };

    renderWithProviders(<Header />);

    expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Recommendations' })).toHaveAttribute('href', '/recommendations');
    expect(screen.getByRole('link', { name: 'My Lists' })).toHaveAttribute('href', '/lists');
    expect(screen.getByRole('link', { name: 'My Reviews' })).toHaveAttribute('href', '/reviews');
    expect(screen.queryByRole('link', { name: 'Search' })).not.toBeInTheDocument();
  });
});
