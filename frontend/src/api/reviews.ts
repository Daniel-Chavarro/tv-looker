import { apiClient } from './client';
import type { Review, CreateReviewRequest, UpdateReviewRequest } from '../types';
import type { PaginatedResponse } from '../types';

export const reviewsApi = {
  getReview: async (id: number): Promise<Review> => {
    const response = await apiClient.get<Review>(`/reviews/${id}`);
    return response.data;
  },

  getItemReviews: async (itemId: number, params?: { page?: number; pageSize?: number }): Promise<PaginatedResponse<Review>> => {
    const response = await apiClient.get<PaginatedResponse<Review>>(`/reviews/item/${itemId}`, { params });
    return response.data;
  },

  getUserReviews: async (userId: string, params?: { page?: number; pageSize?: number }): Promise<PaginatedResponse<Review>> => {
    const response = await apiClient.get<PaginatedResponse<Review>>(`/reviews/user/${userId}`, { params });
    return response.data;
  },

  createReview: async (data: CreateReviewRequest): Promise<Review> => {
    const response = await apiClient.post<Review>('/reviews', data);
    return response.data;
  },

  updateReview: async (id: number, data: UpdateReviewRequest): Promise<Review> => {
    const response = await apiClient.patch<Review>(`/reviews/${id}`, data);
    return response.data;
  },

  deleteReview: async (id: number): Promise<void> => {
    await apiClient.delete(`/reviews/${id}`);
  },
};