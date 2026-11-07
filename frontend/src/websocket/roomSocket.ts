import { Client, type IFrame } from "@stomp/stompjs";
import type {
  ArenaStateSnapshot,
  InputRequest,
  LobbyUpdateMessage,
  MatchLifecycleMessage,
  NetworkControllerSnapshot,
  PingBroadcast,
  ReadyRequest,
} from "../types/multiplayer";

const getWsBaseUrl = () => {
  if (import.meta.env.VITE_WS_URL) {
    return import.meta.env.VITE_WS_URL;
  }
  if (typeof window !== "undefined") {
    const proto = window.location.protocol === "https:" ? "wss:" : "ws:";
    return `${proto}//${window.location.hostname}:8080/ws`;
  }
  return "ws://localhost:8080/ws";
};

const WS_BASE_URL = getWsBaseUrl();

export interface RoomSocketHandlers {
  onLobby?: (msg: LobbyUpdateMessage) => void;
  onState?: (msg: ArenaStateSnapshot) => void;
  onLifecycle?: (msg: MatchLifecycleMessage) => void;
  onNetworkSnapshot?: (msg: NetworkControllerSnapshot) => void;
  onConnect?: () => void;
  onError?: (frame: IFrame) => void;
}

export class RoomSocket {
  private readonly client: Client;

  constructor(
    private readonly roomCode: string,
    jwt: string,
    handlers: RoomSocketHandlers,
    playerId?: string | number,
  ) {
    this.client = new Client({
      brokerURL: WS_BASE_URL,

      connectHeaders: {
        Authorization: `Bearer ${jwt}`,
      },

      reconnectDelay: 3000,

      onConnect: () => {
        this.client.subscribe(
          `/topic/room/${roomCode}/lobby`,
          (frame) => handlers.onLobby?.(JSON.parse(frame.body)),
        );

        this.client.subscribe(
          `/topic/room/${roomCode}/state`,
          (frame) => handlers.onState?.(JSON.parse(frame.body)),
        );

        if (playerId) {
          this.client.subscribe(
            `/topic/room/${roomCode}/player/${playerId}/state`,
            (frame) => handlers.onState?.(JSON.parse(frame.body)),
          );
        }

        this.client.subscribe(
          `/topic/room/${roomCode}/lifecycle`,
          (frame) => handlers.onLifecycle?.(JSON.parse(frame.body)),
        );

        this.client.subscribe(
          `/topic/room/${roomCode}/network`,
          (frame) => handlers.onNetworkSnapshot?.(JSON.parse(frame.body)),
        );

        this.client.subscribe(
          `/topic/room/${roomCode}/ping`,
          (frame) => {
            const ping: PingBroadcast = JSON.parse(frame.body);

            this.client.publish({
              destination: `/app/room/${roomCode}/pong`,
              body: JSON.stringify({
                pingId: ping.pingId,
                serverSendTime: ping.serverSendTime,
              }),
            });
          },
        );

        handlers.onConnect?.();
      },

      onStompError: (frame) => {
        handlers.onError?.(frame);
      },

      onWebSocketError: (event) => {
        console.error("WebSocket error:", event);
      },

      onWebSocketClose: (event) => {
        console.warn("WebSocket closed:", event);
      },
    });
  }

  connect() {
    this.client.activate();
  }

  disconnect() {
    void this.client.deactivate();
  }

  sendReady(req: ReadyRequest) {
    this.client.publish({
      destination: `/app/room/${this.roomCode}/ready`,
      body: JSON.stringify(req),
    });
  }

  sendInput(req: InputRequest) {
    this.client.publish({
      destination: `/app/room/${this.roomCode}/input`,
      body: JSON.stringify(req),
    });
  }
}