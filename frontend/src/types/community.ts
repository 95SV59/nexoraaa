export type CommunityPostType = "DISCUSSION" | "GUIDE" | "ANNOUNCEMENT";

export interface CommunityPostDto {
  id: number;
  gameSlug: string;
  authorId: number;
  authorName: string;
  authorAvatarUrl: string | null;
  title: string;
  content: string;
  postType: CommunityPostType;
  likesCount: number;
  createdAt: string;
}

export interface CreateCommunityPostRequest {
  title: string;
  content: string;
  postType: CommunityPostType;
}
