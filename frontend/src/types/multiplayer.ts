// Mirrors the backend DTOs under com.nexora.multiplayer.{room,websocket,engine.arena}.
// Keep these in sync by hand for now; consider generating them from the OpenAPI spec
// once Phase 3 backend foundations settle.

export interface PlayerSummary {
  userId: number | null;
  displayName: string;
  ready: boolean;
  bot: boolean;
  spectator?: boolean;
  pingMs: number | null;
}

export interface RoomResponse {
  id: number;
  roomCode: string;
  gameId: string;
  hostUserId: number;
  maxPlayers: number;
  status: "LOBBY" | "IN_PROGRESS" | "FINISHED" | "CLOSED";
  reconnectToken?: string;
  players: PlayerSummary[];
}

export interface CreateRoomRequest {
  gameId: string;
  maxPlayers: number;
  region?: string;
  /** Phase 20: which controller mode the match runs in. Omit for ordinary play (PROPOSED). */
  controllerMode?: "BASELINE" | "PROPOSED";
}

export interface JoinRoomRequest {
  roomCode: string;
}

// ---- WebSocket: outbound (client -> server) ----
// No identity fields on any of these (Phase 5): the server derives who's calling from the
// authenticated STOMP session (see RoomSocket's connectHeaders), never from the message body.

export interface ReadyRequest {
  ready: boolean;
}

export interface InputRequest {
  sequence: number;
  clientTimestamp: number;
  moveX: number; // -1..1
  moveY: number; // -1..1
  aimAngleRad: number;
  firing: boolean;
  actionPressed: boolean;
}

// ---- WebSocket: inbound (server -> client) ----

export interface LobbyUpdateMessage {
  roomCode: string;
  status: RoomResponse["status"];
  players: PlayerSummary[];
}

export interface MatchLifecycleMessage {
  roomCode: string;
  event: "GAME_START" | "GAME_END" | "REMATCH_RESET";
  matchResult: ArenaMatchResult | null;
  /** The GameSession this match was persisted as — what Phase 20 comparisons are keyed on. */
  sessionId: number | null;
}

export interface ArenaMatchResult {
  winnerPlayerId: string | null;
  durationSec: number;
  summary: Record<string, { displayName: string; score: number }>;
}

// ---- WebSocket: telemetry ping/pong (Phase 10) ----

export interface PingBroadcast {
  pingId: number;
  serverSendTime: number;
}

export interface PongRequest {
  pingId: number;
  serverSendTime: number; // echoed back unchanged
}

// ---- WebSocket: network controller snapshot (Phase 10-15) ----

export interface NetworkControllerSnapshot {
  roomCode: string;
  timestamp: number;
  prediction: {
    rttMs: number;
    jitterMs: number;
    lossPct: number;
    throughput: number;
    confidence: number;
    horizonMs: number;
    sampleCount: number;
  };
  risks: Array<{
    elementName: string;
    priority: string;
    risk: "HIGH" | "LOW";
    reason: string;
    predictedStalenessMs: number;
    predictedDivergenceEstimate: number;
  }>;
  candidates: Array<{
    actionType: string;
    targetElementNames: string[];
    consistencyCost: number;
    networkCost: number;
    processingCost: number;
    controlOverhead: number;
    totalScore: number;
  }>;
  decision: {
    actionType: string;
    targetElementNames: string[];
    confidenceGatePassed: boolean;
    reason: string;
    appliedBroadcastEveryNTicks: number;
  };
}

export interface ArenaPlayerSnapshot {
  playerId: string;
  displayName: string;
  x: number;
  y: number;
  aimAngleRad: number;
  health: number;
  alive: boolean;
  score: number;
}

export interface ArenaProjectileSnapshot {
  id: number;
  x: number;
  y: number;
}

export interface ArenaStateSnapshot {
  roomId: string;
  serverTimestamp: number;
  players: ArenaPlayerSnapshot[];
  projectiles: ArenaProjectileSnapshot[];
}

export const ARENA_WIDTH = 1600;
export const ARENA_HEIGHT = 900;
export const PLAYER_RADIUS = 18;
export const PROJECTILE_RADIUS = 5;

export const PLAYABLE_SLUGS = ["arena", "racers", "football", "tag"] as const;
export type PlayableSlug = typeof PLAYABLE_SLUGS[number];

// ---- Racers Snapshot ----
export interface RacersVehicleSnapshot {
  playerId: string;
  displayName: string;
  x: number;
  y: number;
  vx: number;
  vy: number;
  headingAngleRad: number;
  speed: number;
  lap: number;
  checkpoint: number;
  finished: boolean;
  place: number;
}

export interface RacersStateSnapshot {
  gameId: "racers";
  roomId: string;
  serverTimestamp: number;
  vehicles: RacersVehicleSnapshot[];
  matchOver: boolean;
  winnerPlayerId: string | null;
  elapsedMs: number;
}

// ---- Football Snapshot ----
export interface FootballBallSnapshot {
  x: number;
  y: number;
  vx: number;
  vy: number;
  carrierPlayerId: string | null;
}

export interface FootballPlayerSnapshot {
  playerId: string;
  displayName: string;
  x: number;
  y: number;
  vx: number;
  vy: number;
  aimAngleRad: number;
  team: "RED" | "BLUE";
  hasBall: boolean;
}

export interface FootballStateSnapshot {
  gameId: "football";
  roomId: string;
  serverTimestamp: number;
  ball: FootballBallSnapshot;
  players: FootballPlayerSnapshot[];
  redScore: number;
  blueScore: number;
  timeRemainingSec: number;
  matchOver: boolean;
  winnerTeam: string | null;
  lastEvent: string;
}

// ---- Tag Snapshot ----
export interface TagPlayerSnapshot {
  playerId: string;
  displayName: string;
  x: number;
  y: number;
  vx: number;
  vy: number;
  isIt: boolean;
  timeAsItSec: number;
  immune: boolean;
}

export interface TagStateSnapshot {
  gameId: "tag";
  roomId: string;
  serverTimestamp: number;
  players: TagPlayerSnapshot[];
  currentItPlayerId: string | null;
  timeRemainingSec: number;
  matchOver: boolean;
  winnerPlayerId: string | null;
  lastEvent: string;
}

