import { apiClient } from './client';
import type { FavoriteList, CreateListRequest, UpdateListRequest, ListResponse, ListsListResponse } from '../types';

export const listsApi = {
  getList: async (id: number): Promise<ListResponse> => {
    const response = await apiClient.get<ListResponse>(`/lists/${id}`);
    return response.data;
  },

  getUserLists: async (userId: string): Promise<ListsListResponse> => {
    const response = await apiClient.get<ListsListResponse>(`/lists/user/${userId}`);
    return response.data;
  },

  createList: async (data: CreateListRequest): Promise<ListResponse> => {
    const response = await apiClient.post<ListResponse>('/lists', data);
    return response.data;
  },

  updateList: async (id: number, data: UpdateListRequest): Promise<ListResponse> => {
    const response = await apiClient.patch<ListResponse>(`/lists/${id}`, data);
    return response.data;
  },

  deleteList: async (id: number): Promise<void> => {
    await apiClient.delete(`/lists/${id}`);
  },

  addItemToList: async (listId: number, itemId: number): Promise<ListResponse> => {
    const response = await apiClient.post<ListResponse>(`/lists/${listId}/items`, { itemId });
    return response.data;
  },

  removeItemFromList: async (listId: number, itemId: number): Promise<void> => {
    await apiClient.delete(`/lists/${listId}/items/${itemId}`);
  },
};