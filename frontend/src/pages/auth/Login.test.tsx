import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Login } from './Login';
import { renderWithProviders } from '../../test/render';

const authMocks = vi.hoisted(() => ({
  login: vi.fn(),
  isAuthenticated: false,
}));

vi.mock('../../hooks/useAuth', () => ({
  useAuth: () => authMocks,
}));

describe('Login page', () => {
  it('renders the real login form and router link to register', () => {
    renderWithProviders(<Login />);

    expect(screen.getByRole('heading', { name: 'Sign In' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Create one' })).toHaveAttribute('href', '/register');
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
  });

  it('submits the identifier field as usernameOrEmail', async () => {
    const user = userEvent.setup();

    renderWithProviders(<Login />);

    await user.type(screen.getByLabelText('Email'), 'demo@example.com');
    await user.type(screen.getByLabelText('Password'), 'secret123');
    await user.click(screen.getByRole('button', { name: 'Sign In' }));

    expect(authMocks.login).toHaveBeenCalledWith('demo@example.com', 'secret123');
  });
});
