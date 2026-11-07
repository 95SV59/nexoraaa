import { api } from "./api";
import type { CreateRoomRequest, JoinRoomRequest, RoomResponse } from "../types/multiplayer";

export const roomService = {
  createRoom: (req: CreateRoomRequest) =>
    api.post<RoomResponse>("/rooms", req).then((r) => r.data),

  joinRoom: (req: JoinRoomRequest) =>
    api.post<RoomResponse>("/rooms/join", req).then((r) => r.data),

  spectateRoom: (roomCode: string) =>
    api.post<RoomResponse>(`/rooms/${roomCode}/spectate`).then((r) => r.data),

  rematch: (roomCode: string) =>
    api.post<RoomResponse>(`/rooms/${roomCode}/rematch`).then((r) => r.data),

  getRoom: (roomCode: string) =>
    api.get<RoomResponse>(`/rooms/${roomCode}`).then((r) => r.data),

  leaveRoom: (roomCode: string) =>
    api.post<void>(`/rooms/${roomCode}/leave`),

  /** Host-only: fill remaining slots with AI bots. */
  fillBots: (roomCode: string) =>
    api.post<{ botsAdded: number; roomCode: string }>(`/rooms/${roomCode}/fill-bots`).then((r) => r.data),

  /** Host-only: fill bots + start match immediately, bypassing "wait for full room". */
  startNow: (roomCode: string) =>
    api.post<{ roomCode: string; botsAdded: number; started: boolean }>(`/rooms/${roomCode}/start-now`).then((r) => r.data),

  /** Reconnect to an in-progress or interrupted match with grace period. */
  reconnectRoom: (roomCode: string, reconnectToken: string) =>
    api.post<RoomResponse>(`/rooms/${roomCode}/reconnect`, { reconnectToken }).then((r) => {
      if (r.data.reconnectToken) {
        sessionStorage.setItem(`nexora_reconnect_${roomCode}`, r.data.reconnectToken);
      }
      return r.data;
    }),

  saveReconnectToken: (roomCode: string, token: string) => {
    sessionStorage.setItem(`nexora_reconnect_${roomCode}`, token);
  },

  getReconnectToken: (roomCode: string): string | null => {
    return sessionStorage.getItem(`nexora_reconnect_${roomCode}`);
  },
};
