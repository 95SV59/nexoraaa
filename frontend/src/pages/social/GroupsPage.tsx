import { useEffect, useState } from "react";
import { groupService } from "../../services/groupService";
import { useAuth } from "../../context/AuthContext";
import type { GroupDto } from "../../types/group";

export function GroupsPage() {
  const { user } = useAuth();
  const [allGroups, setAllGroups] = useState<GroupDto[]>([]);
  const [myGroups, setMyGroups] = useState<GroupDto[]>([]);
  const [activeTab, setActiveTab] = useState<"browse" | "my">("browse");
  const [loading, setLoading] = useState(true);
  const [selectedGroup, setSelectedGroup] = useState<GroupDto | null>(null);

  // Create form state
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState("");
  const [tag, setTag] = useState("");
  const [description, setDescription] = useState("");
  const [avatarUrl, setAvatarUrl] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [createLoading, setCreateLoading] = useState(false);

  useEffect(() => {
    loadGroups();
  }, []);

  async function loadGroups() {
    setLoading(true);
    try {
      const [all, my] = await Promise.all([
        groupService.getAllGroups().catch(() => []),
        groupService.getMyGroups().catch(() => []),
      ]);
      setAllGroups(all);
      setMyGroups(my);
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setFormError(null);
    setCreateLoading(true);
    try {
      const created = await groupService.createGroup({
        name,
        tag: tag.trim().toUpperCase(),
        description,
        avatarUrl,
      });
      setShowCreate(false);
      setName("");
      setTag("");
      setDescription("");
      setAvatarUrl("");
      await loadGroups();
      setSelectedGroup(created);
    } catch (err: any) {
      setFormError(err.response?.data?.message || "Failed to create clan. Name or tag may already be taken.");
    } finally {
      setCreateLoading(false);
    }
  }

  async function handleJoin(groupId: number) {
    try {
      await groupService.joinGroup(groupId);
      await loadGroups();
      if (selectedGroup?.id === groupId) {
        const refreshed = await groupService.getGroup(groupId);
        setSelectedGroup(refreshed);
      }
    } catch (err: any) {
      alert(err.response?.data?.message || "Could not join clan.");
    }
  }

  async function handleLeave(groupId: number) {
    if (!confirm("Are you sure you want to leave this clan?")) return;
    try {
      await groupService.leaveGroup(groupId);
      await loadGroups();
      if (selectedGroup?.id === groupId) {
        setSelectedGroup(null);
      }
    } catch (err: any) {
      alert(err.response?.data?.message || "Could not leave clan.");
    }
  }

  async function viewDetails(groupId: number) {
    try {
      const g = await groupService.getGroup(groupId);
      setSelectedGroup(g);
    } catch {
      // Ignore
    }
  }

  const displayedGroups = activeTab === "browse" ? allGroups : myGroups;

  return (
    <div style={containerStyle}>
      {/* Header */}
      <div style={headerStyle}>
        <div>
          <span style={badgeStyle}>COMMUNITY GUILDS & CLANS</span>
          <h1 style={titleStyle}>Syndicates & Squadrons</h1>
          <p style={subtitleStyle}>
            Band together with fellow operatives. Compete as a clan in multiplayer matchmaking and show off your clan tag.
          </p>
        </div>
        <button style={createBtnStyle} onClick={() => setShowCreate(!showCreate)}>
          {showCreate ? "✕ Close Form" : "＋ Create Clan"}
        </button>
      </div>

      {/* Create Clan Modal/Card */}
      {showCreate && (
        <form onSubmit={handleCreate} style={createCardStyle}>
          <h3 style={{ margin: "0 0 16px", color: "#00f2fe", fontSize: 18 }}>
            Found a New Gaming Clan
          </h3>
          <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 14 }}>
            <div>
              <label style={labelStyle}>Clan / Syndicate Name *</label>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Cyber Vanguard"
                required
                style={inputStyle}
              />
            </div>
            <div>
              <label style={labelStyle}>Clan Tag (2-6 Chars) *</label>
              <input
                value={tag}
                onChange={(e) => setTag(e.target.value.toUpperCase())}
                placeholder="e.g. CVG"
                maxLength={6}
                required
                style={{ ...inputStyle, textTransform: "uppercase", fontWeight: 700 }}
              />
            </div>
          </div>

          <div style={{ marginTop: 12 }}>
            <label style={labelStyle}>Mission Description / Lore</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What is your clan's combat philosophy, competitive goals, or schedule?"
              rows={3}
              style={{ ...inputStyle, resize: "vertical" }}
            />
          </div>

          <div style={{ marginTop: 12 }}>
            <label style={labelStyle}>Custom Clan Emblem / Avatar URL (Optional)</label>
            <input
              value={avatarUrl}
              onChange={(e) => setAvatarUrl(e.target.value)}
              placeholder="https://..."
              style={inputStyle}
            />
          </div>

          {formError && <p style={{ color: "#ef4444", fontSize: 13, margin: "10px 0 0" }}>{formError}</p>}

          <button type="submit" style={submitBtnStyle} disabled={createLoading}>
            {createLoading ? "Forging Clan..." : "Establish Clan Syndicate"}
          </button>
        </form>
      )}

      {/* Tabs */}
      <div style={tabContainerStyle}>
        <button
          onClick={() => setActiveTab("browse")}
          style={{
            ...tabButtonStyle,
            borderBottom: activeTab === "browse" ? "2px solid #00f2fe" : "none",
            color: activeTab === "browse" ? "#00f2fe" : "#94a3b8",
          }}
        >
          Explore All Clans ({allGroups.length})
        </button>
        <button
          onClick={() => setActiveTab("my")}
          style={{
            ...tabButtonStyle,
            borderBottom: activeTab === "my" ? "2px solid #00ff87" : "none",
            color: activeTab === "my" ? "#00ff87" : "#94a3b8",
          }}
        >
          My Enlistments ({myGroups.length})
        </button>
      </div>

      {/* Group List & Details View */}
      {loading ? (
        <p style={{ color: "#64748b" }}>Loading clan syndicates...</p>
      ) : displayedGroups.length === 0 ? (
        <div style={emptyCardStyle}>
          <p style={{ fontSize: 16, color: "#94a3b8", margin: 0 }}>
            {activeTab === "browse"
              ? "No clans established yet. Be the first commander to establish a syndicate!"
              : "You have not enlisted in any clans yet. Browse available clans above or forge your own!"}
          </p>
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: selectedGroup ? "1fr 1fr" : "1fr", gap: 20 }}>
          {/* List of Clans */}
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {displayedGroups.map((g) => {
              const isSelected = selectedGroup?.id === g.id;
              return (
                <div
                  key={g.id}
                  style={{
                    ...clanCardStyle,
                    borderColor: isSelected ? "#00f2fe" : "#1e293b",
                    boxShadow: isSelected ? "0 0 15px rgba(0,242,254,0.2)" : "none",
                  }}
                >
                  <div style={{ display: "flex", gap: 14, alignItems: "center" }}>
                    <img
                      src={g.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${g.name}`}
                      alt={g.name}
                      style={avatarStyle}
                    />
                    <div style={{ flex: 1 }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                        <span style={tagBadgeStyle}>[{g.tag}]</span>
                        <h3 style={{ margin: 0, fontSize: 16, color: "#f8fafc" }}>{g.name}</h3>
                        {g.myRole && (
                          <span style={roleBadgeStyle}>{g.myRole}</span>
                        )}
                      </div>
                      <p style={{ margin: "4px 0 0", color: "#94a3b8", fontSize: 13, lineHeight: 1.4 }}>
                        {g.description || "No mission brief provided."}
                      </p>
                      <div style={{ fontSize: 11, color: "#64748b", marginTop: 6 }}>
                        Commander: <strong style={{ color: "#e2e8f0" }}>{g.leaderUsername}</strong> · {g.memberCount} Operative{g.memberCount !== 1 ? "s" : ""}
                      </div>
                    </div>
                  </div>

                  <div style={{ display: "flex", gap: 10, marginTop: 12, borderTop: "1px solid #1e293b", paddingTop: 10 }}>
                    <button style={actionBtnStyle} onClick={() => viewDetails(g.id)}>
                      👥 Inspect Roster
                    </button>
                    {!g.isMember ? (
                      <button style={joinBtnStyle} onClick={() => handleJoin(g.id)}>
                        ⚔ Join Clan
                      </button>
                    ) : g.leaderUserId !== user?.userId ? (
                      <button style={leaveBtnStyle} onClick={() => handleLeave(g.id)}>
                        Leave
                      </button>
                    ) : (
                      <span style={{ fontSize: 11, color: "#00ff87", alignSelf: "center", fontWeight: 700 }}>
                        ★ Clan Leader
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Selected Clan Roster / Inspector Sidebar */}
          {selectedGroup && (
            <div style={detailsSidebarStyle}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <span style={tagBadgeStyle}>[{selectedGroup.tag}]</span>
                  <h2 style={{ margin: 0, fontSize: 18, color: "#f8fafc" }}>{selectedGroup.name}</h2>
                </div>
                <button
                  onClick={() => setSelectedGroup(null)}
                  style={{ background: "transparent", border: "none", color: "#94a3b8", fontSize: 18, cursor: "pointer" }}
                >
                  ✕
                </button>
              </div>

              <p style={{ color: "#cbd5e1", fontSize: 13, lineHeight: 1.5, marginBottom: 16 }}>
                {selectedGroup.description || "Official NEXORA multiplayer syndicate."}
              </p>

              <h4 style={{ margin: "0 0 10px", color: "#00f2fe", fontSize: 13, letterSpacing: 1 }}>
                OPERATIVE ROSTER ({selectedGroup.members?.length || selectedGroup.memberCount})
              </h4>

              <div style={{ display: "flex", flexDirection: "column", gap: 8, maxHeight: 380, overflowY: "auto" }}>
                {selectedGroup.members?.map((m) => (
                  <div key={m.userId} style={memberItemStyle}>
                    <img
                      src={m.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${m.username}`}
                      alt={m.username}
                      style={{ width: 32, height: 32, borderRadius: 16, background: "#1e293b" }}
                    />
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: 13, fontWeight: 700, color: "#f8fafc" }}>
                        {m.username}
                        {m.userId === user?.userId && <span style={{ color: "#64748b", fontWeight: 400 }}> (you)</span>}
                      </div>
                      <div style={{ fontSize: 10, color: "#64748b" }}>
                        Enlisted: {new Date(m.joinedAt).toLocaleDateString()}
                      </div>
                    </div>
                    <span style={{
                      ...roleBadgeStyle,
                      background: m.role === "LEADER" ? "rgba(245,158,11,0.15)" : "rgba(0,242,254,0.1)",
                      color: m.role === "LEADER" ? "#f59e0b" : "#00f2fe",
                      border: m.role === "LEADER" ? "1px solid rgba(245,158,11,0.3)" : "1px solid rgba(0,242,254,0.2)",
                    }}>
                      {m.role}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  maxWidth: 1040,
  margin: "0 auto",
  padding: "32px 20px",
  fontFamily: "system-ui, -apple-system, sans-serif",
};

const headerStyle: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "flex-start",
  marginBottom: 24,
  gap: 16,
  flexWrap: "wrap",
};

const badgeStyle: React.CSSProperties = {
  fontSize: 11,
  fontWeight: 700,
  letterSpacing: 1.5,
  color: "#00f2fe",
  background: "rgba(0,242,254,0.1)",
  border: "1px solid rgba(0,242,254,0.3)",
  padding: "3px 10px",
  borderRadius: 4,
};

const titleStyle: React.CSSProperties = {
  margin: "8px 0 4px",
  fontSize: 28,
  fontWeight: 800,
  color: "#f8fafc",
};

const subtitleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 14,
  color: "#94a3b8",
  maxWidth: 600,
};

