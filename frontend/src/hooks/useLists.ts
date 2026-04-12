import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listsApi } from '../api/lists';
import type { CreateListRequest, UpdateListRequest } from '../types';

export const useLists = (userId: string) => {
  return useQuery({
    queryKey: ['lists', userId],
    queryFn: () => listsApi.getUserLists(userId),
    staleTime: 3 * 60 * 1000,
    enabled: !!userId,
  });
};

export const useList = (id: number) => {
  return useQuery({
    queryKey: ['list', id],
    queryFn: () => listsApi.getList(id),
    staleTime: 3 * 60 * 1000,
    enabled: !!id,
  });
};

export const useCreateList = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateListRequest) => listsApi.createList(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['lists', data.data.userId] });
    },
  });
};

export const useUpdateList = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateListRequest }) =>
      listsApi.updateList(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['lists'] });
      queryClient.invalidateQueries({ queryKey: ['list', variables.id] });
    },
  });
};

export const useDeleteList = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => listsApi.deleteList(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lists'] });
    },
  });
};

export const useAddItemToList = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ listId, itemId }: { listId: number; itemId: number }) =>
      listsApi.addItemToList(listId, itemId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['lists'] });
      queryClient.invalidateQueries({ queryKey: ['list', variables.listId] });
    },
  });
};

export const useRemoveItemFromList = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ listId, itemId }: { listId: number; itemId: number }) =>
      listsApi.removeItemFromList(listId, itemId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['lists'] });
      queryClient.invalidateQueries({ queryKey: ['list', variables.listId] });
    },
  });
};