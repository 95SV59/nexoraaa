import { useEffect, useState } from "react";
import { adminService } from "../../services/adminService";
import { articleService } from "../../services/articleService";
import { AdminStatsDto, CreateGameRequest, UserManagementDto } from "../../types/admin";
import { ReviewDto } from "../../types/review";
import { useAuth } from "../../context/AuthContext";
import { Navigate } from "react-router-dom";

export function AdminDashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState<AdminStatsDto | null>(null);
  const [users, setUsers] = useState<UserManagementDto[]>([]);
  const [reportedReviews, setReportedReviews] = useState<ReviewDto[]>([]);
  const [activeTab, setActiveTab] = useState<"users" | "reviews" | "add-game" | "add-article">("users");

  // Add Game form
  const [gameSlug, setGameSlug] = useState("");
  const [gameTitle, setGameTitle] = useState("");
  const [gameDesc, setGameDesc] = useState("");
  const [genreName, setGenreName] = useState("Action");
  const [screenshotUrl, setScreenshotUrl] = useState("");

  // Add Article form
  const [articleTitle, setArticleTitle] = useState("");
  const [articleBody, setArticleBody] = useState("");
  const [articleCategory, setArticleCategory] = useState("Platform Updates");

  const [message, setMessage] = useState<string | null>(null);

  if (user?.role !== "ADMIN") {
    return <Navigate to="/" replace />;
  }

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    try {
      const [s, u, r] = await Promise.all([
        adminService.getStats(),
        adminService.listUsers(),
        adminService.getReportedReviews(),
      ]);
      setStats(s);
      setUsers(u);
      setReportedReviews(r);
    } catch (e) {
      console.error(e);
    }
  }

  async function handleToggleStatus(userId: number) {
    try {
      await adminService.toggleUserStatus(userId);
      await loadData();
    } catch (e) {
      console.error(e);
    }
  }

  async function handleChangeRole(userId: number, currentRole: string) {
    const newRole = currentRole === "ADMIN" ? "PLAYER" : "ADMIN";
    try {
      await adminService.changeUserRole(userId, newRole);
      await loadData();
    } catch (e) {
      console.error(e);
    }
  }

  async function handleDeleteReview(reviewId: number) {
    if (!confirm("Delete this reported review?")) return;
    try {
      await adminService.getReportedReviews();
      setReportedReviews((prev) => prev.filter((r) => r.id !== reviewId));
    } catch (e) {
      console.error(e);
    }
  }

  async function handleCreateGame(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);
    try {
      const req: CreateGameRequest = {
        slug: gameSlug.trim().toLowerCase(),
        title: gameTitle.trim(),
        description: gameDesc.trim(),
        genreName,
        screenshotUrl,
        multiplayer: true,
      };
      await adminService.createGame(req);
      setMessage("Game added to catalogue successfully!");
      setGameSlug("");
      setGameTitle("");
      setGameDesc("");
      setScreenshotUrl("");
      await loadData();
    } catch {
      setMessage("Failed to add game. Check if slug already exists.");
    }
  }

  async function handleCreateArticle(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);
    try {
      await articleService.createArticle({
        title: articleTitle.trim(),
        body: articleBody.trim(),
        categoryName: articleCategory,
        published: true,
      });
      setMessage("Article published successfully!");
      setArticleTitle("");
      setArticleBody("");
      await loadData();
    } catch {
      setMessage("Failed to publish article.");
    }
  }

  return (
    <div style={containerStyle}>
      <div style={{ marginBottom: 28 }}>
        <span style={badgeStyle}>ADMIN CONTROL</span>
        <h1 style={{ margin: "4px 0 0", color: "#f8fafc", fontSize: 28 }}>System & Platform Administration</h1>
        <p style={{ color: "#94a3b8", fontSize: 14, margin: "6px 0 0" }}>
          Full administrative authority over accounts, games, community content, and networking sessions.
        </p>
      </div>

      {/* Stats Summary Bar */}
      {stats && (
        <div style={statsGridStyle}>
          <div style={statBoxStyle}>
            <div style={statLabelStyle}>TOTAL USERS</div>
            <div style={statNumStyle}>{stats.totalUsers}</div>
            <div style={statSubStyle}>{stats.activeUsers} active</div>
          </div>
          <div style={statBoxStyle}>
            <div style={statLabelStyle}>TOTAL GAMES</div>
            <div style={statNumStyle}>{stats.totalGames}</div>
            <div style={statSubStyle}>Catalogue rows</div>
          </div>
          <div style={statBoxStyle}>
            <div style={statLabelStyle}>REPORTED REVIEWS</div>
            <div style={{ ...statNumStyle, color: stats.reportedReviewsCount > 0 ? "#ef4444" : "#00ff87" }}>
              {stats.reportedReviewsCount}
            </div>
            <div style={statSubStyle}>Pending review</div>
          </div>
          <div style={statBoxStyle}>
            <div style={statLabelStyle}>ARTICLES</div>
            <div style={statNumStyle}>{stats.totalArticles}</div>
            <div style={statSubStyle}>Dispatches published</div>
          </div>
          <div style={statBoxStyle}>
            <div style={statLabelStyle}>TOTAL SESSIONS</div>
            <div style={statNumStyle}>{stats.totalSessions}</div>
            <div style={statSubStyle}>{stats.activeRooms} active rooms</div>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div style={tabsBarStyle}>
        <button
          onClick={() => { setActiveTab("users"); setMessage(null); }}
          style={activeTab === "users" ? activeTabBtn : inactiveTabBtn}
        >
          Users Management ({users.length})
        </button>
        <button
          onClick={() => { setActiveTab("reviews"); setMessage(null); }}
          style={activeTab === "reviews" ? activeTabBtn : inactiveTabBtn}
        >
          Reported Reviews ({reportedReviews.length})
        </button>
        <button
          onClick={() => { setActiveTab("add-game"); setMessage(null); }}
          style={activeTab === "add-game" ? activeTabBtn : inactiveTabBtn}
        >
          + Add Catalogue Game
        </button>
        <button
          onClick={() => { setActiveTab("add-article"); setMessage(null); }}
          style={activeTab === "add-article" ? activeTabBtn : inactiveTabBtn}
        >
          + Publish News Dispatch
        </button>
      </div>

      {message && <div style={msgBoxStyle}>{message}</div>}

      {/* Tab: Users Management */}
      {activeTab === "users" && (
        <div style={tableContainerStyle}>
          <table style={tableStyle}>
            <thead>
              <tr style={thRowStyle}>
                <th style={thStyle}>ID</th>
                <th style={thStyle}>Username</th>
                <th style={thStyle}>Email</th>
                <th style={thStyle}>Role</th>
                <th style={thStyle}>Status</th>
                <th style={thStyle}>Registered</th>
                <th style={thStyle}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} style={trStyle}>
                  <td style={tdStyle}>{u.id}</td>
                  <td style={{ ...tdStyle, fontWeight: 600, color: "#f8fafc" }}>{u.username}</td>
                  <td style={tdStyle}>{u.email}</td>
                  <td style={tdStyle}>
                    <span style={u.role === "ADMIN" ? adminRoleBadge : playerRoleBadge}>{u.role}</span>
                  </td>
                  <td style={tdStyle}>
                    <span style={{ color: u.status === "ACTIVE" ? "#00ff87" : "#ef4444" }}>● {u.status}</span>
                  </td>
                  <td style={tdStyle}>{new Date(u.createdAt).toLocaleDateString()}</td>
                  <td style={tdStyle}>
                    <div style={{ display: "flex", gap: 8 }}>
                      <button onClick={() => handleToggleStatus(u.id)} style={actionBtn}>
                        {u.status === "ACTIVE" ? "Suspend" : "Activate"}
                      </button>
                      <button onClick={() => handleChangeRole(u.id, u.role)} style={actionBtn}>
                        Make {u.role === "ADMIN" ? "Player" : "Admin"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Tab: Reported Reviews */}
      {activeTab === "reviews" && (
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          {reportedReviews.length === 0 ? (
            <p style={{ color: "#64748b" }}>No reported reviews currently pending moderation.</p>
          ) : (
            reportedReviews.map((r) => (
              <div key={r.id} style={reportedCardStyle}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <div>
                    <b style={{ color: "#f8fafc" }}>{r.username}</b> on <span style={{ color: "#00f2fe" }}>{r.gameTitle}</span>
                    <span style={{ color: "#facc15", marginLeft: 8 }}>{"★".repeat(r.rating)}</span>
                  </div>
                  <button onClick={() => handleDeleteReview(r.id)} style={deleteBtnStyle}>
                    Remove Review
                  </button>
                </div>
                <p style={{ color: "#cbd5e1", fontSize: 13, margin: "8px 0 0" }}>{r.body}</p>
              </div>
            ))
          )}
        </div>
      )}

      {/* Tab: Add Game */}
      {activeTab === "add-game" && (
        <form onSubmit={handleCreateGame} style={formCardStyle}>
          <h3 style={{ color: "#f8fafc", margin: "0 0 16px" }}>Add New Title to Catalogue</h3>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
            <div>
              <label style={labelStyle}>Game Slug (unique identifier, e.g. "blast")</label>
              <input value={gameSlug} onChange={(e) => setGameSlug(e.target.value)} style={inputStyle} required />
            </div>
            <div>
              <label style={labelStyle}>Game Title</label>
              <input value={gameTitle} onChange={(e) => setGameTitle(e.target.value)} style={inputStyle} required />
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginTop: 14 }}>
            <div>
              <label style={labelStyle}>Genre</label>
              <select value={genreName} onChange={(e) => setGenreName(e.target.value)} style={inputStyle}>
                <option value="Action">Action</option>
                <option value="Racing">Racing</option>
                <option value="Sports">Sports</option>
                <option value="Strategy">Strategy</option>
                <option value="Party">Party</option>
                <option value="Survival">Survival</option>
              </select>
            </div>
            <div>
              <label style={labelStyle}>Screenshot URL</label>
              <input value={screenshotUrl} onChange={(e) => setScreenshotUrl(e.target.value)} style={inputStyle} placeholder="https://..." />
            </div>
          </div>

          <div style={{ marginTop: 14 }}>
            <label style={labelStyle}>Description</label>
            <textarea value={gameDesc} onChange={(e) => setGameDesc(e.target.value)} rows={4} style={textareaStyle} required />
          </div>

          <button type="submit" style={submitBtnStyle}>Add Game to Catalogue</button>
        </form>
      )}

      {/* Tab: Add Article */}
      {activeTab === "add-article" && (
        <form onSubmit={handleCreateArticle} style={formCardStyle}>
          <h3 style={{ color: "#f8fafc", margin: "0 0 16px" }}>Publish Gaming News / Tech Dispatch</h3>
          <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 16 }}>
            <div>
              <label style={labelStyle}>Article Title</label>
              <input value={articleTitle} onChange={(e) => setArticleTitle(e.target.value)} style={inputStyle} required />
            </div>
            <div>
              <label style={labelStyle}>Category</label>
              <select value={articleCategory} onChange={(e) => setArticleCategory(e.target.value)} style={inputStyle}>
                <option value="Platform Updates">Platform Updates</option>
                <option value="Technology">Technology</option>
                <option value="Esports">Esports</option>
                <option value="Hardware">Hardware</option>
              </select>
            </div>
          </div>

          <div style={{ marginTop: 14 }}>
            <label style={labelStyle}>Article Body</label>
            <textarea value={articleBody} onChange={(e) => setArticleBody(e.target.value)} rows={6} style={textareaStyle} required />
          </div>

          <button type="submit" style={submitBtnStyle}>Publish Article Now</button>
        </form>
      )}
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  maxWidth: 1100,
  margin: "0 auto",
  padding: "32px 20px",
  fontFamily: "system-ui, -apple-system, sans-serif",
};

