import { useRecommendations } from '../hooks/useRecommendations';
import { ItemGrid } from '../components/features/ItemGrid';
import { PageLoader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { useAuth } from '../hooks/useAuth';

export function Recommendations() {
  const { user } = useAuth();
  const { data, isLoading, error, refetch } = useRecommendations(user?.id, 20);

  if (isLoading) {
    return <PageLoader message="Loading recommendations..." />;
  }

  if (error) {
    return (
      <ErrorMessage
        message="Failed to load recommendations. Please try again."
        retry={() => refetch()}
      />
    );
  }

  const items = data?.items ?? [];

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-neutral-100 tracking-tight mb-2">
        Recommended for You
      </h1>
      <p className="text-neutral-500 mb-8">
        Personalized recommendations based on your preferences
      </p>

      {items.length > 0 ? (
        <ItemGrid items={items} />
      ) : (
        <p className="text-neutral-500">
          No recommendations available yet. Rate some items to get better
          recommendations.
        </p>
      )}
    </div>
  );
}