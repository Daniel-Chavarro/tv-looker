import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MyLists } from './MyLists';
import { renderWithProviders } from '../test/render';

const listsState = vi.hoisted(() => ({
  user: { id: '123e4567-e89b-12d3-a456-426614174000' },
  data: null as { content: unknown[] } | null,
  isLoading: false,
  error: null as Error | null,
  refetch: vi.fn(),
  createList: { mutateAsync: vi.fn(), isPending: false },
  deleteList: { mutateAsync: vi.fn(), isPending: false },
}));

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({ user: listsState.user }),
}));

vi.mock('../hooks/useLists', () => ({
  useLists: () => ({
    data: listsState.data,
    isLoading: listsState.isLoading,
    error: listsState.error,
    refetch: listsState.refetch,
  }),
  useCreateList: () => listsState.createList,
  useDeleteList: () => listsState.deleteList,
}));

describe('MyLists', () => {
  beforeEach(() => {
    listsState.data = { content: [] };
    listsState.isLoading = false;
    listsState.error = null;
    listsState.refetch.mockReset();
    listsState.createList.mutateAsync.mockReset();
    listsState.deleteList.mutateAsync.mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('shows an accessible alert when creating a list fails', async () => {
    const user = userEvent.setup();
    listsState.createList.mutateAsync.mockRejectedValueOnce(new Error('create failed'));
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    renderWithProviders(<MyLists />);

    await user.click(screen.getByRole('button', { name: 'Create Your First List' }));
    await user.type(screen.getByLabelText('List Name'), 'Weekend watches');
    await user.click(screen.getByRole('button', { name: 'Create' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Failed to create list. Please try again.');
    expect(screen.getByText('Create New List')).toBeInTheDocument();
  });

  it('shows an accessible alert when deleting a list fails', async () => {
    const user = userEvent.setup();
    listsState.data = {
      content: [
        {
          id: 1,
          userId: '123e4567-e89b-12d3-a456-426614174000',
          name: 'Watchlist',
          description: '',
          items: [],
        },
      ],
    };
    listsState.deleteList.mutateAsync.mockRejectedValueOnce(new Error('delete failed'));
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    renderWithProviders(<MyLists />);

    await user.click(screen.getByRole('button', { name: 'Delete' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Failed to delete list. Please try again.');
    expect(screen.getByText('Watchlist')).toBeInTheDocument();
  });
});
