import { beforeEach, describe, expect, it } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { App } from './App';

describe('App auth routes', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('renders the real login page at /login', async () => {
    window.history.pushState({}, '', '/login');

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Sign In' })).toBeInTheDocument();
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
  });

  it('renders the real register page at /register', async () => {
    window.history.pushState({}, '', '/register');

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Create Account' })).toBeInTheDocument();
    expect(screen.queryByText('Register Page')).not.toBeInTheDocument();
  });

  it('redirects unauthenticated protected routes to the login page', async () => {
    window.history.pushState({}, '', '/lists');

    render(<App />);

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Sign In' })).toBeInTheDocument());
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
  });
});
