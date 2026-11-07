import { createContext, useContext, useEffect, useState } from "react";
import { authService } from "../services/authService";
import { tokenStore } from "../services/tokenStore";
import type { LoginRequest, RegisterRequest } from "../types/auth";

export interface AuthUser {
  userId: number;
  username: string;
  role?: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  loading: boolean;
  login: (req: LoginRequest) => Promise<void>;
  register: (req: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = tokenStore.get();
    if (stored) {
      setUser({ userId: stored.userId, username: stored.username, role: stored.role });
      setToken(stored.token);
    }
    setLoading(false);
  }, []);

  async function login(req: LoginRequest) {
    const res = await authService.login(req);
    tokenStore.set(res);
    setUser({ userId: res.userId, username: res.username, role: res.role });
    setToken(res.token);
  }

  async function register(req: RegisterRequest) {
    const res = await authService.register(req);
    tokenStore.set(res);
    setUser({ userId: res.userId, username: res.username, role: res.role });
    setToken(res.token);
  }

  function logout() {
    tokenStore.clear();
    setUser(null);
    setToken(null);
  }

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth() must be used inside <AuthProvider>");
  return ctx;
}
