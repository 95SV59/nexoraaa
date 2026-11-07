import { api } from "./api";
import { AdminStatsDto, CreateGameRequest, UserManagementDto } from "../types/admin";
import { GameDetail } from "../types/catalog";
import { ReviewDto } from "../types/review";

export const adminService = {
  async getStats(): Promise<AdminStatsDto> {
    const res = await api.get<AdminStatsDto>("/admin/stats");
    return res.data;
  },

  async listUsers(): Promise<UserManagementDto[]> {
    const res = await api.get<UserManagementDto[]>("/admin/users");
    return res.data;
  },

  async toggleUserStatus(userId: number): Promise<UserManagementDto> {
    const res = await api.put<UserManagementDto>(`/admin/users/${userId}/status`);
    return res.data;
  },

  async changeUserRole(userId: number, role: string): Promise<UserManagementDto> {
    const res = await api.put<UserManagementDto>(`/admin/users/${userId}/role`, null, {
      params: { role },
    });
    return res.data;
  },

  async createGame(req: CreateGameRequest): Promise<GameDetail> {
    const res = await api.post<GameDetail>("/admin/games", req);
    return res.data;
  },

  async updateGame(id: number, req: CreateGameRequest): Promise<GameDetail> {
    const res = await api.put<GameDetail>(`/admin/games/${id}`, req);
    return res.data;
  },

  async deleteGame(id: number): Promise<void> {
    await api.delete(`/admin/games/${id}`);
  },

  async getReportedReviews(): Promise<ReviewDto[]> {
    const res = await api.get<ReviewDto[]>("/admin/reviews/reported");
    return res.data;
  },
};
