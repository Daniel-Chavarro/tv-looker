import type { UUID } from "crypto";

export type Review = {
  id: number;
  userId: UUID;
  itemId: number;
  rating: number; // 1-5
  content: string;
  createdAt: string;
  updatedAt?: string;
  userName?: string;
}

export type CreateReviewRequest = {
  userId: UUID;
  itemId: number;
  rating: number;
  content: string;
}

export type UpdateReviewRequest = {
  rating?: number;
  content?: string;
}

export type ReviewResponse = {
  data: Review;
}

export type ReviewsListResponse = {
  data: Review[];
  count: number;
}
