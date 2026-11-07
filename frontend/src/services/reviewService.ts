import { api } from "./api";
import { CreateReviewRequest, ReviewDto } from "../types/review";

export const reviewService = {
  async getGameReviews(slug: string): Promise<ReviewDto[]> {
    const res = await api.get<ReviewDto[]>(`/games/${slug}/reviews`);
    return res.data;
  },

  async createReview(slug: string, req: CreateReviewRequest): Promise<ReviewDto> {
    const res = await api.post<ReviewDto>(`/games/${slug}/reviews`, req);
    return res.data;
  },

  async toggleLike(reviewId: number): Promise<{ liked: boolean; reviewId: number }> {
    const res = await api.post<{ liked: boolean; reviewId: number }>(`/reviews/${reviewId}/like`);
    return res.data;
  },

  async reportReview(reviewId: number): Promise<{ reported: boolean; reviewId: number }> {
    const res = await api.post<{ reported: boolean; reviewId: number }>(`/reviews/${reviewId}/report`);
    return res.data;
  },

  async deleteReview(reviewId: number): Promise<void> {
    await api.delete(`/reviews/${reviewId}`);
  },

  async getMyReviews(): Promise<ReviewDto[]> {
    const res = await api.get<ReviewDto[]>("/reviews/my");
    return res.data;
  },
};
