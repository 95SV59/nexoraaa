import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { roomService } from "../../services/roomService";
import { matchmakingService } from "../../services/matchmakingService";
import { useArenaSocket } from "../../hooks/useArenaSocket";
import { RoomLobby } from "../../components/multiplayer/RoomLobby";
import { ArenaCanvas } from "../../components/multiplayer/ArenaCanvas";
import { RacersCanvas } from "../../components/multiplayer/RacersCanvas";
import { FootballCanvas } from "../../components/multiplayer/FootballCanvas";
import { TagCanvas } from "../../components/multiplayer/TagCanvas";
import { NetworkStatusPanel } from "../../components/network/NetworkStatusPanel";
import { NetworkLabPanel } from "../../components/network/NetworkLabPanel";
import type { PlayableSlug, RoomResponse } from "../../types/multiplayer";

const GAME_OPTIONS: Array<{ slug: PlayableSlug; title: string; desc: string; maxPlayers: number }> = [
  { slug: "arena", title: "NEXORA Arena", desc: "Tactical 2D top-down shooter with high-frequency projectile sync", maxPlayers: 8 },
  { slug: "racers", title: "NEXORA Racers", desc: "Multiplayer kart racing with drift steering & lap checkpoints", maxPlayers: 8 },
  { slug: "football", title: "NEXORA Football", desc: "Arcade soccer match emphasizing critical ball position sync", maxPlayers: 6 },
  { slug: "tag", title: "NEXORA Tag", desc: "Fast evasive tag arena benchmarking rapid state transitions & jitter", maxPlayers: 10 },
];

