import { apiClient } from './client';
import type { Item, RecommendationResponse } from '../types';
import type { UUID } from 'crypto';

export const recommendationsApi = {
  getRecommendations: async (userId: UUID, params?: {
    // itemId?: number;
    // type?: 'MOVIE' | 'TV';
    // limit?: number;
  }): Promise<RecommendationResponse> => {
    const response = await apiClient.get<RecommendationResponse>(`/users/${userId}/recommendations`, { params });
    return response.data;
  },
};