import { api } from "./api";
import type { UserMatchHistoryDto } from "../types/matchHistory";

export const matchHistoryService = {
  getHistory: () =>
    api.get<UserMatchHistoryDto[]>("/matches/history").then((r) => r.data),
};
