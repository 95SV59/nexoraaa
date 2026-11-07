export interface AdminStatsDto {
  totalUsers: number;
  activeUsers: number;
  totalGames: number;
  reportedReviewsCount: number;
  totalArticles: number;
  totalSessions: number;
  activeRooms: number;
}

export interface UserManagementDto {
  id: number;
  username: string;
  email: string;
  role: string;
  status: string;
  createdAt: string;
}

export interface CreateGameRequest {
  slug: string;
  title: string;
  description?: string;
  developer?: string;
  publisher?: string;
  multiplayer?: boolean;
  minPlayers?: number;
  maxPlayers?: number;
  genreName?: string;
  platformName?: string;
  screenshotUrl?: string;
  releaseDate?: string;
}
