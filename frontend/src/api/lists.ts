import { apiClient } from './client';
import type { FavoriteList, CreateListRequest, UpdateListRequest} from '../types';
import type { PaginatedResponse } from '../types';

export const listsApi = {
  getList: async (id: number): Promise<FavoriteList> => {
    const response = await apiClient.get<FavoriteList>(`/lists/${id}`);
    return response.data;
  },

  getUserLists: async (userId: string): Promise<PaginatedResponse<FavoriteList>> => {
    const response = await apiClient.get<PaginatedResponse<FavoriteList>>(`/lists/user/${userId}`);
    return response.data;
  },

  createList: async (data: CreateListRequest): Promise<FavoriteList> => {
    const response = await apiClient.post<FavoriteList>('/lists', data);
    return response.data;
  },

  updateList: async (id: number, data: UpdateListRequest): Promise<FavoriteList> => {
    const response = await apiClient.patch<FavoriteList>(`/lists/${id}`, data);
    return response.data;
  },

  deleteList: async (id: number): Promise<void> => {
    await apiClient.delete(`/lists/${id}`);
  },

  addItemToList: async (listId: number, itemId: number): Promise<FavoriteList> => {
    const response = await apiClient.post<FavoriteList>(`/lists/${listId}/items`, { itemId });
    return response.data;
  },

  removeItemFromList: async (listId: number, itemId: number): Promise<void> => {
    await apiClient.delete(`/lists/${listId}/items/${itemId}`);
  },
};