export interface UserMatchHistoryDto {
  sessionId: number;
  roomCode: string;
  gameSlug: string;
  startedAt: string;
  endedAt: string | null;
  durationSec: number | null;
  won: boolean;
  winnerUserId: number | null;
  controllerMode: string;
  resultSummaryJson: string | null;
}
