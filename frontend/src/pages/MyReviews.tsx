import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useUserReviews, useUpdateReview, useDeleteReview } from '../hooks/useReviews';
import { PageLoader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { Button } from '../components/common/Button';
import { RatingDisplay } from '../components/common/Rating';
import { Modal } from '../components/common/Modal';
import { Input } from '../components/common/Input';
import type { Review } from '../types/review';

function ReviewItem({
  review,
  onEdit,
  onDelete,
}: {
  review: Review;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const formattedDate = new Date(review.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });

  return (
    <div className="bg-neutral-900/40 border border-neutral-800 rounded-sm p-4">
      <div className="flex items-start justify-between mb-3">
        <div>
          <Link
            to={`/items/${review.itemId}`}
            className="text-amber-400 hover:text-amber-300 font-medium"
          >
            Item #{review.itemId}
          </Link>
          <p className="text-neutral-600 text-sm">{formattedDate}</p>
        </div>
        <RatingDisplay value={review.rating} size="sm" />
      </div>
      <p className="text-neutral-300 mb-4">{review.comment}</p>
      <div className="flex gap-2">
        <Button variant="secondary" onClick={onEdit} className="text-sm">
          Edit
        </Button>
        <Button variant="danger" onClick={onDelete} className="text-sm">
          Delete
        </Button>
      </div>
    </div>
  );
}

export function MyReviews() {
  const { user } = useAuth();
  const { data, isLoading, error, refetch } = useUserReviews(user?.id ?? '');
  const updateReview = useUpdateReview();
  const deleteReview = useDeleteReview();

  const [editingReview, setEditingReview] = useState<Review | null>(null);
  const [editRating, setEditRating] = useState(5);
  const [editContent, setEditContent] = useState('');
  const [updateError, setUpdateError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const handleEdit = (review: Review) => {
    setUpdateError(null);
    setEditingReview(review);
    setEditRating(review.rating);
    setEditContent(review.comment);
  };

  const closeEditModal = () => {
    setUpdateError(null);
    setEditingReview(null);
  };

  const handleUpdateReview = async () => {
    if (!editingReview || !editContent.trim()) return;
    setUpdateError(null);
    try {
      await updateReview.mutateAsync({
        id: editingReview.id,
        data: { score: editRating, reviewText: editContent.trim() },
      });
      closeEditModal();
    } catch (err) {
      setUpdateError('Failed to update review. Please try again.');
      console.error('Failed to update review:', err);
    }
  };

  const handleDeleteReview = async (id: number) => {
    if (!confirm('Are you sure you want to delete this review?')) return;
    setDeleteError(null);
    try {
      await deleteReview.mutateAsync(id);
    } catch (err) {
      setDeleteError('Failed to delete review. Please try again.');
      console.error('Failed to delete review:', err);
    }
  };

  if (isLoading) {
    return <PageLoader message="Loading your reviews..." />;
  }

  if (error) {
    return (
      <ErrorMessage
        message="Failed to load your reviews. Please try again."
        retry={() => refetch()}
      />
    );
  }

  const reviews = data?.content ?? [];

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-neutral-100 tracking-tight mb-2">
          My Reviews
        </h1>
        <p className="text-neutral-500">Manage your reviews</p>
      </div>

      {deleteError && (
        <div
          role="alert"
          className="mb-6 rounded-sm border border-red-900/30 bg-red-950/20 px-4 py-3 text-sm text-red-300"
        >
          {deleteError}
        </div>
      )}

      {reviews.length > 0 ? (
        <div className="grid gap-4">
          {reviews.map((review) => (
            <ReviewItem
              key={review.id}
              review={review}
              onEdit={() => handleEdit(review)}
              onDelete={() => handleDeleteReview(review.id)}
            />
          ))}
        </div>
      ) : (
        <div className="text-center py-16">
          <p className="text-neutral-500 mb-4">You haven't written any reviews yet.</p>
          <Link to="/">
            <Button>Browse Items</Button>
          </Link>
        </div>
      )}

      <Modal
        isOpen={!!editingReview}
        onClose={closeEditModal}
        title="Edit Review"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm text-neutral-400 mb-2">Rating</label>
            <div className="flex gap-1">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setEditRating(star)}
                  className="p-1"
                >
                  <svg
                    className={`w-6 h-6 ${
                      star <= editRating
                        ? 'text-amber-400'
                        : 'text-neutral-600'
                    }`}
                    fill="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                  </svg>
                </button>
              ))}
            </div>
          </div>
          <Input
            label="Review"
            value={editContent}
            onChange={(e) => setEditContent(e.target.value)}
            placeholder="Write your review"
          />
          {updateError && (
            <div
              role="alert"
              className="rounded-sm border border-red-900/30 bg-red-950/20 px-4 py-3 text-sm text-red-300"
            >
              {updateError}
            </div>
          )}
          <div className="flex gap-3 pt-4">
            <Button
              variant="secondary"
              onClick={closeEditModal}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              onClick={handleUpdateReview}
              disabled={!editContent.trim() || updateReview.isPending}
              isLoading={updateReview.isPending}
              className="flex-1"
            >
              Save
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
