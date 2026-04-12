import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { reviewsApi } from '../api/reviews';
import type { CreateReviewRequest, UpdateReviewRequest } from '../types';

export const useReviews = (itemId?: number, params?: { page?: number; pageSize?: number }) => {
  return useQuery({
    queryKey: ['reviews', 'item', itemId, params],
    queryFn: () => itemId ? reviewsApi.getItemReviews(itemId, params) : reviewsApi.getItemReviews(0, params),
    staleTime: 3 * 60 * 1000,
    enabled: !!itemId,
  });
};

export const useUserReviews = (userId: string, params?: { page?: number; pageSize?: number }) => {
  return useQuery({
    queryKey: ['reviews', 'user', userId, params],
    queryFn: () => reviewsApi.getUserReviews(userId, params),
    staleTime: 3 * 60 * 1000,
    enabled: !!userId,
  });
};



export const useReview = (id: number) => {
  return useQuery({
    queryKey: ['review', id],
    queryFn: () => reviewsApi.getReview(id),
    staleTime: 3 * 60 * 1000,
    enabled: !!id,
  });
};

export const useCreateReview = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateReviewRequest) => reviewsApi.createReview(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
    },
  });
};

export const useUpdateReview = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateReviewRequest }) =>
      reviewsApi.updateReview(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
      queryClient.invalidateQueries({ queryKey: ['review', variables.id] });
    },
  });
};

export const useDeleteReview = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => reviewsApi.deleteReview(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
    },
  });
};