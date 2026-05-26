import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdminTmdb } from './AdminTmdb';
import { renderWithProviders } from '../test/render';

const tmdbState = vi.hoisted(() => ({
  data: {
    collectorRunning: false,
    lastSyncDate: '2026-05-24',
    syncEnabled: true,
    timestamp: '2026-05-24T10:00:00Z',
  },
  isLoading: false,
  error: null as Error | null,
  refetch: vi.fn(),
  collectAll: { mutate: vi.fn(), isPending: false },
  collectGenres: { mutate: vi.fn(), isPending: false },
  collectMovies: { mutate: vi.fn(), isPending: false },
  collectTvShows: { mutate: vi.fn(), isPending: false },
  sync: { mutate: vi.fn(), isPending: false },
}));

vi.mock('../hooks/useTmdbAdmin', () => ({
  useTmdbAdminStatus: () => ({
    data: tmdbState.data,
    isLoading: tmdbState.isLoading,
    error: tmdbState.error,
    refetch: tmdbState.refetch,
  }),
  useCollectTmdb: () => tmdbState.collectAll,
  useCollectTmdbGenres: () => tmdbState.collectGenres,
  useCollectTmdbMovies: () => tmdbState.collectMovies,
  useCollectTmdbTvShows: () => tmdbState.collectTvShows,
  useSyncTmdb: () => tmdbState.sync,
}));

describe('AdminTmdb', () => {
  beforeEach(() => {
    tmdbState.isLoading = false;
    tmdbState.error = null;
    tmdbState.refetch.mockReset();
    tmdbState.collectAll.mutate.mockReset();
    tmdbState.collectGenres.mutate.mockReset();
    tmdbState.collectMovies.mutate.mockReset();
    tmdbState.collectTvShows.mutate.mockReset();
    tmdbState.sync.mutate.mockReset();
  });

  it('shows loading state while fetching status', () => {
    tmdbState.isLoading = true;

    renderWithProviders(<AdminTmdb />);

    expect(screen.getByText('Loading TMDB admin status...')).toBeInTheDocument();
  });

  it('shows status and triggers tmdb actions', async () => {
    const user = userEvent.setup();

    renderWithProviders(<AdminTmdb />);

    expect(screen.getByText('Collector running: No')).toBeInTheDocument();
    expect(screen.getByText('Sync enabled: Yes')).toBeInTheDocument();
    expect(screen.getByText('Last sync: 2026-05-24')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Collect All' }));
    await user.click(screen.getByRole('button', { name: 'Sync TMDB' }));

    expect(tmdbState.collectAll.mutate).toHaveBeenCalledTimes(1);
    expect(tmdbState.sync.mutate).toHaveBeenCalledTimes(1);
  });

  it('shows error state and retries status load', async () => {
    const user = userEvent.setup();
    tmdbState.error = new Error('failed');

    renderWithProviders(<AdminTmdb />);

    expect(screen.getByText('Failed to load TMDB admin status. Please try again.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Try Again' }));

    expect(tmdbState.refetch).toHaveBeenCalledTimes(1);
  });
});
