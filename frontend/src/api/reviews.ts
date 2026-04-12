import { apiClient } from './client';
import type { Review, CreateReviewRequest, UpdateReviewRequest, ReviewResponse, ReviewsListResponse } from '../types';

export const reviewsApi = {
  getReview: async (id: number): Promise<ReviewResponse> => {
    const response = await apiClient.get<ReviewResponse>(`/reviews/${id}`);
    return response.data;
  },

  getItemReviews: async (itemId: number, params?: { page?: number; pageSize?: number }): Promise<ReviewsListResponse> => {
    const response = await apiClient.get<ReviewsListResponse>(`/reviews/item/${itemId}`, { params });
    return response.data;
  },

  getUserReviews: async (userId: string, params?: { page?: number; pageSize?: number }): Promise<ReviewsListResponse> => {
    const response = await apiClient.get<ReviewsListResponse>(`/reviews/user/${userId}`, { params });
    return response.data;
  },

  createReview: async (data: CreateReviewRequest): Promise<ReviewResponse> => {
    const response = await apiClient.post<ReviewResponse>('/reviews', data);
    return response.data;
  },

  updateReview: async (id: number, data: UpdateReviewRequest): Promise<ReviewResponse> => {
    const response = await apiClient.patch<ReviewResponse>(`/reviews/${id}`, data);
    return response.data;
  },

  deleteReview: async (id: number): Promise<void> => {
    await apiClient.delete(`/reviews/${id}`);
  },
};