export interface ReviewDto {
  id: number;
  gameId: number;
  gameSlug: string;
  gameTitle: string;
  userId: number;
  username: string;
  userAvatarUrl: string;
  rating: number;
  recommended: boolean;
  hoursPlayed: number;
  spoiler: boolean;
  body: string;
  reported: boolean;
  likeCount: number;
  likedByCurrentUser: boolean;
  createdAt: string;
}

export interface CreateReviewRequest {
  rating: number;
  body: string;
  recommended?: boolean;
  spoiler?: boolean;
}
