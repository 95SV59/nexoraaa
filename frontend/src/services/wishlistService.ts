import { api } from "./api";
import { WishlistItemDto } from "../types/wishlist";

export const wishlistService = {
  async getMyWishlist(): Promise<WishlistItemDto[]> {
    const res = await api.get<WishlistItemDto[]>("/wishlist");
    return res.data;
  },

  async addToWishlist(slug: string): Promise<WishlistItemDto> {
    const res = await api.post<WishlistItemDto>(`/wishlist/${slug}`);
    return res.data;
  },

  async removeFromWishlist(slug: string): Promise<void> {
    await api.delete(`/wishlist/${slug}`);
  },

  async isInWishlist(slug: string): Promise<boolean> {
    const res = await api.get<{ inWishlist: boolean }>(`/wishlist/${slug}/check`);
    return res.data.inWishlist;
  },
};
