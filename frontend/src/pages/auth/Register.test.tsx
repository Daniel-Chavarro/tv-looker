import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { Register } from './Register';
import { renderWithProviders } from '../../test/render';

const authMocks = vi.hoisted(() => ({
  register: vi.fn(),
  isAuthenticated: false,
}));

vi.mock('../../hooks/useAuth', () => ({
  useAuth: () => authMocks,
}));

describe('Register page', () => {
  it('renders the real register form and router link to login', () => {
    renderWithProviders(<Register />);

    expect(screen.getByRole('heading', { name: 'Create Account' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Sign in' })).toHaveAttribute('href', '/login');
    expect(screen.queryByText('Register Page')).not.toBeInTheDocument();
  });
});
