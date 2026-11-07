import { api } from "./api";
import { RecommendationDto } from "../types/recommendation";

export const recommendationService = {
  async getRecommendations(): Promise<RecommendationDto[]> {
    const res = await api.get<RecommendationDto[]>("/recommendations");
    return res.data;
  },
};