const badgeStyle: React.CSSProperties = {
  display: "inline-block",
  padding: "3px 10px",
  borderRadius: 16,
  fontSize: 11,
  fontWeight: 700,
  letterSpacing: 1.5,
  color: "#ec4899",
  background: "rgba(236, 72, 153, 0.1)",
  border: "1px solid rgba(236, 72, 153, 0.25)",
};

const statsGridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))",
  gap: 16,
  marginBottom: 28,
};

const statBoxStyle: React.CSSProperties = {
  background: "#0f172a",
  borderRadius: 12,
  padding: 16,
  border: "1px solid #1e293b",
};

const statLabelStyle: React.CSSProperties = {
  fontSize: 11,
  color: "#64748b",
  fontWeight: 700,
  letterSpacing: 1,
};

const statNumStyle: React.CSSProperties = {
  fontSize: 26,
  fontWeight: 800,
  color: "#f8fafc",
  margin: "4px 0",
};

const statSubStyle: React.CSSProperties = {
  fontSize: 11,
  color: "#94a3b8",
};

const tabsBarStyle: React.CSSProperties = {
  display: "flex",
  gap: 8,
  flexWrap: "wrap",
  borderBottom: "1px solid #1e293b",
  paddingBottom: 12,
  marginBottom: 20,
};

