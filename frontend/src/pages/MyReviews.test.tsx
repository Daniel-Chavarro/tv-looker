import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MyReviews } from './MyReviews';
import { renderWithProviders } from '../test/render';

const reviewsState = vi.hoisted(() => ({
  user: { id: 'user-1' },
  data: null as { content: unknown[] } | null,
  isLoading: false,
  error: null as Error | null,
  refetch: vi.fn(),
  updateReview: { mutateAsync: vi.fn(), isPending: false },
  deleteReview: { mutateAsync: vi.fn(), isPending: false },
}));

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({ user: reviewsState.user }),
}));

vi.mock('../hooks/useReviews', () => ({
  useUserReviews: () => ({
    data: reviewsState.data,
    isLoading: reviewsState.isLoading,
    error: reviewsState.error,
    refetch: reviewsState.refetch,
  }),
  useUpdateReview: () => reviewsState.updateReview,
  useDeleteReview: () => reviewsState.deleteReview,
}));

describe('MyReviews', () => {
  beforeEach(() => {
    reviewsState.data = null;
    reviewsState.isLoading = false;
    reviewsState.error = null;
    reviewsState.refetch.mockReset();
    reviewsState.updateReview.mutateAsync.mockReset();
    reviewsState.deleteReview.mutateAsync.mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('routes the empty-state browse CTA to the home page', () => {
    reviewsState.data = { content: [] };

    renderWithProviders(<MyReviews />);

    expect(screen.getByRole('link', { name: 'Browse Items' })).toHaveAttribute('href', '/');
    expect(screen.queryByRole('link', { name: 'Browse Items' })).not.toHaveAttribute('href', '/items');
  });

  it('shows an accessible alert when updating a review fails', async () => {
    const user = userEvent.setup();
    reviewsState.data = {
      content: [
        {
          id: 7,
          userId: '123e4567-e89b-12d3-a456-426614174000',
          itemId: 42,
          rating: 4,
          comment: 'Great pacing.',
          createdAt: '2024-01-01T00:00:00Z',
        },
      ],
    };
    reviewsState.updateReview.mutateAsync.mockRejectedValueOnce(new Error('update failed'));
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    renderWithProviders(<MyReviews />);

    await user.click(screen.getByRole('button', { name: 'Edit' }));
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Failed to update review. Please try again.');
    expect(screen.getByText('Edit Review')).toBeInTheDocument();
  });

  it('shows an accessible alert when deleting a review fails', async () => {
    const user = userEvent.setup();
    reviewsState.data = {
      content: [
        {
          id: 7,
          userId: '123e4567-e89b-12d3-a456-426614174000',
          itemId: 42,
          rating: 4,
          comment: 'Great pacing.',
          createdAt: '2024-01-01T00:00:00Z',
        },
      ],
    };
    reviewsState.deleteReview.mutateAsync.mockRejectedValueOnce(new Error('delete failed'));
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    renderWithProviders(<MyReviews />);

    await user.click(screen.getByRole('button', { name: 'Delete' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Failed to delete review. Please try again.');
    expect(screen.getByText('Great pacing.')).toBeInTheDocument();
  });
});
