// ============================================================
// NEXORA — Progression & Leaderboard TypeScript Types
// ============================================================

export interface UserProgressionDto {
  userId: number;
  username: string;
  totalXp: number;
  level: number;
  rankTitle: string;
  seasonPassTier: number;
  matchesPlayed: number;
  matchesWon: number;
  winRate: number;
  currentLevelXpFloor: number;
  nextLevelXpThreshold: number;
  levelProgressPct: number;
  achievementsCompleted: number;
  achievementsTotal: number;
}

export interface AchievementDto {
  id: number;
  gameSlug: string | null;
  slug: string;
  title: string;
  description: string;
  badgeIcon: string;
  iconUrl: string | null;
  xpReward: number;
  criteriaType: string;
  criteriaThreshold: number;
  rarityTier: string;
  completed: boolean;
  progress: number;
  unlockedAt: string | null; // ISO timestamp
}

export interface LeaderboardEntryDto {
  rank: number;
  userId: number;
  username: string;
  avatarUrl: string | null;
  gameSlug: string;
  score: number;
  rankTier: string;
  matchesPlayed: number;
  wins: number;
  winRate: number;
  season: number;
}

export const RARITY_COLORS: Record<string, string> = {
  COMMON: '#64748b',
  RARE: '#3b82f6',
  EPIC: '#8b5cf6',
  LEGENDARY: '#f59e0b',
};

export const RANK_TIER_COLORS: Record<string, string> = {
  BRONZE: '#cd7f32',
  SILVER: '#c0c0c0',
  GOLD: '#ffd700',
  PLATINUM: '#e5e4e2',
  DIAMOND: '#b9f2ff',
  MASTER: '#f59e0b',
};

export const RANK_TIER_ORDER = ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND', 'MASTER'];
