import type { UUID } from "crypto";
import type { Item } from "./item";

export type FavoriteList = {
  id: number;
  userId: string;
  name: string;
  description?: string;
  items: Item[];
}

export type CreateListRequest = {
  userId: UUID;
  name: string;
  description?: string;
}

export type UpdateListRequest = {
  name?: string;
  description?: string;
}

export type ListResponse = {
  data: FavoriteList;
}

export type ListsListResponse = {
  data: FavoriteList[];
  count: number;
}
