import { useEffect, useRef, useState } from "react";
import { RoomSocket } from "../websocket/roomSocket";
import type {
  InputRequest,
  LobbyUpdateMessage,
  MatchLifecycleMessage,
  NetworkControllerSnapshot,
  ReadyRequest,
} from "../types/multiplayer";

/** Connects to one room's WebSocket channel for the lifetime of the component that
 *  calls this hook, and re-renders on lobby/state/lifecycle/network updates. jwt
 *  authenticates the STOMP session (see RoomSocket) — identity for gameplay purposes
 *  comes from the server's view of that session, not from anything passed here. */
export function useArenaSocket(roomCode: string, jwt: string, playerId?: string | number) {
  const socketRef = useRef<RoomSocket | null>(null);
  const [connected, setConnected] = useState(false);
  const [lobby, setLobby] = useState<LobbyUpdateMessage | null>(null);
  const [state, setState] = useState<any>(null);
  const [lifecycle, setLifecycle] = useState<MatchLifecycleMessage | null>(null);
  const [networkSnapshot, setNetworkSnapshot] = useState<NetworkControllerSnapshot | null>(null);

  useEffect(() => {
    if (!roomCode || roomCode === "__none__" || !jwt) {
      setConnected(false);
      setLobby(null);
      setState(null);
      setLifecycle(null);
      setNetworkSnapshot(null);
      return;
    }

    const socket = new RoomSocket(roomCode, jwt, {
      onConnect: () => setConnected(true),
      onLobby: setLobby,
      onState: setState,
      onLifecycle: setLifecycle,
      onNetworkSnapshot: setNetworkSnapshot,
      onError: (frame) => console.error("STOMP error", frame),
    }, playerId);
    socket.connect();
    socketRef.current = socket;

    return () => {
      socket.disconnect();
      socketRef.current = null;
    };
  }, [roomCode, jwt, playerId]);

  const sendReady = (req: ReadyRequest) => socketRef.current?.sendReady(req);
  const sendInput = (req: InputRequest) => socketRef.current?.sendInput(req);

  return { connected, lobby, state, lifecycle, networkSnapshot, sendReady, sendInput };
}
