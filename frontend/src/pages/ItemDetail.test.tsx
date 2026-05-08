import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ItemDetail } from './ItemDetail';
import { renderWithProviders } from '../test/render';

const itemState = vi.hoisted(() => ({
  item: {
    id: 42,
    title: 'Signal Moon',
    overview: 'A quiet sci-fi story.',
    releaseDate: '2024-01-01',
    popularity: 12,
    voteAverage: 8,
    type: 'MOVIE' as const,
    tmdbId: 420,
    genres: [],
    actors: [],
    directors: [],
  },
  itemLoading: false,
  itemError: null as Error | null,
  itemRefetch: vi.fn(),
  reviewsData: null as { content: unknown[] } | null,
  reviewsLoading: false,
  reviewsError: null as Error | null,
}));

vi.mock('../hooks/useItems', () => ({
  useItem: () => ({
    data: itemState.item,
    isLoading: itemState.itemLoading,
    error: itemState.itemError,
    refetch: itemState.itemRefetch,
  }),
}));

vi.mock('../hooks/useReviews', () => ({
  useReviews: () => ({
    data: itemState.reviewsData,
    isLoading: itemState.reviewsLoading,
    error: itemState.reviewsError,
  }),
}));

function renderItemDetail() {
  return renderWithProviders(
    <MemoryRouter initialEntries={['/items/42']}>
      <Routes>
        <Route path="/items/:id" element={<ItemDetail />} />
      </Routes>
    </MemoryRouter>,
    { withRouter: false }
  );
}

describe('ItemDetail', () => {
  beforeEach(() => {
    itemState.itemLoading = false;
    itemState.itemError = null;
    itemState.itemRefetch.mockReset();
    itemState.reviewsData = null;
    itemState.reviewsLoading = false;
    itemState.reviewsError = null;
  });

  it('shows a review load alert instead of the empty reviews state', () => {
    itemState.reviewsError = new Error('reviews failed');

    renderItemDetail();

    expect(screen.getByRole('alert')).toHaveTextContent('Failed to load reviews. Please try again.');
    expect(screen.queryByText('No reviews yet.')).not.toBeInTheDocument();
  });

  it('keeps the empty reviews state for a successful empty response', () => {
    itemState.reviewsData = { content: [] };

    renderItemDetail();

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByText('No reviews yet.')).toBeInTheDocument();
  });
});
