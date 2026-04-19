import type { UUID } from "crypto";

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
  overview: string;
  releaseDate: string;
  popularity: number;
  voteAverage: number;
  type: "MOVIE" | "TV";
  tmdbId: number;
  genres: Genre[];
  actors: ActorItem[];
  directors: Director[];
  posterUrl?: string;
  backdropUrl?: string;
}

export type RecommendationResponse = {
  userId: UUID;
  items: Item[];
  count: number;
};
