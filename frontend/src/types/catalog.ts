export interface Lookup {
  id: number;
  name: string;
}

export interface GameSummary {
  id: number;
  slug: string;
  title: string;
  genre: string | null;
  multiplayer: boolean;
  minPlayers: number | null;
  maxPlayers: number | null;
  releaseDate: string | null;
  thumbnailUrl: string | null;
}

export interface GameMedia {
  type: "SCREENSHOT" | "TRAILER";
  url: string;
}

export interface GameDetail {
  id: number;
  slug: string;
  title: string;
  description: string | null;
  developer: string | null;
  publisher: string | null;
  genre: string | null;
  platforms: string[];
  multiplayer: boolean;
  minPlayers: number | null;
  maxPlayers: number | null;
  releaseDate: string | null;
  media: GameMedia[];
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface GameSearchParams {
  search?: string;
  genreId?: number;
  platformId?: number;
  multiplayerOnly?: boolean;
  sortBy?: "title" | "releaseDate" | "createdAt";
  direction?: "asc" | "desc";
  page?: number;
  size?: number;
}
