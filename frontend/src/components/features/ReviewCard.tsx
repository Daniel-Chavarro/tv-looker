import React from 'react';
import { Card, CardBody } from '../common/Card';
import { RatingDisplay } from '../common/Rating';
import type { Review } from '../../types/review';

interface ReviewCardProps {
  review: Review;
  className?: string;
}

export const ReviewCard: React.FC<ReviewCardProps> = ({ review, className = '' }) => {
  const formattedDate = new Date(review.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });

  return (
    <Card className={className}>
      <CardBody>
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-neutral-800 flex items-center justify-center">
              <span className="text-amber-500 text-lg font-medium">
                {'U'[0].toUpperCase()}
              </span>
            </div>
            <div>
              <p className="text-neutral-200 font-medium">
                {'Anonymous'}
              </p>
              <p className="text-neutral-600 text-sm">{formattedDate}</p>
            </div>
          </div>
          <RatingDisplay value={review.rating} size="sm" />
        </div>
        <div className="mt-4">
          <p className="text-neutral-300 leading-relaxed">{review.comment}</p>
        </div>
      </CardBody>
    </Card>
  );
};