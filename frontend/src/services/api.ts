import axios from "axios";
import { tokenStore } from "./tokenStore";

const getBaseUrl = () => {
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
  }
  if (typeof window !== "undefined" && (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1")) {
    return "/api";
  }
  return "http://localhost:8080/api";
};

export const api = axios.create({
  baseURL: getBaseUrl(),
});

// Attaches the JWT to every request automatically. /api/auth/** doesn't need one (see
// SecurityConfig), but sending a header it doesn't check is harmless, so there's no need
// to special-case those two endpoints here.
api.interceptors.request.use((config) => {
  const auth = tokenStore.get();
  if (auth) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

// A 401 almost always means the stored token expired or was revoked — clear it so the app
// falls back to the login screen instead of silently retrying with a dead token forever.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      tokenStore.clear();
    }
    return Promise.reject(error);
  },
);
