import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Home } from './Home';
import { renderWithProviders } from '../test/render';

const homeState = vi.hoisted(() => ({
  params: [] as unknown[],
  isLoading: false,
  error: null as Error | null,
  refetch: vi.fn(),
  data: {
    content: [
      {
        id: 1,
        title: 'Alien',
        overview: 'A crew encounters horror.',
        releaseDate: '1979-05-25',
        popularity: 100,
        voteAverage: 8.5,
        type: 'MOVIE',
        tmdbId: 1091,
        genres: [],
        actors: [],
        directors: [],
      },
    ],
    totalPages: 4,
  },
}));

vi.mock('../hooks/useItems', () => ({
  useItems: (params?: unknown) => {
    homeState.params.push(params);
    return {
      data: homeState.data,
      isLoading: homeState.isLoading,
      error: homeState.error,
      refetch: homeState.refetch,
    };
  },
}));

describe('Home', () => {
  beforeEach(() => {
    homeState.params = [];
    homeState.isLoading = false;
    homeState.error = null;
    homeState.refetch.mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('uses fixed page size and submits search before querying', async () => {
    const user = userEvent.setup();

    renderWithProviders(<Home />);

    expect(homeState.params.at(-1)).toEqual({ page: 1, size: 20 });

    await user.type(screen.getByRole('searchbox', { name: /search items/i }), 'Alien');
    expect(homeState.params.at(-1)).toEqual({ page: 1, size: 20 });

    await user.click(screen.getByRole('button', { name: 'Search' }));

    expect(homeState.params.at(-1)).toEqual({ search: 'Alien', page: 1, size: 20 });
  });

  it('resets to the first page when filters change', async () => {
    const user = userEvent.setup();

    renderWithProviders(<Home />);

    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(homeState.params.at(-1)).toEqual({ page: 2, size: 20 });

    await user.selectOptions(screen.getByRole('combobox', { name: 'Type' }), 'TV');

    expect(homeState.params.at(-1)).toEqual({ type: 'TV', page: 1, size: 20 });
  });
});
