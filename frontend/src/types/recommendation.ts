import { GameSummary } from "./catalog";

export interface RecommendationDto {
  game: GameSummary;
  ruleName: string;
  reason: string;
}
