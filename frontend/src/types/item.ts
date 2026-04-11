export interface Genre {
  id: number;
  tmdbId: number;
  name: string;
}

export interface Actor {
  id: number;
  tmdbId: number;
  name: string;
}

export interface ActorItem{
    id: number;
    actorId: number;
    actorName: string;
    characterName: string;
    billingOrder: number;
}

export interface Director {
  id: number;
  tmdbId: number;
  name: string;
}

export interface Item {
  id: number;
  title: string;
  type: "MOVIE" | "TV";
  releaseDate: string;
  overview: string;
  posterUrl?: string;
  backdropUrl?: string;
  voteAverage: number;
  popularity: number;
  tmdbId: number;
  genres: Genre[];
  actors: ActorItem[];
  directors: Director[];
}

export interface ItemResponse {
  data: Item;
}

export interface ItemsListResponse {
  data: Item[];
  count: number;
}
