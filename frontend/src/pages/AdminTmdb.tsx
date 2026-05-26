import { ErrorMessage } from '../components/common/ErrorMessage';
import { PageLoader } from '../components/common/Loader';
import { Button } from '../components/common/Button';
import { useCollectTmdb, useCollectTmdbGenres, useCollectTmdbMovies, useCollectTmdbTvShows, useSyncTmdb, useTmdbAdminStatus } from '../hooks/useTmdbAdmin';

export function AdminTmdb() {
  const { data, isLoading, error, refetch } = useTmdbAdminStatus();
  const collectAll = useCollectTmdb();
  const collectGenres = useCollectTmdbGenres();
  const collectMovies = useCollectTmdbMovies();
  const collectTvShows = useCollectTmdbTvShows();
  const syncTmdb = useSyncTmdb();

  if (isLoading) {
    return <PageLoader message="Loading TMDB admin status..." />;
  }

  if (error) {
    return <ErrorMessage message="Failed to load TMDB admin status. Please try again." retry={() => refetch()} />;
  }

  return (
    <div className="container mx-auto px-4 py-8 space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-neutral-100 tracking-tight mb-2">TMDB Admin</h1>
        <p className="text-neutral-500">Manage TMDB collection and synchronization tasks.</p>
      </div>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <div className="rounded-sm border border-neutral-800 bg-neutral-900/40 p-4 space-y-2">
          <h2 className="text-lg font-medium text-neutral-100">Status</h2>
          <p className="text-sm text-neutral-400">Collector running: {data?.collectorRunning ? 'Yes' : 'No'}</p>
          <p className="text-sm text-neutral-400">Sync enabled: {data?.syncEnabled ? 'Yes' : 'No'}</p>
          <p className="text-sm text-neutral-400">Last sync: {data?.lastSyncDate ?? 'Never'}</p>
          <p className="text-xs text-neutral-600 break-all">Updated: {data?.timestamp}</p>
        </div>

        <div className="rounded-sm border border-neutral-800 bg-neutral-900/40 p-4 space-y-3">
          <h2 className="text-lg font-medium text-neutral-100">Collection</h2>
          <Button isLoading={collectAll.isPending} onClick={() => collectAll.mutate()} className="w-full">Collect All</Button>
          <Button variant="secondary" isLoading={collectGenres.isPending} onClick={() => collectGenres.mutate()} className="w-full">Collect Genres</Button>
          <Button variant="secondary" isLoading={collectMovies.isPending} onClick={() => collectMovies.mutate()} className="w-full">Collect Movies</Button>
          <Button variant="secondary" isLoading={collectTvShows.isPending} onClick={() => collectTvShows.mutate()} className="w-full">Collect TV Shows</Button>
        </div>

        <div className="rounded-sm border border-neutral-800 bg-neutral-900/40 p-4 space-y-3">
          <h2 className="text-lg font-medium text-neutral-100">Synchronization</h2>
          <Button isLoading={syncTmdb.isPending} onClick={() => syncTmdb.mutate()} className="w-full">Sync TMDB</Button>
        </div>
      </section>
    </div>
  );
}
