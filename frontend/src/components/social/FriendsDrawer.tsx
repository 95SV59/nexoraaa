import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { socialService } from "../../services/socialService";
import { partyService } from "../../services/partyService";
import { FriendDto, PresenceStatus } from "../../types/social";
import { PartyDto } from "../../types/party";

export function FriendsDrawer() {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<"friends" | "requests" | "party" | "add">("friends");
  const [friends, setFriends] = useState<FriendDto[]>([]);
  const [incomingRequests, setIncomingRequests] = useState<FriendDto[]>([]);
  const [party, setParty] = useState<PartyDto | null>(null);
  const [selectedGame, setSelectedGame] = useState("arena");
  const [addUsername, setAddUsername] = useState("");
  const [addMsg, setAddMsg] = useState<{ text: string; error: boolean } | null>(null);
  const [partyMsg, setPartyMsg] = useState<{ text: string; error: boolean } | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 10000); // 10s auto-refresh
    return () => clearInterval(interval);
  }, []);

  async function loadData() {
    try {
      const [fList, rList, pData] = await Promise.all([
        socialService.getFriends(),
        socialService.getIncomingRequests(),
        partyService.getCurrentParty().catch(() => null),
      ]);
      setFriends(fList);
      setIncomingRequests(rList);
      setParty(pData);
    } catch {
      // Ignore if session expires
    }
  }

  async function handleSendRequest(e: React.FormEvent) {
    e.preventDefault();
    if (!addUsername.trim()) return;
    setLoading(true);
    setAddMsg(null);
    try {
      await socialService.sendFriendRequest(addUsername.trim());
      setAddMsg({ text: `Friend request sent to ${addUsername}!`, error: false });
      setAddUsername("");
      await loadData();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || "Failed to send request.";
      setAddMsg({ text: msg, error: true });
    } finally {
      setLoading(false);
    }
  }

  async function handleAccept(friendshipId: number) {
    try {
      await socialService.acceptFriendRequest(friendshipId);
      await loadData();
    } catch (e) {
      console.error(e);
    }
  }

  async function handleDecline(friendshipId: number) {
    try {
      await socialService.declineFriendRequest(friendshipId);
      await loadData();
    } catch (e) {
      console.error(e);
    }
  }

  async function handleRemove(friendUserId: number) {
    if (!confirm("Remove this friend?")) return;
    try {
      await socialService.removeFriend(friendUserId);
      await loadData();
    } catch (e) {
      console.error(e);
    }
  }

  async function handleCreateParty() {
    try {
      const p = await partyService.createParty();
      setParty(p);
      setPartyMsg({ text: "Party created successfully!", error: false });
    } catch (err: any) {
      setPartyMsg({ text: err.response?.data?.message || "Failed to create party", error: true });
    }
  }

  async function handleInviteToParty(friendUserId: number) {
    try {
      await partyService.inviteToParty(friendUserId);
      setPartyMsg({ text: "Invitation sent to party!", error: false });
    } catch (err: any) {
      setPartyMsg({ text: err.response?.data?.message || "Failed to invite to party", error: true });
    }
  }

  async function handleLeaveParty() {
    try {
      await partyService.leaveParty();
      setParty(null);
      setPartyMsg({ text: "Left party", error: false });
    } catch (err: any) {
      setPartyMsg({ text: err.response?.data?.message || "Failed to leave party", error: true });
    }
  }

  async function handleLaunchPartyMatch() {
    try {
      setLoading(true);
      const res = await partyService.launchMatch(selectedGame);
      setIsOpen(false);
      navigate(`/play/arena?roomCode=${res.roomCode}&game=${selectedGame}`);
    } catch (err: any) {
      setPartyMsg({ text: err.response?.data?.message || "Failed to launch party match", error: true });
    } finally {
      setLoading(false);
    }
  }

  const onlineFriends = friends.filter((f) => f.presenceStatus !== "OFFLINE");
  const offlineFriends = friends.filter((f) => f.presenceStatus === "OFFLINE");

  const getStatusColor = (status: PresenceStatus) => {
    switch (status) {
      case "ONLINE": return "#00ff87";
      case "IN_GAME": return "#00f2fe";
      case "IN_LOBBY": return "#f59e0b";
      case "AWAY": return "#fbbf24";
      default: return "#64748b";
    }
  };

  const getStatusLabel = (f: FriendDto) => {
    if (f.presenceStatus === "IN_GAME" && f.currentGameSlug) {
      return `Playing ${f.currentGameSlug.toUpperCase()}`;
    }
    if (f.presenceStatus === "IN_LOBBY") return "In Lobby";
    return f.presenceStatus;
  };

  return (
    <>
      {/* Floating Friends & Party Pill (Steam Overlay Style) */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        style={{
          position: "fixed",
          bottom: 20,
          right: 20,
          background: "linear-gradient(135deg, #0f172a, #1e293b)",
          border: "1px solid #00f2fe",
          boxShadow: "0 0 15px rgba(0, 242, 254, 0.3)",
          color: "#f8fafc",
          padding: "10px 18px",
          borderRadius: 24,
          display: "flex",
          alignItems: "center",
          gap: 10,
          cursor: "pointer",
          zIndex: 9999,
          fontWeight: 700,
          fontSize: 13,
        }}
      >
        <span style={{ fontSize: 16 }}>👥</span>
        <span>Friends & Party</span>
        <span
          style={{
            background: "rgba(0, 242, 254, 0.15)",
            color: "#00f2fe",
            padding: "2px 8px",
            borderRadius: 12,
            fontSize: 11,
            fontWeight: 800,
          }}
        >
          {onlineFriends.length}/{friends.length}
        </span>
        {party && (
          <span
            style={{
              background: "#8b5cf6",
              color: "#fff",
              padding: "2px 6px",
              borderRadius: 10,
              fontSize: 10,
              fontWeight: 800,
            }}
          >
            🎉 {party.members.length}
          </span>
        )}
        {incomingRequests.length > 0 && (
          <span
            style={{
              background: "#ef4444",
              color: "#fff",
              padding: "2px 6px",
              borderRadius: 10,
              fontSize: 10,
              fontWeight: 800,
            }}
          >
            +{incomingRequests.length}
          </span>
        )}
      </button>

      {/* Slide-out Drawer */}
      {isOpen && (
        <div
          style={{
            position: "fixed",
            bottom: 70,
            right: 20,
            width: 360,
            height: 520,
            background: "#080d1a",
            border: "1px solid #1e293b",
            borderRadius: 16,
            boxShadow: "0 12px 40px rgba(0,0,0,0.8)",
            zIndex: 9999,
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
            fontFamily: "system-ui, -apple-system, sans-serif",
          }}
        >
          {/* Header */}
          <div
            style={{
              padding: "12px 16px",
              background: "rgba(15, 23, 42, 0.9)",
              borderBottom: "1px solid #1e293b",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <span style={{ color: "#00f2fe", fontWeight: 800, fontSize: 14 }}>COMMUNITY & SOCIAL</span>
              {party && (
                <span style={{ background: "#8b5cf6", color: "#fff", fontSize: 10, padding: "2px 6px", borderRadius: 4, fontWeight: 700 }}>
                  PARTY: {party.partyCode}
                </span>
              )}
            </div>
            <button
              onClick={() => setIsOpen(false)}
              style={{
                background: "transparent",
                border: "none",
                color: "#94a3b8",
                fontSize: 16,
                cursor: "pointer",
              }}
            >
              ✕
            </button>
          </div>

          {/* Navigation Tabs */}
          <div style={{ display: "flex", borderBottom: "1px solid #1e293b", background: "#0a101f" }}>
            <button
              onClick={() => setActiveTab("friends")}
              style={{
                flex: 1,
                padding: "8px 0",
                background: activeTab === "friends" ? "rgba(0,242,254,0.1)" : "transparent",
                color: activeTab === "friends" ? "#00f2fe" : "#94a3b8",
                border: "none",
                borderBottom: activeTab === "friends" ? "2px solid #00f2fe" : "none",
                fontSize: 12,
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              Friends ({friends.length})
            </button>
            <button
              onClick={() => setActiveTab("party")}
              style={{
                flex: 1,
                padding: "8px 0",
                background: activeTab === "party" ? "rgba(139,92,246,0.15)" : "transparent",
                color: activeTab === "party" ? "#a78bfa" : "#94a3b8",
                border: "none",
                borderBottom: activeTab === "party" ? "2px solid #a78bfa" : "none",
                fontSize: 12,
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              Party {party ? `(${party.members.length})` : ""}
            </button>
            <button
              onClick={() => setActiveTab("requests")}
              style={{
                flex: 1,
                padding: "8px 0",
                background: activeTab === "requests" ? "rgba(0,242,254,0.1)" : "transparent",
                color: activeTab === "requests" ? "#00f2fe" : "#94a3b8",
                border: "none",
                borderBottom: activeTab === "requests" ? "2px solid #00f2fe" : "none",
                fontSize: 12,
                fontWeight: 600,
                cursor: "pointer",
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                gap: 4,
              }}
            >
              <span>Req</span>
              {incomingRequests.length > 0 && (
                <span style={{ background: "#ef4444", color: "#fff", fontSize: 9, padding: "1px 5px", borderRadius: 8 }}>
                  {incomingRequests.length}
                </span>
              )}
            </button>
            <button
              onClick={() => setActiveTab("add")}
              style={{
                flex: 1,
                padding: "8px 0",
                background: activeTab === "add" ? "rgba(0,242,254,0.1)" : "transparent",
                color: activeTab === "add" ? "#00f2fe" : "#94a3b8",
                border: "none",
                borderBottom: activeTab === "add" ? "2px solid #00f2fe" : "none",
                fontSize: 12,
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              + Add
            </button>
          </div>

          {/* Drawer Body */}
          <div style={{ flex: 1, overflowY: "auto", padding: 12 }}>
            {/* Friends Tab */}
            {activeTab === "friends" && (
              <div>
                {friends.length === 0 ? (
                  <div style={{ textAlign: "center", color: "#64748b", padding: "40px 10px", fontSize: 13 }}>
                    No friends added yet.<br />
                    Click <b>+ Add</b> above to connect with players!
                  </div>
                ) : (
                  <>
                    {/* Online Section */}
                    {onlineFriends.length > 0 && (
                      <div style={{ marginBottom: 14 }}>
                        <div style={{ fontSize: 10, fontWeight: 700, color: "#00ff87", letterSpacing: 1, marginBottom: 8 }}>
                          ONLINE — {onlineFriends.length}
                        </div>
                        {onlineFriends.map((f) => (
                          <div
                            key={f.friendUserId}
                            style={{
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "space-between",
                              padding: "6px 8px",
                              borderRadius: 8,
                              background: "#0f172a",
                              marginBottom: 6,
                              border: "1px solid #1e293b",
                            }}
                          >
                            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                              <div style={{ position: "relative" }}>
                                <img
                                  src={f.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${f.username}`}
                                  alt={f.username}
                                  style={{ width: 32, height: 32, borderRadius: 16, background: "#1e293b" }}
                                />
                                <span
                                  style={{
                                    position: "absolute",
                                    bottom: 0,
                                    right: 0,
                                    width: 8,
                                    height: 8,
                                    borderRadius: 4,
                                    background: getStatusColor(f.presenceStatus),
                                    boxShadow: `0 0 6px ${getStatusColor(f.presenceStatus)}`,
                                  }}
                                />
                              </div>
                              <div>
                                <div style={{ fontSize: 13, fontWeight: 600, color: "#f8fafc" }}>{f.username}</div>
                                <div style={{ fontSize: 10, color: getStatusColor(f.presenceStatus), fontWeight: 500 }}>
                                  {getStatusLabel(f)}
                                </div>
                              </div>
                            </div>

                            <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
                              {party && (
                                <button
                                  onClick={() => handleInviteToParty(f.friendUserId)}
                                  style={{
                                    fontSize: 10,
                                    background: "rgba(139, 92, 246, 0.2)",
                                    border: "1px solid #8b5cf6",
                                    color: "#a78bfa",
                                    padding: "3px 6px",
                                    borderRadius: 4,
                                    cursor: "pointer",
                                    fontWeight: 600,
                                  }}
                                  title="Invite to party"
                                >
                                  +Party
                                </button>
                              )}
                              {f.currentRoomCode && (
                                <Link
                                  to={`/play/arena?roomCode=${f.currentRoomCode}&game=${f.currentGameSlug || "arena"}`}
                                  style={{
                                    fontSize: 10,
                                    background: "linear-gradient(135deg, #00f2fe, #4facfe)",
                                    color: "#080d1a",
                                    padding: "3px 8px",
                                    borderRadius: 4,
                                    textDecoration: "none",
                                    fontWeight: 700,
                                  }}
                                >
                                  Join
                                </Link>
                              )}
                              <button
                                onClick={() => handleRemove(f.friendUserId)}
                                style={{ background: "transparent", border: "none", color: "#64748b", cursor: "pointer", fontSize: 12 }}
                                title="Remove friend"
                              >
                                ✕
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Offline Section */}
                    {offlineFriends.length > 0 && (
                      <div>
                        <div style={{ fontSize: 10, fontWeight: 700, color: "#64748b", letterSpacing: 1, marginBottom: 8 }}>
                          OFFLINE — {offlineFriends.length}
                        </div>
                        {offlineFriends.map((f) => (
                          <div
                            key={f.friendUserId}
                            style={{
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "space-between",
                              padding: "6px 8px",
                              borderRadius: 8,
                              background: "rgba(15, 23, 42, 0.4)",
                              marginBottom: 6,
                              border: "1px solid rgba(30, 41, 59, 0.4)",
                              opacity: 0.7,
                            }}
                          >
                            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                              <img
                                src={f.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${f.username}`}
                                alt={f.username}
                                style={{ width: 28, height: 28, borderRadius: 14, filter: "grayscale(80%)" }}
                              />
                              <div>
                                <div style={{ fontSize: 12, fontWeight: 600, color: "#94a3b8" }}>{f.username}</div>
                                <div style={{ fontSize: 10, color: "#64748b" }}>Offline</div>
                              </div>
                            </div>
                            <button
                              onClick={() => handleRemove(f.friendUserId)}
                              style={{ background: "transparent", border: "none", color: "#475569", cursor: "pointer", fontSize: 11 }}
                              title="Remove friend"
                            >
                              ✕
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </>
                )}
              </div>
            )}

            {/* Party Tab */}
            {activeTab === "party" && (
              <div>
                {!party ? (
                  <div style={{ textAlign: "center", padding: "24px 10px" }}>
                    <div style={{ fontSize: 32, marginBottom: 10 }}>🎉</div>
                    <div style={{ fontSize: 14, fontWeight: 700, color: "#f8fafc", marginBottom: 6 }}>No Active Party</div>
                    <div style={{ fontSize: 12, color: "#94a3b8", lineHeight: 1.5, marginBottom: 16 }}>
                      Create a party to group up with friends and queue into multiplayer matches together as a team!
                    </div>
                    <button
                      onClick={handleCreateParty}
                      style={{
                        background: "linear-gradient(135deg, #8b5cf6, #6366f1)",
                        border: "none",
                        color: "#fff",
                        fontWeight: 700,
                        fontSize: 13,
                        padding: "10px 20px",
                        borderRadius: 8,
                        cursor: "pointer",
                        boxShadow: "0 0 15px rgba(139,92,246,0.3)",
                      }}
                    >
                      + Create Party
                    </button>
                  </div>
                ) : (
                  <div>
                    <div style={{ background: "#0f172a", border: "1px solid #334155", borderRadius: 8, padding: 12, marginBottom: 12 }}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                        <span style={{ fontSize: 11, color: "#a78bfa", fontWeight: 700 }}>PARTY CODE</span>
                        <span style={{ fontSize: 14, fontWeight: 800, color: "#00f2fe", letterSpacing: 1.5 }}>
                          {party.partyCode}
                        </span>
                      </div>
                      <div style={{ fontSize: 11, color: "#94a3b8" }}>
                        Leader: <b style={{ color: "#f8fafc" }}>{party.leaderUsername}</b> • Slots: {party.members.length}/{party.maxMembers}
                      </div>
                    </div>

                    <div style={{ fontSize: 11, fontWeight: 700, color: "#a78bfa", marginBottom: 8 }}>PARTY ROSTER</div>
                    <div style={{ marginBottom: 14 }}>
                      {party.members.map((m) => (
                        <div
                          key={m.userId}
                          style={{
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "space-between",
                            background: "#0f172a",
                            border: "1px solid #1e293b",
                            borderRadius: 6,
                            padding: "6px 10px",
                            marginBottom: 6,
                          }}
                        >
                          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                            <img
                              src={m.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${m.username}`}
                              alt={m.username}
                              style={{ width: 28, height: 28, borderRadius: 14 }}
                            />
                            <span style={{ fontSize: 12, fontWeight: 600, color: "#f8fafc" }}>{m.username}</span>
                          </div>
                          {m.isLeader && (
                            <span style={{ fontSize: 10, background: "#8b5cf6", color: "#fff", padding: "1px 6px", borderRadius: 4, fontWeight: 700 }}>
                              👑 Leader
                            </span>
                          )}
                        </div>
                      ))}
                    </div>

                    {/* Launch Match Controls for Party Leader */}
                    <div style={{ background: "rgba(15, 23, 42, 0.8)", border: "1px solid #1e293b", borderRadius: 8, padding: 12, marginBottom: 12 }}>
                      <label style={{ fontSize: 11, fontWeight: 700, color: "#cbd5e1", display: "block", marginBottom: 6 }}>
                        Select Match Title
                      </label>
                      <select
                        value={selectedGame}
                        onChange={(e) => setSelectedGame(e.target.value)}
                        style={{
                          width: "100%",
                          padding: "8px",
                          background: "#080d1a",
                          border: "1px solid #334155",
                          borderRadius: 6,
                          color: "#f8fafc",
                          fontSize: 12,
                          marginBottom: 8,
                        }}
                      >
                        <option value="arena">⚡ Nexora Arena (Combat)</option>
                        <option value="racers">🏎️ Nexora Racers (Circuit)</option>
                        <option value="football">⚽ Nexora Football (Sports)</option>
                        <option value="tag">🏃 Nexora Tag (Evasion)</option>
                      </select>

                      <button
                        onClick={handleLaunchPartyMatch}
                        disabled={loading}
                        style={{
                          width: "100%",
                          background: "linear-gradient(135deg, #00f2fe, #4facfe)",
                          border: "none",
                          color: "#080d1a",
                          fontWeight: 800,
                          fontSize: 12,
                          padding: "9px 0",
                          borderRadius: 6,
                          cursor: loading ? "default" : "pointer",
                          marginBottom: 8,
                        }}
                      >
                        {loading ? "Launching Match..." : "🚀 Launch Party Match"}
                      </button>

                      <button
                        onClick={handleLeaveParty}
                        style={{
                          width: "100%",
                          background: "#334155",
                          border: "none",
                          color: "#f8fafc",
                          fontWeight: 600,
                          fontSize: 11,
                          padding: "7px 0",
                          borderRadius: 6,
                          cursor: "pointer",
                        }}
                      >
                        Leave Party
                      </button>
                    </div>
                  </div>
                )}

                {partyMsg && (
                  <div
                    style={{
                      fontSize: 11,
                      color: partyMsg.error ? "#ef4444" : "#00ff87",
                      background: partyMsg.error ? "rgba(239, 68, 68, 0.1)" : "rgba(0, 255, 135, 0.1)",
                      padding: "8px 10px",
                      borderRadius: 6,
                      border: `1px solid ${partyMsg.error ? "rgba(239, 68, 68, 0.3)" : "rgba(0, 255, 135, 0.3)"}`,
                      marginTop: 8,
                    }}
                  >
                    {partyMsg.text}
                  </div>
                )}
              </div>
            )}

            {/* Requests Tab */}
            {activeTab === "requests" && (
              <div>
                {incomingRequests.length === 0 ? (
                  <div style={{ textAlign: "center", color: "#64748b", padding: "40px 10px", fontSize: 13 }}>
                    No pending friend requests.
                  </div>
                ) : (
                  <div>
                    <div style={{ fontSize: 11, fontWeight: 700, color: "#00f2fe", marginBottom: 10 }}>
                      INCOMING REQUESTS ({incomingRequests.length})
                    </div>
                    {incomingRequests.map((r) => (
                      <div
                        key={r.friendshipId}
                        style={{
                          background: "#0f172a",
                          border: "1px solid #1e293b",
                          borderRadius: 8,
                          padding: 10,
                          marginBottom: 8,
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
                          <img
                            src={r.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${r.username}`}
                            alt={r.username}
                            style={{ width: 32, height: 32, borderRadius: 16 }}
                          />
                          <div>
                            <div style={{ fontSize: 13, fontWeight: 700, color: "#f8fafc" }}>{r.username}</div>
                            <div style={{ fontSize: 10, color: "#94a3b8" }}>Wants to be friends</div>
                          </div>
                        </div>
                        <div style={{ display: "flex", gap: 8 }}>
                          <button
                            onClick={() => handleAccept(r.friendshipId)}
                            style={{
                              flex: 1,
                              background: "#00ff87",
                              border: "none",
                              color: "#080d1a",
                              fontWeight: 700,
                              fontSize: 11,
                              padding: "6px 0",
                              borderRadius: 4,
                              cursor: "pointer",
                            }}
                          >
                            Accept
                          </button>
                          <button
                            onClick={() => handleDecline(r.friendshipId)}
                            style={{
                              flex: 1,
                              background: "#334155",
                              border: "none",
                              color: "#f8fafc",
                              fontWeight: 600,
                              fontSize: 11,
                              padding: "6px 0",
                              borderRadius: 4,
                              cursor: "pointer",
                            }}
                          >
                            Decline
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Add Friend Tab */}
            {activeTab === "add" && (
              <form onSubmit={handleSendRequest} style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: "#cbd5e1", display: "block", marginBottom: 6 }}>
                    Find Player by Username
                  </label>
                  <input
                    type="text"
                    value={addUsername}
                    onChange={(e) => setAddUsername(e.target.value)}
                    placeholder="Enter username (e.g. player2)..."
                    style={{
                      width: "100%",
                      boxSizing: "border-box",
                      padding: "8px 10px",
                      background: "#0f172a",
                      border: "1px solid #1e293b",
                      borderRadius: 6,
                      color: "#f8fafc",
                      fontSize: 13,
                    }}
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading || !addUsername.trim()}
                  style={{
                    background: "linear-gradient(135deg, #00f2fe, #4facfe)",
                    border: "none",
                    color: "#080d1a",
                    fontWeight: 700,
                    fontSize: 12,
                    padding: "8px 0",
                    borderRadius: 6,
                    cursor: loading ? "default" : "pointer",
                    opacity: loading || !addUsername.trim() ? 0.6 : 1,
                  }}
                >
                  {loading ? "Sending..." : "Send Friend Request"}
                </button>

                {addMsg && (
                  <div
                    style={{
                      fontSize: 11,
                      color: addMsg.error ? "#ef4444" : "#00ff87",
                      background: addMsg.error ? "rgba(239, 68, 68, 0.1)" : "rgba(0, 255, 135, 0.1)",
                      padding: "8px 10px",
                      borderRadius: 6,
                      border: `1px solid ${addMsg.error ? "rgba(239, 68, 68, 0.3)" : "rgba(0, 255, 135, 0.3)"}`,
                    }}
                  >
                    {addMsg.text}
                  </div>
                )}
              </form>
            )}
          </div>
        </div>
      )}
    </>
  );
}
