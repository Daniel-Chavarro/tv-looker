import { apiClient } from './client';
import type { Item, ItemResponse, ItemsListResponse } from '../types';

export const itemsApi = {
  getItem: async (id: number): Promise<ItemResponse> => {
    const response = await apiClient.get<ItemResponse>(`/items/${id}`);
    return response.data;
  },

  getItems: async (params?: {
    type?: 'MOVIE' | 'TV';
    genreId?: number;
    search?: string;
    page?: number;
    pageSize?: number;
  }): Promise<ItemsListResponse> => {
    const response = await apiClient.get<ItemsListResponse>('/items', { params });
    return response.data;
  },
};