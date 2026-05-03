import { useQuery } from '@tanstack/react-query';
import { recommendationsApi } from '../api/recommendations';

export const useRecommendations = (
  userId?: string,
  limit?: number
) => {
  return useQuery({
    queryKey: ['recommendations', userId, limit],
    queryFn: () => recommendationsApi.getRecommendations(userId as import('crypto').UUID),
    staleTime: 5 * 60 * 1000,
    enabled: !!userId,
  });
};