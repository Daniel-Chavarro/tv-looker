import { apiClient } from './client';
import type { TmdbOperationResult, TmdbStatusResponse } from '../types';

export const tmdbAdminApi = {
  getStatus: async (): Promise<TmdbStatusResponse> => {
    const response = await apiClient.get<TmdbStatusResponse>('/admin/tmdb/status');
    return response.data;
  },

  collectAll: async (): Promise<TmdbOperationResult> => {
    const response = await apiClient.post<TmdbOperationResult>('/admin/tmdb/collect');
    return response.data;
  },

  collectGenres: async (): Promise<TmdbOperationResult> => {
    const response = await apiClient.post<TmdbOperationResult>('/admin/tmdb/collect/genres');
    return response.data;
  },

  collectMovies: async (): Promise<TmdbOperationResult> => {
    const response = await apiClient.post<TmdbOperationResult>('/admin/tmdb/collect/movies');
    return response.data;
  },

  collectTvShows: async (): Promise<TmdbOperationResult> => {
    const response = await apiClient.post<TmdbOperationResult>('/admin/tmdb/collect/tvshows');
    return response.data;
  },

  sync: async (): Promise<TmdbOperationResult> => {
    const response = await apiClient.post<TmdbOperationResult>('/admin/tmdb/sync');
    return response.data;
  },
};