export function MultiplayerArenaPage() {
  const { user, token } = useAuth();
  const [searchParams] = useSearchParams();
  const initialGame = (searchParams.get("game") as PlayableSlug) || "arena";

  const [selectedGame, setSelectedGame] = useState<PlayableSlug>(
    GAME_OPTIONS.some((g) => g.slug === initialGame) ? initialGame : "arena"
  );
  const [joinCode, setJoinCode] = useState("");
  const [controllerMode, setControllerMode] = useState<"BASELINE" | "PROPOSED">("PROPOSED");
  const [room, setRoom] = useState<RoomResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [isSearching, setIsSearching] = useState(false);
  const [searchElapsed, setSearchElapsed] = useState(0);
  const [queueCount, setQueueCount] = useState(1);

  const socket = useArenaSocket(room?.roomCode ?? "__none__", token ?? "", user?.userId);
  const matchReset = socket.lifecycle?.event === "REMATCH_RESET";
  const matchStarted = !matchReset && (socket.lifecycle?.event === "GAME_START" || room?.status === "IN_PROGRESS");
  const matchEnded = !matchReset && socket.lifecycle?.event === "GAME_END";

  const currentParticipant = (socket.lobby?.players ?? room?.players ?? []).find(
    (p) => p.userId === user?.userId
  );
  const isSpectator = currentParticipant?.spectator === true;

  useEffect(() => {
    const g = searchParams.get("game") as PlayableSlug;
    if (g && GAME_OPTIONS.some((opt) => opt.slug === g)) {
      setSelectedGame(g);
    }

    const rCode = searchParams.get("roomCode");
    if (rCode && !room) {
      const savedToken = roomService.getReconnectToken(rCode);
      if (savedToken) {
        roomService.reconnectRoom(rCode, savedToken)
          .then((r) => setRoom(r))
          .catch(() => {
            roomService.joinRoom({ roomCode: rCode }).then(setRoom).catch(() => {});
          });
      } else {
        roomService.joinRoom({ roomCode: rCode }).then(setRoom).catch(() => {});
      }
    }
  }, [searchParams]);

  useEffect(() => {
    if (room?.reconnectToken) {
      roomService.saveReconnectToken(room.roomCode, room.reconnectToken);
    }
  }, [room]);

  useEffect(() => {
    let timer: any = null;
    let pollInterval: any = null;

    if (isSearching) {
      setSearchElapsed(0);
      timer = setInterval(() => {
        setSearchElapsed((prev) => prev + 1);
      }, 1000);

      pollInterval = setInterval(async () => {
        try {
          const status = await matchmakingService.getStatus();
          if (status.status === "MATCHED" && status.roomCode) {
            setIsSearching(false);
            const foundRoom = await roomService.getRoom(status.roomCode);
            setRoom(foundRoom);
          } else if (status.status === "QUEUED") {
            setQueueCount(status.queueSize);
          }
        } catch (err) {
          console.error("Matchmaking poll failed:", err);
        }
      }, 1500);
    }

    return () => {
      if (timer) clearInterval(timer);
      if (pollInterval) clearInterval(pollInterval);
    };
  }, [isSearching]);

  async function handleStartMatchmaking() {
    setError(null);
    try {
      const res = await matchmakingService.joinQueue(selectedGame);
      setQueueCount(res.queueSize || 1);
      setIsSearching(true);
    } catch {
      setError("Failed to enter casual matchmaking. Ensure the server is online.");
    }
  }

  async function handleCancelMatchmaking() {
    try {
      await matchmakingService.leaveQueue();
    } catch {
      // Ignore
    } finally {
      setIsSearching(false);
    }
  }

  async function handleCreate() {
    setError(null);
    try {
      const selected = GAME_OPTIONS.find((g) => g.slug === selectedGame)!;
      setRoom(await roomService.createRoom({
        gameId: selectedGame,
        maxPlayers: selected.maxPlayers,
        controllerMode
      }));
    } catch {
      setError("Could not create room. Ensure the backend server is reachable.");
    }
  }

  async function handleJoin() {
    setError(null);
    try {
      setRoom(await roomService.joinRoom({ roomCode: joinCode.trim().toUpperCase() }));
    } catch {
      setError("Couldn't join room. Please verify the room code.");
    }
  }

  async function handleSpectate() {
    setError(null);
    try {
      setRoom(await roomService.spectateRoom(joinCode.trim().toUpperCase()));
    } catch {
      setError("Couldn't spectate room. Please verify the room code.");
    }
  }

  const [rematchLoading, setRematchLoading] = useState(false);
  async function handleRematch() {
    if (!room) return;
    setRematchLoading(true);
    try {
      const updated = await roomService.rematch(room.roomCode);
      setRoom(updated);
    } catch {
      setError("Could not trigger rematch. Only room host can reset.");
    } finally {
      setRematchLoading(false);
    }
  }

  if (!user) return null;

  if (!room) {
    return (
      <div style={containerStyle}>
        <div style={headerStyle}>
          <span style={badgeStyle}>MULTIPLAYER ARENA</span>
          <h1 style={titleStyle}>Enter the Real-Time Arena</h1>
          <p style={subtitleStyle}>
            Select your game, create or join a room, and experience predictive what-if network control.
          </p>
        </div>

        {/* Game Selection Cards */}
        <div style={gameGridStyle}>
          {GAME_OPTIONS.map((g) => (
            <div
              key={g.slug}
              onClick={() => setSelectedGame(g.slug)}
              style={{
                ...gameCardStyle,
                borderColor: selectedGame === g.slug ? "#00f2fe" : "#1e293b",
                boxShadow: selectedGame === g.slug ? "0 0 20px rgba(0,242,254,0.25)" : "none",
                background: selectedGame === g.slug ? "rgba(0,242,254,0.06)" : "#0f172a",
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <h3 style={{ margin: 0, color: selectedGame === g.slug ? "#00f2fe" : "#f8fafc", fontSize: 16 }}>
                  {g.title}
                </h3>
                <span style={{ fontSize: 11, color: "#94a3b8", background: "#1e293b", padding: "2px 8px", borderRadius: 4 }}>
                  Max {g.maxPlayers}
                </span>
              </div>
              <p style={{ margin: "8px 0 0", color: "#94a3b8", fontSize: 13, lineHeight: 1.4 }}>{g.desc}</p>
            </div>
          ))}
        </div>

        {/* Casual Quick Match Banner */}
        {isSearching ? (
          <div style={searchingCardStyle}>
            <div style={{ display: "inline-flex", alignItems: "center", gap: 10, marginBottom: 10 }}>
              <div style={{
                width: 12, height: 12, borderRadius: "50%", background: "#00f2fe",
                boxShadow: "0 0 12px #00f2fe",
              }} />
              <span style={{ fontSize: 13, fontWeight: 800, color: "#00f2fe", letterSpacing: 1.5 }}>
                MATCHMAKING SEARCH ACTIVE
              </span>
            </div>
            <h3 style={{ margin: "4px 0", fontSize: 22, color: "#f8fafc" }}>
              Searching for {GAME_OPTIONS.find((g) => g.slug === selectedGame)?.title} Opponents…
            </h3>
            <p style={{ margin: "8px 0 18px", color: "#94a3b8", fontSize: 13 }}>
              In queue: <strong style={{ color: "#00ff87" }}>{queueCount} player{queueCount > 1 ? "s" : ""}</strong> · Elapsed: <strong style={{ color: "#f8fafc" }}>{Math.floor(searchElapsed / 60)}:{String(searchElapsed % 60).padStart(2, "0")}</strong>
              {searchElapsed >= 10 && (
                <span style={{ display: "block", color: "#f59e0b", marginTop: 4 }}>
                  ⚡ Standby: Auto-filling empty slots with AI bot participants…
                </span>
              )}
            </p>
            <button onClick={handleCancelMatchmaking} style={cancelButtonStyle}>
              Cancel Search
            </button>
          </div>
        ) : (
          <div style={quickMatchBannerStyle}>
            <div style={{ flex: 1, minWidth: 260 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 4 }}>
                <span style={{ fontSize: 18 }}>⚡</span>
                <h3 style={{ margin: 0, fontSize: 18, color: "#f8fafc", fontWeight: 700 }}>
                  Quick Match (Casual Matchmaking)
                </h3>
                <span style={{ fontSize: 10, background: "rgba(0,255,135,0.15)", color: "#00ff87", border: "1px solid rgba(0,255,135,0.3)", padding: "2px 8px", borderRadius: 4, fontWeight: 800 }}>
                  AUTO-MATCH
                </span>
              </div>
              <p style={{ margin: 0, color: "#94a3b8", fontSize: 13, lineHeight: 1.4 }}>
                Find players in <strong style={{ color: "#00f2fe" }}>{GAME_OPTIONS.find((g) => g.slug === selectedGame)?.title}</strong> or automatically match with AI bots if waiting.
              </p>
            </div>
            <button onClick={handleStartMatchmaking} style={quickMatchBtnStyle}>
              ⚡ Find Match Now
            </button>
          </div>
        )}

        {/* Room Setup Box */}
        <div style={formCardStyle}>
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>
              Predictive Network Controller Mode
              <select
                value={controllerMode}
                onChange={(e) => setControllerMode(e.target.value as "BASELINE" | "PROPOSED")}
                style={selectStyle}
              >
                <option value="PROPOSED">PROPOSED — Closed-loop predictive adaptation active</option>
                <option value="BASELINE">BASELINE — Fixed broadcast rate (for comparative evaluation)</option>
              </select>
            </label>
            <p style={{ margin: "4px 0 0", fontSize: 12, color: "#64748b" }}>
              {controllerMode === "PROPOSED"
                ? "The controller will forecast latency/loss, virtually evaluate candidate strategies, and apply optimal network controls."
                : "Standard multiplayer baseline without predictive optimization, used to measure experimental improvements."}
            </p>
          </div>

          <button style={createButtonStyle} onClick={handleCreate}>
            Create {GAME_OPTIONS.find((g) => g.slug === selectedGame)?.title} Room
          </button>

          <div style={{ display: "flex", alignItems: "center", gap: 12, margin: "20px 0" }}>
            <div style={{ flex: 1, height: 1, background: "#1e293b" }} />
            <span style={{ color: "#64748b", fontSize: 12 }}>OR JOIN EXISTING</span>
            <div style={{ flex: 1, height: 1, background: "#1e293b" }} />
          </div>

          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            <input
              placeholder="ENTER ROOM CODE (e.g. A3F8)"
              value={joinCode}
              onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
              style={{ ...inputStyle, flex: 1, minWidth: 180 }}
            />
            <button style={joinButtonStyle} onClick={handleJoin} disabled={!joinCode.trim()}>
              Join Match
            </button>
            <button style={spectateButtonStyle} onClick={handleSpectate} disabled={!joinCode.trim()}>
              👁 Watch Spectator
            </button>
          </div>

          {error && <div style={errorStyle}>{error}</div>}
        </div>
      </div>
    );
  }

  // Match Over State
  if (matchEnded && socket.lifecycle?.matchResult) {
    const result = socket.lifecycle.matchResult;
    return (
      <div style={{ maxWidth: 540, margin: "60px auto", padding: 24, background: "#0f172a", borderRadius: 16, border: "1px solid #1e293b", textAlign: "center" }}>
        <h1 style={{ color: "#00ff87", margin: 0 }}>MATCH COMPLETED</h1>
        {socket.lifecycle.sessionId !== null && (
          <p style={{ color: "#94a3b8", fontSize: 13, marginTop: 8 }}>
            Telemetry Session #{socket.lifecycle.sessionId} recorded for Research Laboratory analysis.
          </p>
        )}
        <div style={{ margin: "24px 0", padding: 16, background: "#1e293b", borderRadius: 12 }}>
          <div style={{ fontSize: 13, color: "#94a3b8" }}>WINNER</div>
          <div style={{ fontSize: 24, fontWeight: 700, color: "#00f2fe", marginTop: 4 }}>
            {result.summary[result.winnerPlayerId ?? ""]?.displayName ?? "Match Finished"}
          </div>
          <div style={{ fontSize: 12, color: "#64748b", marginTop: 4 }}>
            Duration: {result.durationSec} seconds
          </div>
        </div>

        <div style={{ display: "flex", gap: 12, justifyContent: "center", marginTop: 20 }}>
          {room && room.hostUserId === user.userId && (
            <button style={rematchButtonStyle} onClick={handleRematch} disabled={rematchLoading}>
              {rematchLoading ? "Resetting Room..." : "🔄 Rematch (Same Players)"}
            </button>
          )}
          <button style={primaryButton} onClick={() => setRoom(null)}>
            Return to Arena Lobby
          </button>
        </div>
      </div>
    );
  }

  // In Lobby
  if (!matchStarted) {
    return (
      <div style={{ maxWidth: 700, margin: "40px auto", padding: "0 16px" }}>
        <RoomLobby
          room={room}
          lobby={socket.lobby}
          currentUserId={user.userId}
          onToggleReady={(ready: boolean) => socket.sendReady({ ready })}
        />
        {error && <p style={errorStyle}>{error}</p>}
      </div>
    );
  }

  // Active Gameplay View
  const renderGameCanvas = () => {
    const gameId = room.gameId;
    const inputHandler = isSpectator ? () => {} : socket.sendInput;
    if (gameId === "racers") {
      return (
        <RacersCanvas
          state={socket.state}
          currentPlayerId={String(user.userId)}
          sendInput={inputHandler}
        />
      );
    } else if (gameId === "football") {
      return (
        <FootballCanvas
          state={socket.state}
          currentPlayerId={String(user.userId)}
          sendInput={inputHandler}
        />
      );
    } else if (gameId === "tag") {
      return (
        <TagCanvas
          state={socket.state}
          currentPlayerId={String(user.userId)}
          sendInput={inputHandler}
        />
      );
    } else {
      return (
        <ArenaCanvas
          state={socket.state}
          currentPlayerId={String(user.userId)}
          sendInput={inputHandler}
        />
      );
    }
  };

  return (
    <div style={{ padding: "16px 24px", maxWidth: 1400, margin: "0 auto" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
        <div>
          <span style={{ fontSize: 12, color: "#00f2fe", fontWeight: 700, letterSpacing: 1 }}>LIVE MULTIPLAYER SESSION</span>
          <h2 style={{ margin: 0, color: "#f8fafc", fontSize: 20 }}>
            {GAME_OPTIONS.find((g) => g.slug === room.gameId)?.title ?? room.gameId} · Room {room.roomCode}
          </h2>
        </div>
        <button
          onClick={async () => {
            await roomService.leaveRoom(room.roomCode);
            setRoom(null);
          }}
          style={leaveButtonStyle}
        >
          Leave Match
        </button>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 340px", gap: 20, alignItems: "start" }}>
        {/* Game Canvas Container */}
        <div>
          {isSpectator && (
            <div style={spectatorBannerStyle}>
              <span style={{ fontSize: 16 }}>👁</span>
              <div>
                <strong style={{ letterSpacing: 1 }}>SPECTATOR MODE ACTIVE</strong>
                <span style={{ marginLeft: 8, opacity: 0.85, fontSize: 12 }}>
                  Observing live authoritative simulation (Client inputs disabled)
                </span>
              </div>
            </div>
          )}
          {renderGameCanvas()}
        </div>

        {/* Live Controller Dashboard Sidebar */}
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          <NetworkStatusPanel snapshot={socket.networkSnapshot} />
          <NetworkLabPanel roomCode={room.roomCode} />
        </div>
      </div>
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  maxWidth: 800,
  margin: "40px auto",
  padding: "0 20px",
  fontFamily: "system-ui, -apple-system, sans-serif",
};

const headerStyle: React.CSSProperties = {
  textAlign: "center",
  marginBottom: 32,
};

const badgeStyle: React.CSSProperties = {
  display: "inline-block",
  padding: "4px 12px",
  borderRadius: 20,
  fontSize: 11,
  fontWeight: 700,
  letterSpacing: 1.5,
  color: "#00f2fe",
  background: "rgba(0, 242, 254, 0.1)",
  border: "1px solid rgba(0, 242, 254, 0.25)",
  marginBottom: 12,
};

const titleStyle: React.CSSProperties = {
  fontSize: 32,
  fontWeight: 800,
  color: "#f8fafc",
  margin: 0,
};

const subtitleStyle: React.CSSProperties = {
  fontSize: 14,
  color: "#94a3b8",
  marginTop: 8,
};

const gameGridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
  gap: 16,
  marginBottom: 24,
};

const gameCardStyle: React.CSSProperties = {
  padding: 16,
  borderRadius: 12,
  border: "1px solid #1e293b",
  cursor: "pointer",
  transition: "all 0.2s ease",
};

const formCardStyle: React.CSSProperties = {
  background: "#0f172a",
  borderRadius: 14,
  padding: 24,
  border: "1px solid #1e293b",
};

const labelStyle: React.CSSProperties = {
  display: "block",
  color: "#cbd5e1",
  fontSize: 13,
  fontWeight: 600,
};

const selectStyle: React.CSSProperties = {
  width: "100%",
  padding: "10px 12px",
  marginTop: 6,
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 8,
  color: "#f8fafc",
  fontSize: 14,
  outline: "none",
};

const inputStyle: React.CSSProperties = {
  flex: 1,
  padding: "12px 16px",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 8,
  color: "#f8fafc",
  fontSize: 14,
  letterSpacing: 1.5,
  outline: "none",
};

const createButtonStyle: React.CSSProperties = {
  width: "100%",
  padding: "14px",
  background: "linear-gradient(135deg, #00f2fe 0%, #4facfe 100%)",
  color: "#0f172a",
  fontWeight: 700,
  fontSize: 15,
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
  transition: "opacity 0.2s",
};

const joinButtonStyle: React.CSSProperties = {
  padding: "0 24px",
  background: "#334155",
  color: "#f8fafc",
  fontWeight: 600,
  fontSize: 14,
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
};

const primaryButton: React.CSSProperties = {
  padding: "10px 20px",
  background: "#00f2fe",
  color: "#0f172a",
  fontWeight: 700,
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
};

const leaveButtonStyle: React.CSSProperties = {
  padding: "6px 14px",
  background: "transparent",
  border: "1px solid #dc2626",
  color: "#ef4444",
  borderRadius: 6,
  fontSize: 12,
  fontWeight: 600,
  cursor: "pointer",
};

const errorStyle: React.CSSProperties = {
  color: "#ef4444",
  background: "rgba(239, 68, 68, 0.1)",
  border: "1px solid rgba(239, 68, 68, 0.2)",
  padding: "10px 14px",
  borderRadius: 8,
  marginTop: 14,
  fontSize: 13,
};

const quickMatchBannerStyle: React.CSSProperties = {
  background: "linear-gradient(135deg, rgba(0, 242, 254, 0.08), rgba(0, 255, 135, 0.04))",
  border: "1px solid rgba(0, 242, 254, 0.3)",
  borderRadius: 14,
  padding: "18px 24px",
  marginBottom: 20,
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: 16,
  flexWrap: "wrap",
};

const quickMatchBtnStyle: React.CSSProperties = {
  padding: "12px 24px",
  background: "linear-gradient(135deg, #00ff87 0%, #00f2fe 100%)",
  color: "#060913",
  fontWeight: 800,
  fontSize: 14,
  borderRadius: 8,
  border: "none",
  cursor: "pointer",
  letterSpacing: 0.5,
  boxShadow: "0 0 15px rgba(0, 255, 135, 0.3)",
  transition: "all 0.2s ease",
};

const searchingCardStyle: React.CSSProperties = {
  background: "linear-gradient(135deg, rgba(0, 242, 254, 0.12), rgba(15, 23, 42, 0.95))",
  border: "1px solid #00f2fe",
  boxShadow: "0 0 25px rgba(0, 242, 254, 0.2)",
  borderRadius: 14,
  padding: 24,
  marginBottom: 20,
  textAlign: "center",
};

const cancelButtonStyle: React.CSSProperties = {
  padding: "8px 20px",
  background: "#1e293b",
  border: "1px solid #475569",
  color: "#f8fafc",
  borderRadius: 8,
  fontSize: 13,
  fontWeight: 600,
  cursor: "pointer",
};

const spectateButtonStyle: React.CSSProperties = {
  padding: "0 20px",
  background: "rgba(168, 85, 247, 0.15)",
  border: "1px solid rgba(168, 85, 247, 0.4)",
  color: "#c084fc",
  fontWeight: 700,
  fontSize: 13,
  borderRadius: 8,
  cursor: "pointer",
  transition: "all 0.2s ease",
};

const rematchButtonStyle: React.CSSProperties = {
  padding: "10px 20px",
  background: "linear-gradient(135deg, #00ff87 0%, #00f2fe 100%)",
  color: "#060913",
  fontWeight: 800,
  fontSize: 14,
  borderRadius: 8,
  border: "none",
  cursor: "pointer",
};

const spectatorBannerStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 10,
  padding: "10px 16px",
  background: "linear-gradient(90deg, rgba(168, 85, 247, 0.25), rgba(15, 23, 42, 0.8))",
  border: "1px solid rgba(168, 85, 247, 0.4)",
  borderRadius: 8,
  color: "#c084fc",
  marginBottom: 10,
  fontSize: 13,
};
