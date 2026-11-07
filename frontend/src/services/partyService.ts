import { api } from "./api";
import type { PartyDto } from "../types/party";

export const partyService = {
  getCurrentParty: () =>
    api.get<PartyDto>("/parties/current").then((r) => (r.status === 204 ? null : r.data)),

  createParty: () =>
    api.post<PartyDto>("/parties").then((r) => r.data),

  inviteToParty: (friendUserId: number) =>
    api.post<void>("/parties/invite", { friendUserId }).then((r) => r.data),

  respondInvitation: (invitationId: number, accept: boolean) =>
    api.put<PartyDto>(`/parties/invitations/${invitationId}/respond?accept=${accept}`).then((r) => r.data),

  leaveParty: () =>
    api.post<void>("/parties/leave").then((r) => r.data),

  launchMatch: (gameSlug: string) =>
    api.post<{ roomCode: string }>("/parties/launch", { gameSlug }).then((r) => r.data),
};
