import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { tmdbAdminApi } from '../api/tmdbAdmin';

const tmdbAdminQueryKey = ['admin', 'tmdb', 'status'];

export function useTmdbAdminStatus() {
  return useQuery({
    queryKey: tmdbAdminQueryKey,
    queryFn: () => tmdbAdminApi.getStatus(),
    staleTime: 30 * 1000,
  });
}

function useTmdbAdminMutation(mutationFn: () => Promise<unknown>) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tmdbAdminQueryKey });
    },
  });
}

export function useCollectTmdb() {
  return useTmdbAdminMutation(() => tmdbAdminApi.collectAll());
}

export function useCollectTmdbGenres() {
  return useTmdbAdminMutation(() => tmdbAdminApi.collectGenres());
}

export function useCollectTmdbMovies() {
  return useTmdbAdminMutation(() => tmdbAdminApi.collectMovies());
}

export function useCollectTmdbTvShows() {
  return useTmdbAdminMutation(() => tmdbAdminApi.collectTvShows());
}

export function useSyncTmdb() {
  return useTmdbAdminMutation(() => tmdbAdminApi.sync());
}
