export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  username: string;
  role?: string;
}

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  role: string;
  avatarUrl: string;
  status: string;
  createdAt: string;
  gamesInLibrary: number;
  wishlistCount: number;
  reviewsCount: number;
  matchesPlayed: number;
  wins: number;
  winRatePct: number;
}
