import { api } from "./api";

export interface LabProfileResponse {
  roomCode: string;
  profileName: string;
  latencyMs: number;
  jitterMs: number;
  packetLossPct: number;
  bandwidthKbps: number;
  active: boolean;
}

export interface SetLabProfileRequest {
  profileName?: string; // preset name, or omit for a custom profile
  latencyMs?: number;
  jitterMs?: number;
  packetLossPct?: number;
  bandwidthKbps?: number;
}

/** Phase 16 — Network Laboratory REST client. */
export const labService = {
  getProfile: (roomCode: string) =>
    api.get<LabProfileResponse>(`/network-lab/${roomCode}`).then((r) => r.data),

  setProfile: (roomCode: string, req: SetLabProfileRequest) =>
    api.post<LabProfileResponse>(`/network-lab/${roomCode}`, req).then((r) => r.data),

  clearProfile: (roomCode: string) =>
    api.delete<LabProfileResponse>(`/network-lab/${roomCode}`).then((r) => r.data),
};
