import type { UUID } from "crypto";

export type Review = {
  id: number;
  userId: UUID;
  itemId: number;
  rating: number; // 1-5
  comment: string;
  createdAt: string;
};

export type CreateReviewRequest = {
  userId: UUID;
  itemId: number;
  rating: number;
  comment: string;
}

export type UpdateReviewRequest = {
  score?: number;
  reviewText?: string;
};
