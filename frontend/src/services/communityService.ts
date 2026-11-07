import { api } from "./api";
import type { CommunityPostDto, CommunityPostType, CreateCommunityPostRequest } from "../types/community";

export const communityService = {
  async getPosts(gameSlug: string, type?: CommunityPostType): Promise<CommunityPostDto[]> {
    const params: Record<string, any> = {};
    if (type) params.type = type;
    const res = await api.get<CommunityPostDto[]>(`/community/${gameSlug}`, { params });
    return res.data;
  },

  async createPost(gameSlug: string, req: CreateCommunityPostRequest): Promise<CommunityPostDto> {
    const res = await api.post<CommunityPostDto>(`/community/${gameSlug}`, req);
    return res.data;
  },

  async likePost(postId: number): Promise<CommunityPostDto> {
    const res = await api.post<CommunityPostDto>(`/community/${postId}/like`);
    return res.data;
  },
};
