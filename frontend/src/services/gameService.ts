import { api } from "./api";
import type { GameDetail, GameSearchParams, GameSummary, Lookup, PageResponse } from "../types/catalog";

export const gameService = {
  listGames: (params: GameSearchParams) =>
    api.get<PageResponse<GameSummary>>("/games", { params }).then((r) => r.data),

  getGame: (slug: string) => api.get<GameDetail>(`/games/${slug}`).then((r) => r.data),

  listGenres: () => api.get<Lookup[]>("/genres").then((r) => r.data),

  listPlatforms: () => api.get<Lookup[]>("/platforms").then((r) => r.data),
};
