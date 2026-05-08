import { apiClient } from './client';
import type { RecommendationResponse, UUID } from '../types';

type RecommendationParams = {
  limit?: number;
};

export const recommendationsApi = {
  getRecommendations: async (userId: UUID, params?: RecommendationParams): Promise<RecommendationResponse> => {
    const response = params?.limit && params.limit > 0
      ? await apiClient.get<RecommendationResponse>(`/users/${userId}/recommendations`, { params })
      : await apiClient.get<RecommendationResponse>(`/users/${userId}/recommendations`);
    return response.data;
  },
};
