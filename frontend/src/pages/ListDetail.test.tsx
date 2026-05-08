import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ListDetail } from './ListDetail';
import { renderWithProviders } from '../test/render';

type ListFixture = {
  id: number;
  name: string;
  description: string;
  items: Array<{
    id: number;
    title: string;
    posterUrl?: string;
  }>;
};

const listState = vi.hoisted(() => ({
  data: null as ListFixture | null,
  isLoading: false,
  error: null as Error | null,
  refetch: vi.fn(),
  updateList: { mutateAsync: vi.fn(), isPending: false },
  deleteList: { mutateAsync: vi.fn(), isPending: false },
  removeItem: { mutateAsync: vi.fn(), isPending: false },
}));

vi.mock('../hooks/useLists', () => ({
  useList: () => ({
    data: listState.data,
    isLoading: listState.isLoading,
    error: listState.error,
    refetch: listState.refetch,
  }),
  useUpdateList: () => listState.updateList,
  useDeleteList: () => listState.deleteList,
  useRemoveItemFromList: () => listState.removeItem,
}));

describe('ListDetail', () => {
  beforeEach(() => {
    listState.data = null;
    listState.isLoading = false;
    listState.error = null;
    listState.refetch.mockReset();
    listState.updateList.mutateAsync.mockReset();
    listState.deleteList.mutateAsync.mockReset();
    listState.removeItem.mutateAsync.mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function renderListDetail() {
    return renderWithProviders(
      <MemoryRouter initialEntries={['/lists/1']}>
        <Routes>
          <Route path="/lists/:id" element={<ListDetail />} />
          <Route path="/lists" element={<div>Lists page</div>} />
        </Routes>
      </MemoryRouter>,
      { withRouter: false }
    );
  }

  function setPopulatedList() {
    listState.data = {
      id: 1,
      name: 'Watchlist',
      description: 'Movies to revisit',
      items: [
        { id: 42, title: 'Alpha', posterUrl: '/alpha.jpg' },
      ],
    };
  }

  it('renders item links as router links to item detail routes', () => {
    listState.data = {
      id: 1,
      name: 'Watchlist',
      description: '',
      items: [
        { id: 42, title: 'Alpha', posterUrl: '/alpha.jpg' },
      ],
    };

    renderListDetail();

    expect(screen.getByRole('link')).toHaveAttribute('href', '/items/42');
  });

  it('routes the empty-state browse CTA to the home page', () => {
    listState.data = {
      id: 1,
      name: 'Watchlist',
      description: '',
      items: [],
    };

    renderListDetail();

    expect(screen.getByRole('link', { name: 'Browse Items' })).toHaveAttribute('href', '/');
  });

  it('shows an accessible alert when updating the list fails', async () => {
    const user = userEvent.setup();
    setPopulatedList();
    listState.updateList.mutateAsync.mockRejectedValueOnce(new Error('update failed'));
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    renderListDetail();

    await user.click(screen.getByRole('button', { name: 'Edit' }));
    await user.clear(screen.getByLabelText('List Name'));
    await user.type(screen.getByLabelText('List Name'), 'Updated watchlist');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Failed to update list. Please try again.');
    expect(screen.getByText('Edit List')).toBeInTheDocument();
  });

  it('shows an accessible alert when deleting the list fails', async () => {
    const user = userEvent.setup();
    setPopulatedList();
    listState.deleteList.mutateAsync.mockRejectedValueOnce(new Error('delete failed'));
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    renderListDetail();

    await user.click(screen.getByRole('button', { name: 'Delete' }));
    await user.click(screen.getAllByRole('button', { name: 'Delete' }).at(-1)!);

    expect(await screen.findByRole('alert')).toHaveTextContent('Failed to delete list. Please try again.');
    expect(screen.getByText('Delete List')).toBeInTheDocument();
  });

  it('shows an accessible alert when removing an item fails', async () => {
    const user = userEvent.setup();
    setPopulatedList();
    listState.removeItem.mutateAsync.mockRejectedValueOnce(new Error('remove failed'));
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    renderListDetail();

    await user.click(screen.getByRole('button', { name: 'Remove Alpha from list' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Failed to remove item from list. Please try again.');
    expect(screen.getByText('Alpha')).toBeInTheDocument();
  });
});
