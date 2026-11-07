export interface MatchmakingRequest {
  gameSlug: string;
}

export interface MatchmakingStatusResponse {
  status: "IDLE" | "QUEUED" | "MATCHED";
  gameSlug: string | null;
  roomCode: string | null;
  elapsedSeconds: number;
  queueSize: number;
}

export interface MatchFoundMessage {
  event: "MATCH_FOUND";
  roomCode: string;
  gameSlug: string;
}
