import { useQuery } from '@tanstack/react-query';
import { itemsApi, type ItemQueryParams } from '../api/items';

export const useItems = (params?: ItemQueryParams) => {
  return useQuery({
    queryKey: ['items', params],
    queryFn: () => itemsApi.getItems(params),
    staleTime: 5 * 60 * 1000,
  });
};

export const useItem = (id: number) => {
  return useQuery({
    queryKey: ['item', id],
    queryFn: () => itemsApi.getItem(id),
    staleTime: 5 * 60 * 1000,
    enabled: !!id,
  });
};
