import { apiClient } from './client';
import type { Item, RecommendationResponse } from '../types';

export const recommendationsApi = {
  getRecommendations: async (params?: {
    itemId?: number;
    userId?: string;
    type?: 'MOVIE' | 'TV';
    limit?: number;
  }): Promise<RecommendationResponse> => {
    const response = await apiClient.get<RecommendationResponse>('/recommendations', { params });
    return response.data;
  },
};