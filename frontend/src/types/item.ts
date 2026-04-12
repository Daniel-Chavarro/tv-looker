export type Genre = {
  id: number;
  tmdbId: number;
  name: string;
}

export type Actor = {
  id: number;
  tmdbId: number;
  name: string;
}

export type ActorItem = {
    id: number;
    actorId: number;
    actorName: string;
    characterName: string;
    billingOrder: number;
}

export type Director = {
  id: number;
  tmdbId: number;
  name: string;
}

export type Item = {
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

export type ItemResponse = {
  data: Item;
}

export type ItemsListResponse = {
  data: Item[];
  count: number;
}
