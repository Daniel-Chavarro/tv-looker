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

  it('does not show delete account controls', () => {
    renderWithProviders(<Profile />);

    expect(screen.queryByRole('button', { name: 'Delete Account' })).not.toBeInTheDocument();
    expect(screen.queryByText(/This action cannot be undone/i)).not.toBeInTheDocument();
  });
});
