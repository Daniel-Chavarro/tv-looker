import { describe, expect, it, vi, beforeEach } from 'vitest';
import { waitFor } from '@testing-library/react';
import { useRecommendations } from './useRecommendations';
import { renderWithProviders } from '../test/render';

const { getRecommendations } = vi.hoisted(() => ({
  getRecommendations: vi.fn(),
}));

vi.mock('../api/recommendations', () => ({
  recommendationsApi: {
    getRecommendations,
  },
}));

function TestComponent({ userId, limit }: { userId?: string; limit?: number }) {
  useRecommendations(userId, limit);
  return null;
}

describe('useRecommendations', () => {
  const userId = '123e4567-e89b-12d3-a456-426614174000';

  beforeEach(() => {
    getRecommendations.mockReset();
  });

  it('forwards a positive limit to the API', async () => {
    getRecommendations.mockResolvedValue({
      userId,
      items: [],
      count: 0,
    });

    renderWithProviders(<TestComponent userId={userId} limit={20} />);

    await waitFor(() => {
      expect(getRecommendations).toHaveBeenCalledWith(userId, { limit: 20 });
    });
  });

  it('omits params when limit is undefined or zero', async () => {
    getRecommendations.mockResolvedValue({
      userId,
      items: [],
      count: 0,
    });

    renderWithProviders(<TestComponent userId={userId} />);
    await waitFor(() => {
      expect(getRecommendations).toHaveBeenCalledWith(userId, undefined);
    });

    getRecommendations.mockClear();

    renderWithProviders(<TestComponent userId={userId} limit={0} />);
    await waitFor(() => {
      expect(getRecommendations).toHaveBeenCalledWith(userId, undefined);
    });
  });
});
