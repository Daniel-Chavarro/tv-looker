import type { UUID } from "crypto";

export interface Review {
  id: number;
  userId: UUID;
  itemId: number;
  rating: number; // 1-5
  content: string;
  createdAt: string;
  updatedAt?: string;
  userName?: string;
}

export interface CreateReviewRequest {
  userId: UUID;
  itemId: number;
  rating: number;
  content: string;
}

export interface UpdateReviewRequest {
  rating?: number;
  content?: string;
}

export interface ReviewResponse {
  data: Review;
}

export interface ReviewsListResponse {
  data: Review[];
  count: number;
}
