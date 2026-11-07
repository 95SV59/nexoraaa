import { useState } from "react";
import type { LobbyUpdateMessage, RoomResponse } from "../../types/multiplayer";
import { roomService } from "../../services/roomService";

interface Props {
  room: RoomResponse;
  lobby: LobbyUpdateMessage | null;
  currentUserId: number;
  onToggleReady: (ready: boolean) => void;
}

/**
 * Shown while a room is in LOBBY status, before the match starts. Player list comes
 * from live WebSocket lobby updates once connected, falling back to the REST snapshot
 * from the create/join call for the very first render.
 *
 * Bot support: bots appear in the player list with a [BOT] badge and robot icon.
 * The host sees "Fill with Bots" and "Start Now" buttons to start without waiting
 * for a full room of human players.
 */
export function RoomLobby({ room, lobby, currentUserId, onToggleReady }: Props) {
  const players = lobby?.players ?? room.players;
  const me = players.find((p) => p.userId === currentUserId);
  const isHost = room.hostUserId === currentUserId;

  const [botLoading, setBotLoading] = useState(false);
  const [startLoading, setStartLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const humanPlayers = players.filter((p) => !p.bot);
  const botPlayers = players.filter((p) => p.bot);

  async function handleFillBots() {
    setBotLoading(true);
    setActionError(null);
    try {
      await roomService.fillBots(room.roomCode);
    } catch {
      setActionError("Could not add bots. Try again.");
    } finally {
      setBotLoading(false);
    }
  }

  async function handleStartNow() {
    setStartLoading(true);
    setActionError(null);
    try {
      await roomService.startNow(room.roomCode);
    } catch {
      setActionError("Could not start match. Try again.");
    } finally {
      setStartLoading(false);
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={styles.heading}>
          <span style={styles.roomBadge}>ROOM</span>
          {room.roomCode}
        </h2>
        <p style={styles.subtext}>
          Share this code with friends &nbsp;·&nbsp;
          <span style={{ color: "#00f2fe" }}>{players.length}</span>
          <span style={{ color: "#64748b" }}>/{room.maxPlayers}</span>
          {" "}players
          {botPlayers.length > 0 && (
            <span style={{ color: "#f59e0b", marginLeft: 8 }}>
              ({botPlayers.length} AI bot{botPlayers.length !== 1 ? "s" : ""})
            </span>
          )}
        </p>
      </div>

      {/* Player list */}
      <ul style={styles.list}>
        {players.map((p, i) => (
          <li key={p.userId ?? `bot-${i}`} style={{
            ...styles.listItem,
            borderColor: p.spectator ? "rgba(168,85,247,0.35)" : p.bot ? "rgba(245,158,11,0.3)" : "rgba(0,242,254,0.15)",
            background: p.spectator ? "rgba(168,85,247,0.06)" : p.bot ? "rgba(245,158,11,0.06)" : "#0f172a",
          }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <span style={{
                width: 32, height: 32, borderRadius: "50%",
                display: "flex", alignItems: "center", justifyContent: "center",
                background: p.spectator ? "rgba(168,85,247,0.2)" : p.bot ? "rgba(245,158,11,0.2)" : "rgba(0,242,254,0.1)",
                fontSize: 16,
              }}>
                {p.spectator ? "👁" : p.bot ? "🤖" : "🎮"}
              </span>
              <div>
                <div style={{ color: p.spectator ? "#c084fc" : p.bot ? "#f59e0b" : "#f8fafc", fontWeight: 600, fontSize: 14 }}>
                  {p.displayName}
                  {p.userId === currentUserId && <span style={{ color: "#64748b", fontWeight: 400, fontSize: 12, marginLeft: 6 }}>(you)</span>}
                  {room.hostUserId === p.userId && <span style={{ color: "#00f2fe", fontSize: 11, marginLeft: 6 }}>HOST</span>}
                </div>
                {p.spectator && <div style={{ fontSize: 11, color: "#a855f7" }}>Spectator — Observing live match</div>}
                {p.bot && <div style={{ fontSize: 11, color: "#78716c" }}>AI Bot — auto-plays this game</div>}
                {!p.bot && !p.spectator && p.pingMs != null && (
                  <div style={{ fontSize: 11, color: "#64748b" }}>{p.pingMs}ms</div>
                )}
              </div>
            </div>
            <span style={p.spectator ? styles.spectatorBadge : (p.ready ? styles.readyBadge : (p.bot ? styles.botReadyBadge : styles.notReadyBadge))}>
              {p.spectator ? "👁 Watching" : (p.ready ? "✓ Ready" : (p.bot ? "✓ Auto" : "Not ready"))}
            </span>
          </li>
        ))}

        {/* Empty slot placeholders */}
        {Array.from({ length: Math.max(0, room.maxPlayers - players.length) }).map((_, i) => (
          <li key={`empty-${i}`} style={{ ...styles.listItem, opacity: 0.35, borderColor: "#1e293b", background: "transparent" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <span style={{ width: 32, height: 32, borderRadius: "50%", background: "#1e293b", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16 }}>
                ⬜
              </span>
              <div style={{ color: "#475569", fontSize: 13 }}>Waiting for player...</div>
            </div>
          </li>
        ))}
      </ul>

      {/* Ready toggle (for non-bot non-spectator self) */}
      {me && !me.bot && !me.spectator && (
        <button style={{ ...styles.button, background: me.ready ? "#374151" : "linear-gradient(135deg,#00f2fe,#4facfe)" }}
          onClick={() => onToggleReady(!(me.ready))}>
          {me.ready ? "✓ Ready — Cancel" : "I'm Ready"}
        </button>
      )}

      {/* Host-only bot controls */}
      {isHost && (
        <div style={styles.botControls}>
          <div style={styles.divider}>
            <div style={{ flex: 1, height: 1, background: "#1e293b" }} />
            <span style={{ color: "#475569", fontSize: 11, padding: "0 12px" }}>HOST CONTROLS</span>
            <div style={{ flex: 1, height: 1, background: "#1e293b" }} />
          </div>

          <div style={{ display: "flex", gap: 10, marginTop: 12 }}>
            <button
              style={{
                ...styles.botButton,
                opacity: botLoading || players.length >= room.maxPlayers ? 0.5 : 1,
                flex: 1,
              }}
              onClick={handleFillBots}
              disabled={botLoading || players.length >= room.maxPlayers}
            >
              {botLoading ? "Adding..." : "🤖 Fill with Bots"}
            </button>

            <button
              style={{
                ...styles.startButton,
                opacity: startLoading || humanPlayers.length < 1 ? 0.5 : 1,
                flex: 1,
              }}
              onClick={handleStartNow}
              disabled={startLoading || humanPlayers.length < 1}
            >
              {startLoading ? "Starting..." : "▶ Start Now"}
            </button>
          </div>

          <p style={styles.hint}>
            "Fill with Bots" fills empty slots with AI players.&nbsp;
            "Start Now" fills remaining slots with bots and starts immediately.
          </p>
        </div>
      )}

      {!isHost && (
        <p style={styles.hint}>Waiting for the host to start the match...</p>
      )}

      {actionError && <p style={styles.errorText}>{actionError}</p>}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    maxWidth: 520,
    margin: "0 auto",
    padding: 24,
    fontFamily: "system-ui, -apple-system, sans-serif",
    background: "#080d1a",
    borderRadius: 16,
    border: "1px solid #1e293b",
  },
  header: { marginBottom: 20 },
  heading: {
    margin: 0,
    color: "#f8fafc",
    fontSize: 22,
    fontWeight: 800,
    display: "flex",
    alignItems: "center",
    gap: 10,
  },
  roomBadge: {
    fontSize: 10,
    fontWeight: 700,
    letterSpacing: 1.5,
    color: "#00f2fe",
    background: "rgba(0,242,254,0.1)",
    border: "1px solid rgba(0,242,254,0.2)",
    padding: "2px 8px",
    borderRadius: 4,
  },
  subtext: { color: "#94a3b8", fontSize: 13, marginTop: 8, marginBottom: 0 },
  list: { listStyle: "none", padding: 0, margin: "16px 0 16px" },
  listItem: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "10px 14px",
    borderRadius: 10,
    border: "1px solid",
    marginBottom: 8,
  },
  readyBadge: {
    color: "#00ff87",
    fontWeight: 700,
    fontSize: 12,
    background: "rgba(0,255,135,0.1)",
    padding: "2px 8px",
    borderRadius: 4,
  },
  spectatorBadge: {
    color: "#c084fc",
    fontWeight: 700,
    fontSize: 12,
    background: "rgba(168,85,247,0.15)",
    padding: "2px 8px",
    borderRadius: 4,
    border: "1px solid rgba(168,85,247,0.3)",
  },
  botReadyBadge: {
    color: "#f59e0b",
    fontWeight: 700,
    fontSize: 12,
    background: "rgba(245,158,11,0.1)",
    padding: "2px 8px",
    borderRadius: 4,
  },
  notReadyBadge: {
    color: "#64748b",
    fontSize: 12,
    background: "rgba(100,116,139,0.1)",
    padding: "2px 8px",
    borderRadius: 4,
  },
  button: {
    width: "100%",
    padding: "12px 16px",
    borderRadius: 8,
    border: "none",
    color: "#0f172a",
    fontWeight: 700,
    fontSize: 14,
    cursor: "pointer",
    marginBottom: 4,
  },
  botControls: { marginTop: 12 },
  divider: { display: "flex", alignItems: "center" },
  botButton: {
    padding: "10px 16px",
    borderRadius: 8,
    border: "1px solid rgba(245,158,11,0.4)",
    background: "rgba(245,158,11,0.1)",
    color: "#f59e0b",
    fontWeight: 600,
    fontSize: 13,
    cursor: "pointer",
  },
  startButton: {
    padding: "10px 16px",
    borderRadius: 8,
    border: "none",
    background: "linear-gradient(135deg,#00f2fe,#4facfe)",
    color: "#0f172a",
    fontWeight: 700,
    fontSize: 13,
    cursor: "pointer",
  },
  hint: { color: "#475569", fontSize: 12, marginTop: 8, textAlign: "center" },
  errorText: { color: "#ef4444", fontSize: 12, marginTop: 8, textAlign: "center" },
};