const activeTabBtn: React.CSSProperties = {
  padding: "8px 16px",
  borderRadius: 8,
  border: "1px solid #00f2fe",
  background: "rgba(0, 242, 254, 0.12)",
  color: "#00f2fe",
  fontWeight: 600,
  fontSize: 13,
  cursor: "pointer",
};

const inactiveTabBtn: React.CSSProperties = {
  padding: "8px 16px",
  borderRadius: 8,
  border: "1px solid #1e293b",
  background: "transparent",
  color: "#94a3b8",
  fontWeight: 600,
  fontSize: 13,
  cursor: "pointer",
};

const tableContainerStyle: React.CSSProperties = {
  overflowX: "auto",
  background: "#0f172a",
  borderRadius: 12,
  border: "1px solid #1e293b",
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  fontSize: 13,
};

const thRowStyle: React.CSSProperties = {
  background: "#1e293b",
  textAlign: "left",
};

const thStyle: React.CSSProperties = {
  padding: "12px 16px",
  color: "#94a3b8",
  fontWeight: 600,
  fontSize: 12,
};

const trStyle: React.CSSProperties = {
  borderBottom: "1px solid #1e293b",
};

const tdStyle: React.CSSProperties = {
  padding: "12px 16px",
  color: "#cbd5e1",
};