const createBtnStyle: React.CSSProperties = {
  padding: "10px 18px",
  background: "linear-gradient(135deg, #00f2fe 0%, #00ff87 100%)",
  color: "#070c17",
  fontWeight: 800,
  fontSize: 13,
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
};

const tabContainerStyle: React.CSSProperties = {
  display: "flex",
  gap: 20,
  borderBottom: "1px solid #1e293b",
  marginBottom: 20,
};

const tabButtonStyle: React.CSSProperties = {
  padding: "10px 4px",
  background: "transparent",
  border: "none",
  fontSize: 14,
  fontWeight: 700,
  cursor: "pointer",
};

const createCardStyle: React.CSSProperties = {
  background: "#0f172a",
  border: "1px solid #00f2fe",
  boxShadow: "0 0 25px rgba(0,242,254,0.15)",
  borderRadius: 14,
  padding: 22,
  marginBottom: 24,
};

const labelStyle: React.CSSProperties = {
  display: "block",
  fontSize: 11,
  color: "#94a3b8",
  marginBottom: 5,
  fontWeight: 600,
};

const inputStyle: React.CSSProperties = {
  width: "100%",
  boxSizing: "border-box",
  padding: "9px 12px",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 6,
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
};

const submitBtnStyle: React.CSSProperties = {
  marginTop: 16,
  padding: "10px 22px",
  background: "#00f2fe",
  color: "#070c17",
  fontWeight: 800,
  fontSize: 13,
  border: "none",
  borderRadius: 6,
  cursor: "pointer",
};

