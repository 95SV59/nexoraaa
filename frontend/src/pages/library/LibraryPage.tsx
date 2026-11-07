import { useEffect, useState, useMemo } from "react";
import { Link } from "react-router-dom";
import { libraryService } from "../../services/libraryService";
import { LibraryItemDto, LibraryStatus } from "../../types/library";
import { PLAYABLE_SLUGS } from "../../types/multiplayer";

const PLAYABLE_SET = new Set<string>(PLAYABLE_SLUGS);

type ShelfKey = "ALL" | "FAVORITES" | LibraryStatus;

const SHELVES: Array<{ key: ShelfKey; label: string; icon: string; status?: LibraryStatus }> = [
  { key: "ALL", label: "All Games", icon: "📚" },
  { key: "FAVORITES", label: "Favorites", icon: "⭐" },
  { key: "PLAYING", label: "Playing", icon: "🎮", status: "PLAYING" },
  { key: "COMPLETED", label: "Completed", icon: "🏆", status: "COMPLETED" },
  { key: "PLAN_TO_PLAY", label: "Plan to Play", icon: "📋", status: "PLAN_TO_PLAY" },
  { key: "ON_HOLD", label: "On Hold", icon: "⏸️", status: "ON_HOLD" },
  { key: "DROPPED", label: "Dropped", icon: "🛑", status: "DROPPED" },
];

