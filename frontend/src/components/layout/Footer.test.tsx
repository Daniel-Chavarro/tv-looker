import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { Footer } from './Footer';
import { renderWithProviders } from '../../test/render';

const authState = vi.hoisted(() => ({
  isAuthenticated: false,
  logout: vi.fn(),
}));

vi.mock('../../contexts/AuthContext', () => ({
  useAuth: () => authState,
}));

describe('Footer', () => {
  beforeEach(() => {
    authState.isAuthenticated = false;
    authState.logout.mockReset();
  });

  it('uses live routes for anonymous users and omits dead links', () => {
    renderWithProviders(<Footer />);

    expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Login' })).toHaveAttribute('href', '/login');
    expect(screen.getByRole('link', { name: 'Register' })).toHaveAttribute('href', '/register');
    expect(screen.queryByRole('link', { name: 'Search' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'My Lists' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'My Reviews' })).not.toBeInTheDocument();
  });

  it('uses live routes for authenticated users and omits dead links', () => {
    authState.isAuthenticated = true;

    renderWithProviders(<Footer />);

    expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Recommendations' })).toHaveAttribute('href', '/recommendations');
    expect(screen.getByRole('link', { name: 'My Lists' })).toHaveAttribute('href', '/lists');
    expect(screen.getByRole('link', { name: 'My Reviews' })).toHaveAttribute('href', '/reviews');
    expect(screen.queryByRole('link', { name: 'Search' })).not.toBeInTheDocument();
  });
});
