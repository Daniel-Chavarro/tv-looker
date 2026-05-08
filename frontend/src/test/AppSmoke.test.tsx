import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from './render';
import { Home } from '../pages/Home';
import { itemsApi } from '../api/items';

vi.mock('../api/items', () => ({
  itemsApi: {
    getItems: vi.fn(),
  },
}));

const mockedGetItems = vi.mocked(itemsApi.getItems);

describe('frontend smoke test', () => {
  beforeEach(() => {
    mockedGetItems.mockResolvedValue({
      content: [
        {
          id: 1,
          title: 'Smoke Item',
          overview: 'A test item rendered through the real Home route.',
          releaseDate: '2024-01-01',
          popularity: 42,
          voteAverage: 8.7,
          type: 'MOVIE',
          tmdbId: 999,
          genres: [],
          actors: [],
          directors: [],
          posterUrl: undefined,
          backdropUrl: undefined,
        },
      ],
      actualPage: 1,
      totalPages: 1,
      totalItems: 1,
      isLast: true,
    });
  });

  it('renders the home route with fetched content', async () => {
    renderWithProviders(<Home />, { initialEntries: ['/'] });

    expect(await screen.findByRole('heading', { name: 'Featured' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Trending' })).toBeInTheDocument();
    expect(screen.getAllByRole('heading', { name: 'Smoke Item' })).toHaveLength(2);
  });
});
