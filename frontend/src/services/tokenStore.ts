const STORAGE_KEY = "nexora.auth";

export interface StoredAuth {
  token: string;
  userId: number;
  username: string;
  role?: string;
}

export const tokenStore = {
  get(): StoredAuth | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StoredAuth;
    } catch {
      return null;
    }
  },
  set(auth: StoredAuth) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  },
  clear() {
    localStorage.removeItem(STORAGE_KEY);
  },
};