export function LibraryPage() {
  const [activeShelf, setActiveShelf] = useState<ShelfKey>("ALL");
  const [items, setItems] = useState<LibraryItemDto[]>([]);
  const [stats, setStats] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortBy, setSortBy] = useState<"recent" | "playtime" | "title">("recent");
  const [editingNotesSlug, setEditingNotesSlug] = useState<string | null>(null);
  const [noteDraft, setNoteDraft] = useState("");

  useEffect(() => {
    loadLibrary();
  }, [activeShelf]);

  async function loadLibrary() {
    setLoading(true);
    try {
      const isFav = activeShelf === "FAVORITES";
      const statusParam = (activeShelf !== "ALL" && activeShelf !== "FAVORITES") ? activeShelf as LibraryStatus : undefined;

      const [list, counts] = await Promise.all([
        libraryService.getMyLibrary(statusParam, isFav),
        libraryService.getStats(),
      ]);
      setItems(list);
      setStats(counts);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }

  async function handleStatusChange(slug: string, newStatus: LibraryStatus) {
    try {
      await libraryService.setStatus(slug, newStatus);
      await loadLibrary();
    } catch (e) {
      console.error(e);
    }
  }

  async function handleToggleFavorite(slug: string) {
    try {
      await libraryService.toggleFavorite(slug);
      await loadLibrary();
    } catch (e) {
      console.error(e);
    }
  }

  async function handleSaveNotes(slug: string) {
    try {
      await libraryService.updateNotes(slug, noteDraft);
      setEditingNotesSlug(null);
      await loadLibrary();
    } catch (e) {
      console.error(e);
    }
  }

  async function handleRemove(slug: string) {
    if (!confirm("Remove this game from your library?")) return;
    try {
      await libraryService.removeFromLibrary(slug);
      await loadLibrary();
    } catch (e) {
      console.error(e);
    }
  }

  const filteredAndSortedItems = useMemo(() => {
    let result = [...items];

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter(
        (item) => item.title.toLowerCase().includes(q) || item.genreName.toLowerCase().includes(q)
      );
    }

    if (sortBy === "recent") {
      result.sort((a, b) => {
        const timeA = a.lastPlayed ? new Date(a.lastPlayed).getTime() : 0;
        const timeB = b.lastPlayed ? new Date(b.lastPlayed).getTime() : 0;
        return timeB - timeA;
      });
    } else if (sortBy === "playtime") {
      result.sort((a, b) => (b.hoursPlayed || 0) - (a.hoursPlayed || 0));
    } else if (sortBy === "title") {
      result.sort((a, b) => a.title.localeCompare(b.title));
    }

    return result;
  }, [items, searchQuery, sortBy]);

  return (
    <div style={containerStyle}>
      {/* Top Banner */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24, flexWrap: "wrap", gap: 12 }}>
        <div>
          <span style={badgeStyle}>STEAM-INSPIRED LIBRARY</span>
          <h1 style={{ margin: "4px 0 0", color: "#f8fafc", fontSize: 28, fontWeight: 800 }}>
            My Game Collection
          </h1>
        </div>
        <div style={{ display: "flex", gap: 16, alignItems: "center", color: "#94a3b8", fontSize: 14 }}>
          <span>Total: <b style={{ color: "#00f2fe" }}>{stats["TOTAL"] ?? 0}</b></span>
          <span>Favorites: <b style={{ color: "#f59e0b" }}>{stats["FAVORITES"] ?? 0}</b></span>
        </div>
      </div>

      {/* Shelf Tabs */}
      <div style={tabsBarStyle}>
        {SHELVES.map((shelf) => {
          const isActive = activeShelf === shelf.key;
          let count = 0;
          if (shelf.key === "ALL") count = stats["TOTAL"] ?? 0;
          else if (shelf.key === "FAVORITES") count = stats["FAVORITES"] ?? 0;
          else if (shelf.status) count = stats[shelf.status] ?? 0;

          return (
            <button
              key={shelf.key}
              onClick={() => setActiveShelf(shelf.key)}
              style={{
                ...tabBtnStyle,
                background: isActive ? "rgba(0, 242, 254, 0.15)" : "transparent",
                color: isActive ? "#00f2fe" : "#94a3b8",
                borderColor: isActive ? "#00f2fe" : "#1e293b",
              }}
            >
              <span>{shelf.icon}</span> {shelf.label} ({count})
            </button>
          );
        })}
      </div>

      {/* Search and Sort Toolbar */}
      <div style={toolbarStyle}>
        <div style={{ flex: 1, minWidth: 200 }}>
          <input
            placeholder="Search within your library…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={searchInputStyle}
          />
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <label style={{ fontSize: 13, color: "#64748b" }}>Sort by:</label>
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
            style={sortSelectStyle}
          >
            <option value="recent">Recently Played</option>
            <option value="playtime">Hours Played (High to Low)</option>
            <option value="title">Title (A - Z)</option>
          </select>
        </div>
      </div>

      {/* Games Shelf Grid */}
      {loading ? (
        <p style={{ color: "#64748b", margin: "40px 0", textAlign: "center" }}>Loading library items…</p>
      ) : filteredAndSortedItems.length === 0 ? (
        <div style={emptyCardStyle}>
          <div style={{ fontSize: 40, marginBottom: 8 }}>🎮</div>
          <p style={{ margin: 0, fontSize: 18, color: "#f8fafc", fontWeight: 700 }}>No games on this shelf</p>
          <p style={{ color: "#64748b", fontSize: 13, marginTop: 6 }}>
            Browse the catalogue or play matches to automatically build your Steam-style library.
          </p>
          <Link to="/games" style={browseBtnStyle}>Browse Store Catalogue</Link>
        </div>
      ) : (
        <div style={gridStyle}>
          {filteredAndSortedItems.map((item) => (
            <div key={item.id} style={cardStyle}>
              {/* Thumbnail with Favorite Star */}
              <div style={{ position: "relative" }}>
                <img src={item.thumbnailUrl} alt={item.title} style={thumbnailStyle} />
                <button
                  onClick={() => handleToggleFavorite(item.gameSlug)}
                  style={{
                    position: "absolute",
                    top: 10,
                    right: 10,
                    background: item.isFavorite ? "rgba(245, 158, 11, 0.9)" : "rgba(15, 23, 42, 0.75)",
                    border: "1px solid",
                    borderColor: item.isFavorite ? "#f59e0b" : "#475569",
                    borderRadius: 20,
                    width: 32,
                    height: 32,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    cursor: "pointer",
                    fontSize: 16,
                  }}
                  title={item.isFavorite ? "Remove from Favorites" : "Add to Favorites"}
                >
                  ⭐
                </button>
              </div>

              <div style={{ padding: 18 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 6 }}>
                  <Link to={`/games/${item.gameSlug}`} style={gameTitleLink}>
                    {item.title}
                  </Link>
                </div>

                <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 12, flexWrap: "wrap" }}>
                  <span style={{ color: "#94a3b8", fontSize: 12 }}>{item.genreName}</span>
                  {item.multiplayer && (
                    <span style={{ fontSize: 10, color: "#00f2fe", background: "rgba(0,242,254,0.1)", border: "1px solid rgba(0,242,254,0.25)", padding: "1px 6px", borderRadius: 4, fontWeight: 700 }}>
                      ONLINE
                    </span>
                  )}
                </div>

                {/* Playtime and Last Played Info */}
                <div style={metaBoxStyle}>
                  <div>
                    <div style={{ fontSize: 11, color: "#64748b", fontWeight: 700 }}>TIME PLAYED</div>
                    <div style={{ fontSize: 15, fontWeight: 800, color: item.hoursPlayed > 0 ? "#00ff87" : "#cbd5e1" }}>
                      {item.hoursPlayed > 0 ? `${item.hoursPlayed.toFixed(1)} hrs` : "Never Played"}
                    </div>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <div style={{ fontSize: 11, color: "#64748b", fontWeight: 700 }}>LAST PLAYED</div>
                    <div style={{ fontSize: 12, color: "#94a3b8" }}>
                      {item.lastPlayed ? new Date(item.lastPlayed).toLocaleDateString() : "Never"}
                    </div>
                  </div>
                </div>

                {/* Status Switcher */}
                <div style={{ marginBottom: 14 }}>
                  <label style={{ fontSize: 11, color: "#64748b", display: "block", marginBottom: 4, fontWeight: 700 }}>SHELF STATUS</label>
                  <select
                    value={item.status}
                    onChange={(e) => handleStatusChange(item.gameSlug, e.target.value as LibraryStatus)}
                    style={selectStyle}
                  >
                    <option value="PLAYING">Currently Playing</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="PLAN_TO_PLAY">Plan to Play</option>
                    <option value="ON_HOLD">On Hold</option>
                    <option value="DROPPED">Dropped</option>
                  </select>
                </div>

                {/* Personal Notes */}
                {editingNotesSlug === item.gameSlug ? (
                  <div style={{ marginBottom: 14 }}>
                    <textarea
                      value={noteDraft}
                      onChange={(e) => setNoteDraft(e.target.value)}
                      placeholder="Add personal notes, strategies, or completion goals…"
                      style={notesTextareaStyle}
                      rows={3}
                    />
                    <div style={{ display: "flex", gap: 8, marginTop: 6 }}>
                      <button onClick={() => handleSaveNotes(item.gameSlug)} style={saveNoteBtnStyle}>
                        Save Note
                      </button>
                      <button onClick={() => setEditingNotesSlug(null)} style={cancelNoteBtnStyle}>
                        Cancel
                      </button>
                    </div>
                  </div>
                ) : (
                  <div style={{ marginBottom: 14 }}>
                    {item.userNotes ? (
                      <div
                        onClick={() => {
                          setEditingNotesSlug(item.gameSlug);
                          setNoteDraft(item.userNotes || "");
                        }}
                        style={notesPreviewStyle}
                        title="Click to edit personal notes"
                      >
                        📝 {item.userNotes}
                      </div>
                    ) : (
                      <button
                        onClick={() => {
                          setEditingNotesSlug(item.gameSlug);
                          setNoteDraft("");
                        }}
                        style={addNoteBtnStyle}
                      >
                        + Add Personal Note
                      </button>
                    )}
                  </div>
                )}

                {/* Quick Launch & Actions */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", paddingTop: 10, borderTop: "1px solid #1e293b" }}>
                  {PLAYABLE_SET.has(item.gameSlug) ? (
                    <Link to={`/play/arena?game=${item.gameSlug}`} style={playBtnStyle}>
                      ⚡ Launch Game
                    </Link>
                  ) : (
                    <Link to={`/games/${item.gameSlug}`} style={storeBtnStyle}>
                      View Page
                    </Link>
                  )}
                  <button onClick={() => handleRemove(item.gameSlug)} style={removeBtnStyle}>
                    Remove
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  maxWidth: 1200,
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
  color: "#00f2fe",
  background: "rgba(0, 242, 254, 0.1)",
  border: "1px solid rgba(0, 242, 254, 0.25)",
};

const tabsBarStyle: React.CSSProperties = {
  display: "flex",
  gap: 8,
  flexWrap: "wrap",
  borderBottom: "1px solid #1e293b",
  paddingBottom: 14,
  marginBottom: 20,
};

const tabBtnStyle: React.CSSProperties = {
  padding: "8px 16px",
  borderRadius: 8,
  border: "1px solid #1e293b",
  cursor: "pointer",
  fontSize: 13,
  fontWeight: 600,
  transition: "all 0.2s",
};

const toolbarStyle: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: 16,
  marginBottom: 24,
  flexWrap: "wrap",
};

const searchInputStyle: React.CSSProperties = {
  width: "100%",
  boxSizing: "border-box",
  padding: "10px 14px",
  background: "#0f172a",
  border: "1px solid #1e293b",
  borderRadius: 8,
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
};

const sortSelectStyle: React.CSSProperties = {
  padding: "8px 12px",
  background: "#0f172a",
  border: "1px solid #1e293b",
  borderRadius: 8,
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
};

const gridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))",
  gap: 20,
};

