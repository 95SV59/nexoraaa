// ============================================================
// NEXORA — Progression & Leaderboard Service
// ============================================================

import { api } from './api';
import type { UserProgressionDto, AchievementDto, LeaderboardEntryDto } from '../types/progression';

// --- Progression ---
export async function getMyProgression(): Promise<UserProgressionDto> {
  const res = await api.get<UserProgressionDto>('/progression/me');
  return res.data;
}

// --- Achievements ---
export async function getMyAchievements(game?: string): Promise<AchievementDto[]> {
  const params: Record<string, string> = {};
  if (game) params.game = game;
  const res = await api.get<AchievementDto[]>('/achievements/me', { params });
  return res.data;
}

export async function getAchievements(game?: string): Promise<AchievementDto[]> {
  const params: Record<string, string> = {};
  if (game) params.game = game;
  const res = await api.get<AchievementDto[]>('/achievements', { params });
  return res.data;
}

// --- Leaderboards ---
export async function getLeaderboard(
  game: string,
  season: number = 1,
  limit: number = 50
): Promise<LeaderboardEntryDto[]> {
  const res = await api.get<LeaderboardEntryDto[]>('/leaderboards', {
    params: { game, season, limit },
  });
  return res.data;
}

export async function getFriendsLeaderboard(
  game: string,
  season: number = 1
): Promise<LeaderboardEntryDto[]> {
  const res = await api.get<LeaderboardEntryDto[]>('/leaderboards/friends', {
    params: { game, season },
  });
  return res.data;
}
