import { useQuery } from '@tanstack/react-query';
import { recommendationsApi } from '../api/recommendations';
import type { UUID } from '../types';

export const useRecommendations = (
  userId?: UUID,
  limit?: number
) => {
  const recommendationLimit = typeof limit === 'number' && limit > 0 ? limit : undefined;

  return useQuery({
    queryKey: ['recommendations', userId, recommendationLimit],
    queryFn: () => recommendationsApi.getRecommendations(userId!, recommendationLimit ? { limit: recommendationLimit } : undefined),
    staleTime: 5 * 60 * 1000,
    enabled: !!userId,
  });
};
