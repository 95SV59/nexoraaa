import { api } from "./api";
import type { MatchmakingStatusResponse } from "../types/matchmaking";

export const matchmakingService = {
  async joinQueue(gameSlug: string): Promise<MatchmakingStatusResponse> {
    const res = await api.post<MatchmakingStatusResponse>("/matchmaking/join", { gameSlug });
    return res.data;
  },

  async leaveQueue(): Promise<MatchmakingStatusResponse> {
    const res = await api.post<MatchmakingStatusResponse>("/matchmaking/leave");
    return res.data;
  },

  async getStatus(): Promise<MatchmakingStatusResponse> {
    const res = await api.get<MatchmakingStatusResponse>("/matchmaking/status");
    return res.data;
  },
};