const adminRoleBadge: React.CSSProperties = {
  padding: "2px 8px",
  borderRadius: 4,
  background: "rgba(236, 72, 153, 0.15)",
  color: "#ec4899",
  fontSize: 11,
  fontWeight: 700,
};

const playerRoleBadge: React.CSSProperties = {
  padding: "2px 8px",
  borderRadius: 4,
  background: "rgba(56, 189, 248, 0.15)",
  color: "#38bdf8",
  fontSize: 11,
  fontWeight: 700,
};

const actionBtn: React.CSSProperties = {
  padding: "4px 10px",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 6,
  color: "#f8fafc",
  fontSize: 12,
  cursor: "pointer",
};

const reportedCardStyle: React.CSSProperties = {
  background: "#0f172a",
  padding: 16,
  borderRadius: 10,
  border: "1px solid #ef444444",
};

const deleteBtnStyle: React.CSSProperties = {
  padding: "4px 10px",
  background: "#ef4444",
  border: "none",
  borderRadius: 6,
  color: "white",
  fontSize: 12,
  cursor: "pointer",
  fontWeight: 600,
};

const formCardStyle: React.CSSProperties = {
  background: "#0f172a",
  padding: 24,
  borderRadius: 12,
  border: "1px solid #1e293b",
};

const labelStyle: React.CSSProperties = {
  display: "block",
  fontSize: 12,
  color: "#94a3b8",
  marginBottom: 6,
  fontWeight: 600,
};

const inputStyle: React.CSSProperties = {
  width: "100%",
  boxSizing: "border-box",
  padding: "10px 12px",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 8,
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
};

const textareaStyle: React.CSSProperties = {
  width: "100%",
  boxSizing: "border-box",
  padding: "10px 12px",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 8,
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
  resize: "vertical",
};

const submitBtnStyle: React.CSSProperties = {
  marginTop: 18,
  padding: "10px 24px",
  background: "#00f2fe",
  color: "#0f172a",
  fontWeight: 700,
  fontSize: 13,
  borderRadius: 8,
  border: "none",
  cursor: "pointer",
};

const msgBoxStyle: React.CSSProperties = {
  padding: "10px 16px",
  background: "rgba(0, 255, 135, 0.1)",
  border: "1px solid rgba(0, 255, 135, 0.25)",
  color: "#00ff87",
  borderRadius: 8,
  marginBottom: 16,
  fontSize: 13,
};
