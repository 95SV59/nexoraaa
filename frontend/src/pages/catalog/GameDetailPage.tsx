import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { gameService } from "../../services/gameService";
import { wishlistService } from "../../services/wishlistService";
import { libraryService } from "../../services/libraryService";
import { ReviewList } from "../../components/reviews/ReviewList";
import { GameCommunityHub } from "../../components/community/GameCommunityHub";
import { PLAYABLE_SLUGS } from "../../types/multiplayer";
import type { GameDetail } from "../../types/catalog";
import type { LibraryStatus } from "../../types/library";

const PLAYABLE_SET = new Set<string>(PLAYABLE_SLUGS);

export function GameDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const [game, setGame] = useState<GameDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [inWishlist, setInWishlist] = useState(false);
  const [libraryStatus, setLibraryStatus] = useState<LibraryStatus | "">("");
  const [activeTab, setActiveTab] = useState<"overview" | "reviews" | "community">("overview");

  useEffect(() => {
    if (!slug) return;
    gameService.getGame(slug).then(setGame).catch(() => setError("Game not found."));

    wishlistService.isInWishlist(slug).then(setInWishlist).catch(() => {});
    libraryService.getGameStatus(slug).then((res) => setLibraryStatus(res.status)).catch(() => {});
  }, [slug]);

  async function handleToggleWishlist() {
    if (!slug) return;
    try {
      if (inWishlist) {
        await wishlistService.removeFromWishlist(slug);
        setInWishlist(false);
      } else {
        await wishlistService.addToWishlist(slug);
        setInWishlist(true);
      }
    } catch (e) {
      console.error(e);
    }
  }

  async function handleStatusChange(status: string) {
    if (!slug) return;
    try {
      if (status === "") {
        await libraryService.removeFromLibrary(slug);
        setLibraryStatus("");
      } else {
        await libraryService.setStatus(slug, status as LibraryStatus);
        setLibraryStatus(status as LibraryStatus);
      }
    } catch (e) {
      console.error(e);
    }
  }

  if (error) return <div style={styles.page}><p style={{ color: "#f87171" }}>{error}</p></div>;
  if (!game) return <div style={styles.page}><p style={styles.dim}>Loading…</p></div>;

  const screenshot = game.media.find((m) => m.type === "SCREENSHOT");

  return (
    <div style={styles.page}>
      <Link to="/" style={styles.back}>&larr; Back to Catalogue</Link>
      {screenshot && <img src={screenshot.url} alt={game.title} style={styles.hero} />}

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: 16 }}>
        <div>
          <h1 style={{ margin: "0 0 6px", fontSize: 28, color: "#f8fafc" }}>{game.title}</h1>
          <p style={styles.dim}>
            {game.genre ?? "Uncategorized"}
            {game.platforms.length > 0 ? ` · ${game.platforms.join(", ")}` : ""}
            {game.multiplayer && game.minPlayers && game.maxPlayers ? ` · ${game.minPlayers}-${game.maxPlayers} players` : ""}
          </p>
        </div>

        {/* Action Controls */}
        <div style={{ display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap" }}>
          {PLAYABLE_SET.has(game.slug) ? (
            <Link to={`/play/arena?game=${game.slug}`} style={styles.playButton}>
              ▶ Play Multiplayer Now
            </Link>
          ) : (
            <span style={styles.comingSoon}>Upcoming Engine</span>
          )}

          <button onClick={handleToggleWishlist} style={inWishlist ? styles.wishlistActive : styles.wishlistBtn}>
            {inWishlist ? "★ In Wishlist" : "☆ Add to Wishlist"}
          </button>

          <select
            value={libraryStatus}
            onChange={(e) => handleStatusChange(e.target.value)}
            style={styles.librarySelect}
          >
            <option value="">Add to Library...</option>
            <option value="PLAYING">Currently Playing</option>
            <option value="COMPLETED">Completed</option>
            <option value="PLAN_TO_PLAY">Plan to Play</option>
            <option value="ON_HOLD">On Hold</option>
            <option value="DROPPED">Dropped</option>
          </select>
        </div>
      </div>

      {/* Steam Navigation Tabs */}
      <div style={styles.navTabs}>
        <button
          onClick={() => setActiveTab("overview")}
          style={{
            ...styles.navTabBtn,
            color: activeTab === "overview" ? "#00f2fe" : "#94a3b8",
            borderBottomColor: activeTab === "overview" ? "#00f2fe" : "transparent",
          }}
        >
          📖 Store Overview
        </button>
        <button
          onClick={() => setActiveTab("reviews")}
          style={{
            ...styles.navTabBtn,
            color: activeTab === "reviews" ? "#00ff87" : "#94a3b8",
            borderBottomColor: activeTab === "reviews" ? "#00ff87" : "transparent",
          }}
        >
          💬 Player Reviews
        </button>
        <button
          onClick={() => setActiveTab("community")}
          style={{
            ...styles.navTabBtn,
            color: activeTab === "community" ? "#f59e0b" : "#94a3b8",
            borderBottomColor: activeTab === "community" ? "#f59e0b" : "transparent",
          }}
        >
          👥 Community Hub & Guides
        </button>
      </div>

      {/* Tab 1: Overview */}
      {activeTab === "overview" && (
        <>
          <p style={{ marginTop: 20, lineHeight: 1.7, color: "#cbd5e1", fontSize: 15 }}>{game.description}</p>

          <dl style={styles.meta}>
            {game.developer && (<><dt style={styles.dt}>Developer</dt><dd style={styles.dd}>{game.developer}</dd></>)}
            {game.publisher && (<><dt style={styles.dt}>Publisher</dt><dd style={styles.dd}>{game.publisher}</dd></>)}
            {game.releaseDate && (<><dt style={styles.dt}>Released</dt><dd style={styles.dd}>{game.releaseDate}</dd></>)}
            <dt style={styles.dt}>Multiplayer Engine</dt>
            <dd style={styles.dd}>
              {PLAYABLE_SET.has(game.slug) ? "Authoritative HTML5 Canvas + STOMP WebSocket (PWNC Active)" : "Catalogue Entry"}
            </dd>
          </dl>

          <ReviewList gameSlug={game.slug} />
        </>
      )}

      {/* Tab 2: Reviews */}
      {activeTab === "reviews" && (
        <ReviewList gameSlug={game.slug} />
      )}

      {/* Tab 3: Community Hub */}
      {activeTab === "community" && (
        <GameCommunityHub gameSlug={game.slug} gameTitle={game.title} />
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: { maxWidth: 900, margin: "0 auto", padding: "24px 20px", fontFamily: "system-ui, -apple-system, sans-serif", color: "#e2e8f0" },
  dim: { color: "#94a3b8", fontSize: 14 },
  back: { color: "#38bdf8", fontSize: 13, textDecoration: "none", display: "inline-block", marginBottom: 14, fontWeight: 500 },
  hero: { width: "100%", borderRadius: 12, marginBottom: 18, aspectRatio: "16/9", objectFit: "cover", border: "1px solid #1e293b" },
  playButton: {
    display: "inline-block", padding: "10px 20px", borderRadius: 8,
    background: "linear-gradient(135deg, #00f2fe 0%, #4facfe 100%)", color: "#0f172a", fontWeight: 700, textDecoration: "none", fontSize: 14,
  },
  comingSoon: { display: "inline-block", padding: "8px 14px", borderRadius: 8, background: "#1e293b", color: "#fbbf24", fontSize: 13 },
  wishlistBtn: {
    padding: "10px 16px", borderRadius: 8, border: "1px solid #334155", background: "#1e293b", color: "#f8fafc", cursor: "pointer", fontSize: 13, fontWeight: 600,
  },
  wishlistActive: {
    padding: "10px 16px", borderRadius: 8, border: "1px solid #facc15", background: "rgba(250, 204, 21, 0.15)", color: "#facc15", cursor: "pointer", fontSize: 13, fontWeight: 600,
  },
  librarySelect: {
    padding: "10px 14px", borderRadius: 8, border: "1px solid #334155", background: "#1e293b", color: "#f8fafc", cursor: "pointer", fontSize: 13, outline: "none",
  },
  navTabs: {
    display: "flex", gap: 12, borderBottom: "1px solid #1e293b", marginTop: 24, paddingBottom: 2,
  },
  navTabBtn: {
    background: "transparent", border: "none", borderBottom: "2px solid transparent",
    padding: "10px 14px", fontSize: 14, fontWeight: 700, cursor: "pointer", transition: "all 0.15s ease",
  },
  meta: { marginTop: 24, padding: "16px", background: "#0f172a", borderRadius: 10, border: "1px solid #1e293b", display: "grid", gridTemplateColumns: "auto 1fr", columnGap: 16, rowGap: 8 },
  dt: { color: "#94a3b8", fontSize: 13, fontWeight: 600 },
  dd: { fontSize: 13, color: "#f8fafc" },
};