const cardStyle: React.CSSProperties = {
  background: "#0f172a",
  borderRadius: 14,
  overflow: "hidden",
  border: "1px solid #1e293b",
  transition: "transform 0.15s ease",
};

const thumbnailStyle: React.CSSProperties = {
  width: "100%",
  aspectRatio: "16/9",
  objectFit: "cover",
  display: "block",
};

const gameTitleLink: React.CSSProperties = {
  color: "#f8fafc",
  textDecoration: "none",
  fontWeight: 700,
  fontSize: 17,
};

const metaBoxStyle: React.CSSProperties = {
  background: "#1e293b",
  padding: "10px 14px",
  borderRadius: 8,
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  marginBottom: 14,
};

const selectStyle: React.CSSProperties = {
  width: "100%",
  padding: "8px 10px",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 6,
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
};

const notesTextareaStyle: React.CSSProperties = {
  width: "100%",
  boxSizing: "border-box",
  padding: "8px 10px",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 6,
  color: "#f8fafc",
  fontSize: 12,
  fontFamily: "inherit",
  outline: "none",
  resize: "vertical",
};

const saveNoteBtnStyle: React.CSSProperties = {
  padding: "4px 10px",
  background: "#00f2fe",
  color: "#0f172a",
  fontWeight: 700,
  fontSize: 11,
  borderRadius: 4,
  border: "none",
  cursor: "pointer",
};

