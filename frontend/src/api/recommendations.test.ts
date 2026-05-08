import { describe, expect, it, vi, beforeEach } from 'vitest';
import type { RecommendationResponse } from '../types';

const get = vi.fn();

vi.mock('./client', () => ({
  apiClient: {
    get,
  },
}));

describe('recommendationsApi', () => {
  const userId = '123e4567-e89b-12d3-a456-426614174000';

  beforeEach(() => {
    get.mockReset();
  });

  it('omits params when no positive limit is provided', async () => {
    const response: RecommendationResponse = {
      userId,
      items: [],
      count: 0,
    };

    get.mockResolvedValue({ data: response });

    const { recommendationsApi } = await import('./recommendations');
    const result = await recommendationsApi.getRecommendations(userId);

    expect(get).toHaveBeenCalledWith(`/users/${userId}/recommendations`);
    expect(result).toEqual(response);
  });

  it('sends limit params when positive limit is provided', async () => {
    const response: RecommendationResponse = {
      userId,
      items: [],
      count: 0,
    };

    get.mockResolvedValue({ data: response });

    const { recommendationsApi } = await import('./recommendations');
    const result = await recommendationsApi.getRecommendations(userId, { limit: 20 });

    expect(get).toHaveBeenCalledWith(`/users/${userId}/recommendations`, {
      params: { limit: 20 },
    });
    expect(result).toEqual(response);
  });
});
