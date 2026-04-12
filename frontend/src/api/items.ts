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

  getTrending: async (type?: 'MOVIE' | 'TV'): Promise<ItemsListResponse> => {
    const response = await apiClient.get<ItemsListResponse>('/items/trending', { params: { type } });
    return response.data;
  },

  getPopular: async (type?: 'MOVIE' | 'TV'): Promise<ItemsListResponse> => {
    const response = await apiClient.get<ItemsListResponse>('/items/popular', { params: { type } });
    return response.data;
  },

  getTopRated: async (type?: 'MOVIE' | 'TV'): Promise<ItemsListResponse> => {
    const response = await apiClient.get<ItemsListResponse>('/items/top-rated', { params: { type } });
    return response.data;
  },
};