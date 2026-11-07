import { api } from "./api";
import { LibraryItemDto, LibraryStatus } from "../types/library";

export const libraryService = {
  async getMyLibrary(status?: LibraryStatus, favoritesOnly?: boolean): Promise<LibraryItemDto[]> {
    const params: Record<string, any> = {};
    if (status) params.status = status;
    if (favoritesOnly) params.favoritesOnly = true;
    const res = await api.get<LibraryItemDto[]>("/library", { params });
    return res.data;
  },

  async setStatus(slug: string, status: LibraryStatus): Promise<LibraryItemDto> {
    const res = await api.post<LibraryItemDto>(`/library/${slug}`, { status });
    return res.data;
  },

  async toggleFavorite(slug: string): Promise<LibraryItemDto> {
    const res = await api.post<LibraryItemDto>(`/library/${slug}/favorite`);
    return res.data;
  },

  async updateNotes(slug: string, notes: string): Promise<LibraryItemDto> {
    const res = await api.post<LibraryItemDto>(`/library/${slug}/notes`, { notes });
    return res.data;
  },

  async removeFromLibrary(slug: string): Promise<void> {
    await api.delete(`/library/${slug}`);
  },

  async getGameStatus(slug: string): Promise<{ status: LibraryStatus | ""; inLibrary: boolean }> {
    const res = await api.get<{ status: LibraryStatus | ""; inLibrary: boolean }>(`/library/${slug}/status`);
    return res.data;
  },

  async getStats(): Promise<Record<string, number>> {
    const res = await api.get<Record<string, number>>("/library/stats");
    return res.data;
  },
};