const cancelNoteBtnStyle: React.CSSProperties = {
  padding: "4px 10px",
  background: "transparent",
  color: "#94a3b8",
  fontSize: 11,
  borderRadius: 4,
  border: "1px solid #334155",
  cursor: "pointer",
};

const notesPreviewStyle: React.CSSProperties = {
  fontSize: 12,
  color: "#94a3b8",
  background: "rgba(30, 41, 59, 0.6)",
  border: "1px dashed #334155",
  borderRadius: 6,
  padding: "6px 10px",
  cursor: "pointer",
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
};

const addNoteBtnStyle: React.CSSProperties = {
  background: "transparent",
  border: "none",
  color: "#64748b",
  fontSize: 11,
  cursor: "pointer",
  padding: 0,
};

const playBtnStyle: React.CSSProperties = {
  padding: "8px 16px",
  background: "linear-gradient(135deg, #00f2fe 0%, #00ff87 100%)",
  color: "#0f172a",
  fontWeight: 800,
  fontSize: 12,
  borderRadius: 6,
  textDecoration: "none",
  letterSpacing: 0.5,
};

const storeBtnStyle: React.CSSProperties = {
  padding: "6px 12px",
  background: "#1e293b",
  color: "#f8fafc",
  fontWeight: 600,
  fontSize: 12,
  borderRadius: 6,
  textDecoration: "none",
};

const removeBtnStyle: React.CSSProperties = {
  background: "transparent",
  border: "none",
  color: "#ef4444",
  fontSize: 12,
  cursor: "pointer",
};

const emptyCardStyle: React.CSSProperties = {
  padding: "60px 24px",
  textAlign: "center",
  background: "#0f172a",
  borderRadius: 14,
  border: "1px solid #1e293b",
};

const browseBtnStyle: React.CSSProperties = {
  display: "inline-block",
  marginTop: 18,
  padding: "10px 24px",
  background: "linear-gradient(135deg, #00f2fe 0%, #4facfe 100%)",
  color: "#0f172a",
  borderRadius: 8,
  textDecoration: "none",
  fontSize: 13,
  fontWeight: 700,
};
