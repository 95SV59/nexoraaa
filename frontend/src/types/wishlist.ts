export interface WishlistItemDto {
  id: number;
  gameId: number;
  gameSlug: string;
  title: string;
  description: string;
  genreName: string;
  thumbnailUrl: string;
  multiplayer: boolean;
  addedAt: string;
}
