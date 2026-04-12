import { useItems } from '../hooks/useItems';
import { ItemGrid } from '../components/features/ItemGrid';
import { PageLoader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';

export function Home() {
  const { data, isLoading, error, refetch } = useItems({ pageSize: 20 });

  if (isLoading) {
    return <PageLoader message="Loading items..." />;
  }

  if (error) {
    return (
      <ErrorMessage
        message="Failed to load items. Please try again."
        retry={() => refetch()}
      />
    );
  }

  const items = data?.data ?? [];

  return (
    <div className="container mx-auto px-4 py-8">
      <section className="mb-12">
        <h2 className="text-2xl font-semibold text-neutral-200 tracking-wide mb-6">
          Featured
        </h2>
        <ItemGrid items={items} />
      </section>

      <section>
        <h2 className="text-2xl font-semibold text-neutral-200 tracking-wide mb-6">
          Trending
        </h2>
        <ItemGrid items={items} />
      </section>
    </div>
  );
}