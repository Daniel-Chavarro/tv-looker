import { apiClient } from './client';
import type { Item } from '../types';
import type { PaginatedResponse } from '../types';

export const itemsApi = {
  getItem: async (id: number): Promise<Item> => {
    const response = await apiClient.get<Item>(`/items/${id}`);
    return response.data;
  },

  getItems: async (params?: {
    type?: 'MOVIE' | 'TV';
    genreId?: number;
    search?: string;
    page?: number;
    pageSize?: number;
  }): Promise<PaginatedResponse<Item>> => {
    const response = await apiClient.get<PaginatedResponse<Item>>('/items', { params });
    return response.data;
  },
};