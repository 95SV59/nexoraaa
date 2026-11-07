import { api } from "./api";
import type { AuthResponse, LoginRequest, RegisterRequest } from "../types/auth";

export const authService = {
  register: (req: RegisterRequest) => api.post<AuthResponse>("/auth/register", req).then((r) => r.data),
  login: (req: LoginRequest) => api.post<AuthResponse>("/auth/login", req).then((r) => r.data),
};
