import { apiClient } from './client';
import type { Item } from '../types';
import type { PaginatedResponse } from '../types';

type ItemResponse = {
  id: number;
  title: string;
  overview: string;
  releaseDate: string;
  popularity: number;
  voteAverage: number;
  tmdbType: Item['type'];
  tmdbId: number;
  genreResponses?: Item['genres'];
  actorItemResponses?: Item['actors'];
  directorResponses?: Item['directors'];
};

const toItem = (item: ItemResponse): Item => ({
  id: item.id,
  title: item.title,
  overview: item.overview,
  releaseDate: item.releaseDate,
  popularity: item.popularity,
  voteAverage: item.voteAverage,
  type: item.tmdbType,
  tmdbId: item.tmdbId,
  genres: item.genreResponses ?? [],
  actors: item.actorItemResponses ?? [],
  directors: item.directorResponses ?? [],
});

export interface ItemQueryParams {
  type?: 'MOVIE' | 'TV';
  genreId?: number;
  search?: string;
  year?: number;
  rating?: number;
  page?: number;
  size?: number;
}

export const itemsApi = {
  getItem: async (id: number): Promise<Item> => {
    const response = await apiClient.get<ItemResponse>(`/items/${id}`);
    return toItem(response.data);
  },

  getItems: async (params?: ItemQueryParams): Promise<PaginatedResponse<Item>> => {
    const response = await apiClient.get<PaginatedResponse<ItemResponse>>('/items', { params });
    return {
      ...response.data,
      content: response.data.content.map(toItem),
    };
  },
};