const clanCardStyle: React.CSSProperties = {
  background: "#0f172a",
  border: "1px solid #1e293b",
  borderRadius: 12,
  padding: 16,
  transition: "all 0.2s ease",
};

const avatarStyle: React.CSSProperties = {
  width: 52,
  height: 52,
  borderRadius: 26,
  border: "2px solid #334155",
  background: "#1e293b",
};

const tagBadgeStyle: React.CSSProperties = {
  fontSize: 11,
  fontWeight: 800,
  color: "#facc15",
  background: "rgba(250, 204, 21, 0.12)",
  border: "1px solid rgba(250, 204, 21, 0.3)",
  padding: "2px 6px",
  borderRadius: 4,
  fontFamily: "monospace",
};

const roleBadgeStyle: React.CSSProperties = {
  fontSize: 10,
  fontWeight: 700,
  color: "#00ff87",
  background: "rgba(0,255,135,0.12)",
  border: "1px solid rgba(0,255,135,0.3)",
  padding: "2px 6px",
  borderRadius: 4,
};

const actionBtnStyle: React.CSSProperties = {
  padding: "6px 12px",
  background: "#1e293b",
  border: "1px solid #334155",
  color: "#f8fafc",
  borderRadius: 6,
  fontSize: 12,
  cursor: "pointer",
  fontWeight: 600,
};

const joinBtnStyle: React.CSSProperties = {
  padding: "6px 14px",
  background: "rgba(0, 242, 254, 0.15)",
  border: "1px solid rgba(0, 242, 254, 0.4)",
  color: "#00f2fe",
  borderRadius: 6,
  fontSize: 12,
  cursor: "pointer",
  fontWeight: 700,
};

const leaveBtnStyle: React.CSSProperties = {
  padding: "6px 12px",
  background: "transparent",
  border: "1px solid #dc2626",
  color: "#ef4444",
  borderRadius: 6,
  fontSize: 12,
  cursor: "pointer",
};

const detailsSidebarStyle: React.CSSProperties = {
  background: "#0f172a",
  border: "1px solid #1e293b",
  borderRadius: 14,
  padding: 20,
};

const memberItemStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 10,
  padding: "8px 12px",
  background: "#1e293b",
  borderRadius: 8,
};

const emptyCardStyle: React.CSSProperties = {
  background: "#0f172a",
  border: "1px solid #1e293b",
  borderRadius: 12,
  padding: 32,
  textAlign: "center",
};
