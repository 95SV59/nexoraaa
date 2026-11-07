export type LibraryStatus =
  | "PLAYING"
  | "COMPLETED"
  | "PLAN_TO_PLAY"
  | "DROPPED"
  | "ON_HOLD";

export type LibraryShelf = "ALL" | "FAVORITES" | LibraryStatus;

export interface LibraryItemDto {
  id: number;
  gameId: number;
  gameSlug: string;
  title: string;
  description: string;
  genreName: string;
  thumbnailUrl: string;
  multiplayer: boolean;
  status: LibraryStatus;
  hoursPlayed: number;
  lastPlayed: string | null;
  isFavorite: boolean;
  userNotes: string | null;
  updatedAt: string;
}

export interface UpdateLibraryRequest {
  status: LibraryStatus;
}
