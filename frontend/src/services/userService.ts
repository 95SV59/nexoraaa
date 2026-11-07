import { api } from "./api";
import { UserProfile } from "../types/auth";

export const userService = {
  async getProfile(): Promise<UserProfile> {
    const res = await api.get<UserProfile>("/users/profile");
    return res.data;
  },

  async getProfileByUsername(username: string): Promise<UserProfile> {
    const res = await api.get<UserProfile>(`/users/${username}`);
    return res.data;
  },

  async updateProfile(req: { email?: string; avatarUrl?: string }): Promise<UserProfile> {
    const res = await api.put<UserProfile>("/users/profile", req);
    return res.data;
  },
};
