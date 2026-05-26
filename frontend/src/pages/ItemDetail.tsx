import { useParams, Link } from 'react-router-dom';
import { useItem } from '../hooks/useItems';
import { PageLoader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { RatingDisplay } from '../components/common/Rating';
import { ReviewCard } from '../components/features/ReviewCard';
import { useReviews } from '../hooks/useReviews';
import type { Item } from '../types/item';

const ItemDetailContent: React.FC<{ item: Item }> = ({ item }) => {
  const { data: reviewsData, isLoading: reviewsLoading, error: reviewsError } = useReviews(item.id, { pageSize: 5 });

  return (
    <div className="min-h-screen">
      <div className="relative h-[50vh] min-h-[400px]">
        {item.backdropUrl && (
          <div className="absolute inset-0 pointer-events-none">
            <img
              src={item.backdropUrl}
              alt={item.title}
              className="w-full h-full object-cover"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-neutral-950 via-neutral-950/60 to-transparent" />
          </div>
        )}
        <div className="absolute inset-0 pointer-events-none bg-gradient-to-r from-neutral-950/90 via-neutral-950/40 to-transparent" />
      </div>

      <div className="container mx-auto px-4 relative -mt-48 z-20 pb-16">
        <div className="flex flex-col lg:flex-row gap-8 rounded-sm bg-neutral-950/80 backdrop-blur-sm p-6 shadow-2xl shadow-black/30">
          <div className="flex-shrink-0">
            <div className="w-64 aspect-[2/3] rounded-sm overflow-hidden bg-neutral-900 shadow-2xl shadow-black/50">
              {item.posterUrl ? (
                <img
                  src={item.posterUrl}
                  alt={item.title}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <span className="text-neutral-700 text-6xl">?</span>
                </div>
              )}
            </div>
          </div>

          <div className="flex-1">
            <div className="mb-2">
              <span className="inline-block px-3 py-1 text-xs tracking-wider uppercase bg-amber-500/20 text-amber-400 border border-amber-500/30 rounded-sm">
                {item.type === 'MOVIE' ? 'Movie' : 'TV Show'}
              </span>
            </div>

            <h1 className="text-4xl lg:text-5xl font-bold text-neutral-100 mb-4 tracking-tight">
              {item.title}
            </h1>

            <div className="flex items-center gap-6 mb-6">
              <RatingDisplay value={item.voteAverage} size="lg" />
              <span className="text-neutral-400">
                {new Date(item.releaseDate).getFullYear()}
              </span>
              <span className="text-neutral-500">
                {item.type === 'MOVIE' ? 'Movie' : 'Series'}
              </span>
            </div>

            <div className="flex flex-wrap gap-2 mb-8">
              {item.genres.map((genre) => (
                <span
                  key={genre.id}
                  className="px-3 py-1 text-sm bg-neutral-800 text-neutral-300 rounded-sm"
                >
                  {genre.name}
                </span>
              ))}
            </div>

            <p className="text-neutral-300 text-lg leading-relaxed mb-8 max-w-2xl">
              {item.overview}
            </p>

            {item.directors.length > 0 && (
              <div className="mb-8">
                <h3 className="text-sm font-medium text-neutral-500 tracking-wider uppercase mb-3">
                  Director{item.directors.length > 1 ? 's' : ''}
                </h3>
                <div className="flex gap-2">
                  {item.directors.map((director) => (
                    <span key={director.id} className="text-neutral-200">
                      {director.name}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {item.actors.length > 0 && (
              <div>
                <h3 className="text-sm font-medium text-neutral-500 tracking-wider uppercase mb-3">
                  Cast
                </h3>
                <div className="flex flex-wrap gap-4">
                  {item.actors.slice(0, 6).map((actor) => (
                    <div key={actor.id} className="text-center">
                      <div className="w-16 h-16 rounded-full bg-neutral-800 flex items-center justify-center mb-2">
                        <span className="text-neutral-500 text-xl">?</span>
                      </div>
                      <p className="text-neutral-200 text-sm">{actor.actorName}</p>
                      <p className="text-neutral-600 text-xs">{actor.characterName}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="mt-16">
          <h2 className="text-2xl font-semibold text-neutral-200 tracking-wide mb-6">
            Reviews
          </h2>
          {reviewsLoading ? (
            <PageLoader message="Loading reviews..." />
          ) : reviewsError ? (
            <div role="alert" aria-live="assertive">
              <ErrorMessage
                message="Failed to load reviews. Please try again."
                className="items-start text-left p-4"
              />
            </div>
          ) : reviewsData?.content && reviewsData.content.length > 0 ? (
            <div className="grid gap-4">
              {reviewsData.content.map((review) => (
                <ReviewCard key={review.id} review={review} />
              ))}
            </div>
          ) : (
            <p className="text-neutral-500">No reviews yet.</p>
          )}
        </div>
      </div>
    </div>
  );
};

export function ItemDetail() {
  const { id } = useParams<{ id: string }>();
  const itemId = Number(id);
  const { data, isLoading, error, refetch } = useItem(itemId);

  if (isLoading) {
    return <PageLoader message="Loading item details..." />;
  }

  if (error || !data) {
    return (
      <ErrorMessage
        message="Failed to load item details. Please try again."
        retry={() => refetch()}
      />
    );
  }

  return <ItemDetailContent item={data} />;
}
