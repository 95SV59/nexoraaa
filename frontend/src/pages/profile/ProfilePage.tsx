import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { userService } from "../../services/userService";
import { socialService } from "../../services/socialService";
import { getMyProgression } from "../../services/progressionService";
import { matchHistoryService } from "../../services/matchHistoryService";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  AreaChart,
  Area,
} from "recharts";
import { UserProfile } from "../../types/auth";
import { FriendDto } from "../../types/social";
import type { UserProgressionDto } from "../../types/progression";
import type { UserMatchHistoryDto } from "../../types/matchHistory";

export function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [friends, setFriends] = useState<FriendDto[]>([]);
  const [progression, setProgression] = useState<UserProgressionDto | null>(null);
  const [matches, setMatches] = useState<UserMatchHistoryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [email, setEmail] = useState("");
  const [avatarUrl, setAvatarUrl] = useState("");
  const [msg, setMsg] = useState<string | null>(null);

  useEffect(() => {
    loadProfile();
  }, []);

  async function loadProfile() {
    setLoading(true);
    try {
      const [profileData, friendsData, progressionData, matchesData] = await Promise.all([
        userService.getProfile(),
        socialService.getFriends().catch(() => []),
        getMyProgression().catch(() => null),
        matchHistoryService.getHistory().catch(() => []),
      ]);
      setProfile(profileData);
      setFriends(friendsData);
      if (progressionData) setProgression(progressionData);
      setMatches(matchesData);
      setEmail(profileData.email);
      setAvatarUrl(profileData.avatarUrl);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }

  async function handleUpdate(e: React.FormEvent) {
    e.preventDefault();
    setMsg(null);
    try {
      const updated = await userService.updateProfile({ email, avatarUrl });
      setProfile(updated);
      setEditing(false);
      setMsg("Profile updated successfully!");
    } catch {
      setMsg("Failed to update profile.");
    }
  }

  if (loading) return <div style={containerStyle}><p style={{ color: "#64748b" }}>Loading gamer profile…</p></div>;
  if (!profile) return <div style={containerStyle}><p style={{ color: "#ef4444" }}>Could not load profile.</p></div>;

  return (
    <div style={containerStyle}>
      {/* Header Profile Card */}
      <div style={headerCardStyle}>
        <div style={{ display: "flex", gap: 20, alignItems: "center", flexWrap: "wrap" }}>
          <img
            src={profile.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${profile.username}`}
            alt={profile.username}
            style={{ width: 80, height: 80, borderRadius: 40, border: "2px solid #00f2fe", background: "#1e293b" }}
          />
          <div>
            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              <h1 style={{ margin: 0, fontSize: 26, color: "#f8fafc" }}>{profile.username}</h1>
              <span style={roleBadgeStyle}>{profile.role}</span>
              <span style={{ fontSize: 12, color: profile.status === "ACTIVE" ? "#00ff87" : "#ef4444" }}>
                ● {profile.status}
              </span>
            </div>
            <p style={{ margin: "4px 0 0", color: "#94a3b8", fontSize: 13 }}>
              {profile.email} · Member since {new Date(profile.createdAt).toLocaleDateString()}
            </p>
          </div>
          <span style={{ flex: 1 }} />
          <button onClick={() => setEditing(!editing)} style={editBtnStyle}>
            {editing ? "Cancel Edit" : "✎ Edit Profile"}
          </button>
        </div>

        {msg && <p style={{ color: "#00ff87", fontSize: 13, margin: "14px 0 0" }}>{msg}</p>}

        {/* Edit Form */}
        {editing && (
          <form onSubmit={handleUpdate} style={editFormStyle}>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
              <div>
                <label style={labelStyle}>Email Address</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  style={inputStyle}
                  required
                />
              </div>
              <div>
                <label style={labelStyle}>Avatar Image URL</label>
                <input
                  type="url"
                  value={avatarUrl}
                  onChange={(e) => setAvatarUrl(e.target.value)}
                  style={inputStyle}
                  placeholder="https://..."
                />
              </div>
            </div>
            <button type="submit" style={saveBtnStyle}>Save Changes</button>
          </form>
        )}
      </div>

      {/* XP Progression Card */}
      {progression && (
        <div style={{
          background: "#0f172a", border: "1px solid #1e293b", borderRadius: 16,
          padding: "20px 24px", marginTop: 20,
        }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 14, flexWrap: "wrap", gap: 10 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              {/* Level badge */}
              <div style={{
                width: 52, height: 52, borderRadius: 12,
                background: "linear-gradient(135deg, #00f2fe22, #7b2cbf22)",
                border: "2px solid #00f2fe",
                display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center",
              }}>
                <span style={{ fontSize: 9, color: "#00f2fe", fontWeight: 700, letterSpacing: 1 }}>LVL</span>
                <span style={{ fontSize: 20, fontWeight: 900, color: "#f8fafc", lineHeight: 1 }}>
                  {progression.level}
                </span>
              </div>
              <div>
                <div style={{ fontSize: 16, fontWeight: 700, color: "#f8fafc" }}>{progression.rankTitle}</div>
                <div style={{ fontSize: 12, color: "#64748b" }}>
                  {progression.totalXp.toLocaleString()} XP total
                </div>
              </div>
            </div>
            <div style={{ display: "flex", gap: 10 }}>
              <Link to="/achievements" style={{
                padding: "7px 14px", borderRadius: 8, fontSize: 12, fontWeight: 700,
                background: "rgba(0,242,254,0.1)", color: "#00f2fe",
                border: "1px solid rgba(0,242,254,0.3)", textDecoration: "none",
              }}>
                🏆 {progression.achievementsCompleted}/{progression.achievementsTotal} Achievements
              </Link>
              <Link to="/leaderboard" style={{
                padding: "7px 14px", borderRadius: 8, fontSize: 12, fontWeight: 700,
                background: "rgba(245,158,11,0.1)", color: "#f59e0b",
                border: "1px solid rgba(245,158,11,0.3)", textDecoration: "none",
              }}>
                📊 Leaderboard
              </Link>
            </div>
          </div>

          {/* XP Progress bar */}
          <div style={{ marginBottom: 6 }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6, fontSize: 11, color: "#64748b" }}>
              <span>Level {progression.level}</span>
              <span>Level {progression.level + 1}</span>
            </div>
            <div style={{ height: 8, background: "#1e293b", borderRadius: 4, overflow: "hidden" }}>
              <div style={{
                width: `${progression.levelProgressPct}%`, height: "100%", borderRadius: 4,
                background: "linear-gradient(90deg, #00f2fe, #00ff87)",
                transition: "width 0.6s ease",
              }} />
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", marginTop: 5, fontSize: 11, color: "#94a3b8" }}>
              <span>{progression.totalXp - progression.currentLevelXpFloor} / {progression.nextLevelXpThreshold - progression.currentLevelXpFloor} XP</span>
              <span>{progression.levelProgressPct.toFixed(1)}% to next level</span>
            </div>
          </div>

          {/* Quick stats */}
          <div style={{ display: "flex", gap: 20, marginTop: 14, flexWrap: "wrap" }}>
            <div style={{ fontSize: 12, color: "#94a3b8" }}>
              <span style={{ color: "#f8fafc", fontWeight: 700 }}>{progression.matchesPlayed}</span> Matches
            </div>
            <div style={{ fontSize: 12, color: "#94a3b8" }}>
              <span style={{ color: "#00ff87", fontWeight: 700 }}>{progression.matchesWon}</span> Wins
            </div>
            <div style={{ fontSize: 12, color: "#94a3b8" }}>
              <span style={{ color: "#00f2fe", fontWeight: 700 }}>{progression.winRate}%</span> Win Rate
            </div>
          </div>
        </div>
      )}

      {/* Gamer Statistics Grid */}
      <h2 style={{ fontSize: 20, color: "#f8fafc", margin: "32px 0 16px" }}>Performance & Activity Metrics</h2>
      <div style={statsGridStyle}>
        <div style={statCardStyle}>
          <div style={statLabelStyle}>MATCHES PLAYED</div>
          <div style={statValStyle}>{profile.matchesPlayed}</div>
          <div style={statSubStyle}>Authoritative sessions</div>
        </div>

        <div style={statCardStyle}>
          <div style={statLabelStyle}>TOTAL VICTORIES</div>
          <div style={{ ...statValStyle, color: "#00ff87" }}>{profile.wins}</div>
          <div style={statSubStyle}>1st place finishes</div>
        </div>

        <div style={statCardStyle}>
          <div style={statLabelStyle}>WIN RATE</div>
          <div style={{ ...statValStyle, color: "#00f2fe" }}>{profile.winRatePct}%</div>
          <div style={statSubStyle}>Across all game modes</div>
        </div>

        <div style={statCardStyle}>
          <div style={statLabelStyle}>LIBRARY GAMES</div>
          <div style={statValStyle}>{profile.gamesInLibrary}</div>
          <div style={statSubStyle}>Tracked titles</div>
        </div>

        <div style={statCardStyle}>
          <div style={statLabelStyle}>WISHLIST SAVED</div>
          <div style={{ ...statValStyle, color: "#facc15" }}>{profile.wishlistCount}</div>
          <div style={statSubStyle}>Bookmarked titles</div>
        </div>

        <div style={statCardStyle}>
          <div style={statLabelStyle}>REVIEWS POSTED</div>
          <div style={statValStyle}>{profile.reviewsCount}</div>
          <div style={statSubStyle}>Community ratings</div>
        </div>
      </div>

      {/* Recharts Analytics Dashboard */}
      <div style={{ marginTop: 32 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
          <h2 style={{ fontSize: 20, color: "#f8fafc", margin: 0 }}>Gamer Analytics & Performance Hub</h2>
          <span style={{ fontSize: 12, color: "#00f2fe", fontWeight: 700 }}>
            ⚡ Live Authoritative Telemetry
          </span>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
          {/* Matches & Wins by Game Mode */}
          <div style={{ background: "#0f172a", border: "1px solid #1e293b", borderRadius: 12, padding: 18 }}>
            <h4 style={{ margin: "0 0 14px", color: "#f8fafc", fontSize: 14, fontWeight: 700 }}>
              Game Mode Engagement & Victories
            </h4>
            <div style={{ width: "100%", height: 200 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={[
                  {
                    mode: "Arena",
                    matches: Math.max(1, matches.filter(m => m.gameSlug === "arena").length),
                    wins: matches.filter(m => m.gameSlug === "arena" && m.won).length
                  },
                  {
                    mode: "Racers",
                    matches: Math.max(1, matches.filter(m => m.gameSlug === "racers").length),
                    wins: matches.filter(m => m.gameSlug === "racers" && m.won).length
                  },
                  {
                    mode: "Football",
                    matches: Math.max(1, matches.filter(m => m.gameSlug === "football").length),
                    wins: matches.filter(m => m.gameSlug === "football" && m.won).length
                  },
                  {
                    mode: "Tag",
                    matches: Math.max(1, matches.filter(m => m.gameSlug === "tag").length),
                    wins: matches.filter(m => m.gameSlug === "tag" && m.won).length
                  },
                ]}>
                  <XAxis dataKey="mode" stroke="#64748b" fontSize={11} />
                  <YAxis stroke="#64748b" fontSize={11} />
                  <Tooltip
                    contentStyle={{ background: "#0f172a", borderColor: "#334155", borderRadius: 8, fontSize: 12 }}
                  />
                  <Bar dataKey="matches" fill="#00f2fe" radius={[4, 4, 0, 0]} name="Matches Played" />
                  <Bar dataKey="wins" fill="#00ff87" radius={[4, 4, 0, 0]} name="Victories" />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div style={{ display: "flex", gap: 16, justifyContent: "center", marginTop: 8, fontSize: 11 }}>
              <span style={{ color: "#00f2fe" }}>■ Matches Played</span>
              <span style={{ color: "#00ff87" }}>■ Victories</span>
            </div>
          </div>

          {/* Session Durations & Activity */}
          <div style={{ background: "#0f172a", border: "1px solid #1e293b", borderRadius: 12, padding: 18 }}>
            <h4 style={{ margin: "0 0 14px", color: "#f8fafc", fontSize: 14, fontWeight: 700 }}>
              Session Duration Trends (Seconds)
            </h4>
            <div style={{ width: "100%", height: 200 }}>
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={
                  matches.length > 0
                    ? matches.slice(-6).map((m, idx) => ({ session: `#${idx + 1}`, duration: m.durationSec || 45 }))
                    : [
                        { session: "S1", duration: 35 },
                        { session: "S2", duration: 60 },
                        { session: "S3", duration: 45 },
                        { session: "S4", duration: 80 },
                        { session: "S5", duration: 72 },
                      ]
                }>
                  <XAxis dataKey="session" stroke="#64748b" fontSize={11} />
                  <YAxis stroke="#64748b" fontSize={11} />
                  <Tooltip
                    contentStyle={{ background: "#0f172a", borderColor: "#334155", borderRadius: 8, fontSize: 12 }}
                  />
                  <Area
                    type="monotone"
                    dataKey="duration"
                    stroke="#f59e0b"
                    fill="rgba(245, 158, 11, 0.2)"
                    strokeWidth={2}
                    name="Duration (s)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
            <div style={{ display: "flex", gap: 16, justifyContent: "center", marginTop: 8, fontSize: 11 }}>
              <span style={{ color: "#f59e0b" }}>■ Match Length Curve</span>
            </div>
          </div>
        </div>
      </div>

      {/* Friends Roster Section */}
      <div style={{ marginTop: 32 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
          <h2 style={{ fontSize: 20, color: "#f8fafc", margin: 0 }}>Friends & Teammates ({friends.length})</h2>
          <span style={{ fontSize: 12, color: "#00f2fe" }}>
            {friends.filter((f) => f.presenceStatus !== "OFFLINE").length} Online Now
          </span>
        </div>

        {friends.length === 0 ? (
          <div style={{ background: "#0f172a", border: "1px solid #1e293b", borderRadius: 12, padding: 24, textAlign: "center", color: "#64748b" }}>
            No friends connected yet. Use the <b>Friends & Chat</b> overlay at the bottom-right to add teammates!
          </div>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: 12 }}>
            {friends.map((f) => (
              <div
                key={f.friendUserId}
                style={{
                  background: "#0f172a",
                  border: "1px solid #1e293b",
                  borderRadius: 12,
                  padding: "12px 14px",
                  display: "flex",
                  alignItems: "center",
                  gap: 12,
                }}
              >
                <div style={{ position: "relative" }}>
                  <img
                    src={f.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${f.username}`}
                    alt={f.username}
                    style={{ width: 40, height: 40, borderRadius: 20, background: "#1e293b" }}
                  />
                  <span
                    style={{
                      position: "absolute",
                      bottom: 0,
                      right: 0,
                      width: 10,
                      height: 10,
                      borderRadius: 5,
                      background: f.presenceStatus === "ONLINE" ? "#00ff87" : f.presenceStatus === "IN_GAME" ? "#00f2fe" : "#64748b",
                      border: "2px solid #0f172a",
                    }}
                  />
                </div>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 700, color: "#f8fafc" }}>{f.username}</div>
                  <div style={{ fontSize: 11, color: f.presenceStatus === "ONLINE" ? "#00ff87" : f.presenceStatus === "IN_GAME" ? "#00f2fe" : "#64748b" }}>
                    {f.presenceStatus === "IN_GAME" && f.currentGameSlug ? `In ${f.currentGameSlug}` : f.presenceStatus}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Match History & Telemetry Log Section */}
      <div style={{ marginTop: 32 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
          <h2 style={{ fontSize: 20, color: "#f8fafc", margin: 0 }}>Match History & Telemetry Logs</h2>
          <span style={{ fontSize: 12, color: "#64748b" }}>
            {matches.length} Authoritative Session{matches.length !== 1 ? "s" : ""}
          </span>
        </div>

        {matches.length === 0 ? (
          <div style={{ background: "#0f172a", border: "1px solid #1e293b", borderRadius: 12, padding: 24, textAlign: "center", color: "#64748b" }}>
            No recorded matches yet. Enter the <b>Multiplayer Arena</b> to play and log telemetry sessions!
          </div>
        ) : (
          <div style={{ background: "#0f172a", border: "1px solid #1e293b", borderRadius: 12, overflow: "hidden" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", textAlign: "left", fontSize: 13 }}>
              <thead>
                <tr style={{ background: "#1e293b", color: "#94a3b8", fontSize: 11, letterSpacing: 0.8 }}>
                  <th style={{ padding: "12px 16px" }}>GAME</th>
                  <th style={{ padding: "12px 16px" }}>ROOM</th>
                  <th style={{ padding: "12px 16px" }}>RESULT</th>
                  <th style={{ padding: "12px 16px" }}>DURATION</th>
                  <th style={{ padding: "12px 16px" }}>NETWORK CONTROLLER</th>
                  <th style={{ padding: "12px 16px" }}>TIMESTAMP</th>
                  <th style={{ padding: "12px 16px" }}>LAB SESSION</th>
                </tr>
              </thead>
              <tbody>
                {matches.map((m) => (
                  <tr key={m.sessionId} style={{ borderBottom: "1px solid #1e293b" }}>
                    <td style={{ padding: "12px 16px", color: "#f8fafc", fontWeight: 700, textTransform: "capitalize" }}>
                      {m.gameSlug}
                    </td>
                    <td style={{ padding: "12px 16px", color: "#00f2fe", fontFamily: "monospace", fontWeight: 700 }}>
                      {m.roomCode}
                    </td>
                    <td style={{ padding: "12px 16px" }}>
                      <span style={{
                        padding: "2px 8px",
                        borderRadius: 4,
                        fontSize: 11,
                        fontWeight: 700,
                        background: m.won ? "rgba(0,255,135,0.15)" : "rgba(100,116,139,0.15)",
                        color: m.won ? "#00ff87" : "#94a3b8",
                        border: m.won ? "1px solid rgba(0,255,135,0.3)" : "1px solid rgba(100,116,139,0.3)",
                      }}>
                        {m.won ? "🏆 VICTORY" : "DEFEAT"}
                      </span>
                    </td>
                    <td style={{ padding: "12px 16px", color: "#94a3b8" }}>
                      {m.durationSec != null ? `${m.durationSec}s` : "—"}
                    </td>
                    <td style={{ padding: "12px 16px" }}>
                      <span style={{
                        padding: "2px 8px",
                        borderRadius: 4,
                        fontSize: 11,
                        fontWeight: 700,
                        background: m.controllerMode === "PROPOSED" ? "rgba(0,242,254,0.15)" : "rgba(245,158,11,0.15)",
                        color: m.controllerMode === "PROPOSED" ? "#00f2fe" : "#f59e0b",
                      }}>
                        {m.controllerMode}
                      </span>
                    </td>
                    <td style={{ padding: "12px 16px", color: "#64748b", fontSize: 12 }}>
                      {new Date(m.startedAt).toLocaleString(undefined, {
                        month: "short", day: "numeric", hour: "2-digit", minute: "2-digit"
                      })}
                    </td>
                    <td style={{ padding: "12px 16px" }}>
                      <Link
                        to={`/experiments?session=${m.sessionId}`}
                        style={{ color: "#00f2fe", fontSize: 12, textDecoration: "none" }}
                      >
                        Session #{m.sessionId} →
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Quick Navigation Cards */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginTop: 24 }}>
        <Link to="/library" style={navCardStyle}>
          <h3 style={{ margin: 0, color: "#00f2fe", fontSize: 16 }}>Manage Your Library →</h3>
          <p style={{ margin: "6px 0 0", color: "#94a3b8", fontSize: 13 }}>
            Organize currently playing, completed, and backlog games.
          </p>
        </Link>
        <Link to="/wishlist" style={navCardStyle}>
          <h3 style={{ margin: 0, color: "#facc15", fontSize: 16 }}>View Saved Wishlist →</h3>
          <p style={{ margin: "6px 0 0", color: "#94a3b8", fontSize: 13 }}>
            Inspect bookmarked games and jump straight into live matches.
          </p>
        </Link>
        <Link to="/achievements" style={navCardStyle}>
          <h3 style={{ margin: 0, color: "#00ff87", fontSize: 16 }}>Achievements →</h3>
          <p style={{ margin: "6px 0 0", color: "#94a3b8", fontSize: 13 }}>
            Track milestones, unlock XP rewards, and showcase your progress.
          </p>
        </Link>
        <Link to="/leaderboard" style={navCardStyle}>
          <h3 style={{ margin: 0, color: "#f59e0b", fontSize: 16 }}>Leaderboard →</h3>
          <p style={{ margin: "6px 0 0", color: "#94a3b8", fontSize: 13 }}>
            Check Season 1 rankings across Arena, Racers, Football, and Tag.
          </p>
        </Link>
        <Link to="/groups" style={navCardStyle}>
          <h3 style={{ margin: 0, color: "#c084fc", fontSize: 16 }}>Syndicates & Clans →</h3>
          <p style={{ margin: "6px 0 0", color: "#94a3b8", fontSize: 13 }}>
            Enlist with a clan squadron or establish your own elite combat syndicate.
          </p>
        </Link>
      </div>
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  maxWidth: 960,
  margin: "0 auto",
  padding: "32px 20px",
  fontFamily: "system-ui, -apple-system, sans-serif",
};

const headerCardStyle: React.CSSProperties = {
  background: "#0f172a",
  borderRadius: 16,
  padding: 24,
  border: "1px solid #1e293b",
};

const roleBadgeStyle: React.CSSProperties = {
  padding: "2px 8px",
  borderRadius: 4,
  fontSize: 11,
  fontWeight: 700,
  color: "#00f2fe",
  background: "rgba(0, 242, 254, 0.12)",
  border: "1px solid rgba(0, 242, 254, 0.3)",
};

const editBtnStyle: React.CSSProperties = {
  padding: "8px 16px",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 8,
  color: "#f8fafc",
  fontSize: 13,
  cursor: "pointer",
  fontWeight: 600,
};

const editFormStyle: React.CSSProperties = {
  marginTop: 20,
  paddingTop: 16,
  borderTop: "1px solid #1e293b",
};

const labelStyle: React.CSSProperties = {
  display: "block",
  fontSize: 12,
  color: "#94a3b8",
  marginBottom: 4,
};

const inputStyle: React.CSSProperties = {
  width: "100%",
  boxSizing: "border-box",
  padding: "8px 12px",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 6,
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
};

const saveBtnStyle: React.CSSProperties = {
  marginTop: 12,
  padding: "8px 20px",
  background: "#00f2fe",
  color: "#0f172a",
  fontWeight: 700,
  fontSize: 13,
  borderRadius: 6,
  border: "none",
  cursor: "pointer",
};

const statsGridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
  gap: 16,
};

const statCardStyle: React.CSSProperties = {
  background: "#0f172a",
  borderRadius: 12,
  padding: 16,
  border: "1px solid #1e293b",
  textAlign: "center",
};

const statLabelStyle: React.CSSProperties = {
  fontSize: 11,
  color: "#64748b",
  fontWeight: 700,
  letterSpacing: 1,
};

const statValStyle: React.CSSProperties = {
  fontSize: 28,
  fontWeight: 800,
  color: "#f8fafc",
  margin: "6px 0",
};

const statSubStyle: React.CSSProperties = {
  fontSize: 11,
  color: "#94a3b8",
};

const navCardStyle: React.CSSProperties = {
  background: "#0f172a",
  borderRadius: 12,
  padding: 18,
  border: "1px solid #1e293b",
  textDecoration: "none",
  display: "block",
};
