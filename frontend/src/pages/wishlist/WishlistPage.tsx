import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { wishlistService } from "../../services/wishlistService";
import { WishlistItemDto } from "../../types/wishlist";
import { PLAYABLE_SLUGS } from "../../types/multiplayer";

const PLAYABLE_SET = new Set<string>(PLAYABLE_SLUGS);

export function WishlistPage() {
  const [items, setItems] = useState<WishlistItemDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadWishlist();
  }, []);

  async function loadWishlist() {
    setLoading(true);
    try {
      const data = await wishlistService.getMyWishlist();
      setItems(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }

  async function handleRemove(slug: string) {
    try {
      await wishlistService.removeFromWishlist(slug);
      setItems((prev) => prev.filter((i) => i.gameSlug !== slug));
    } catch (e) {
      console.error(e);
    }
  }

  return (
    <div style={containerStyle}>
      <div style={{ marginBottom: 24 }}>
        <span style={badgeStyle}>SAVED</span>
        <h1 style={{ margin: "4px 0 0", color: "#f8fafc", fontSize: 28 }}>My Wishlist</h1>
        <p style={{ color: "#94a3b8", fontSize: 14, margin: "6px 0 0" }}>
          Games you've bookmarked to play or track across the platform.
        </p>
      </div>

      {loading ? (
        <p style={{ color: "#64748b" }}>Loading wishlist…</p>
      ) : items.length === 0 ? (
        <div style={emptyCardStyle}>
          <p style={{ margin: 0, fontSize: 16, color: "#cbd5e1" }}>Your wishlist is empty.</p>
          <p style={{ color: "#64748b", fontSize: 13, marginTop: 6 }}>
            Explore the catalogue and click "Add to Wishlist" on games you want to save.
          </p>
          <Link to="/" style={browseBtnStyle}>Browse Games</Link>
        </div>
      ) : (
        <div style={gridStyle}>
          {items.map((item) => (
            <div key={item.id} style={cardStyle}>
              <img src={item.thumbnailUrl} alt={item.title} style={thumbnailStyle} />
              <div style={{ padding: 16 }}>
                <Link to={`/games/${item.gameSlug}`} style={gameTitleLink}>
                  {item.title}
                </Link>
                <div style={{ color: "#94a3b8", fontSize: 12, marginTop: 4, marginBottom: 12 }}>
                  {item.genreName} · Added {new Date(item.addedAt).toLocaleDateString()}
                </div>
                <p style={{ fontSize: 13, color: "#cbd5e1", lineHeight: 1.4, height: 36, overflow: "hidden", textOverflow: "ellipsis" }}>
                  {item.description}
                </p>

                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 16 }}>
                  {PLAYABLE_SET.has(item.gameSlug) ? (
                    <Link to={`/play/arena?game=${item.gameSlug}`} style={playBtnStyle}>
                      Play Now
                    </Link>
                  ) : (
                    <Link to={`/games/${item.gameSlug}`} style={viewBtnStyle}>
                      View Details
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
  color: "#facc15",
  background: "rgba(250, 204, 21, 0.1)",
  border: "1px solid rgba(250, 204, 21, 0.25)",
};

const gridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
  gap: 20,
};

const cardStyle: React.CSSProperties = {
  background: "#0f172a",
  borderRadius: 12,
  overflow: "hidden",
  border: "1px solid #1e293b",
};

const thumbnailStyle: React.CSSProperties = {
  width: "100%",
  aspectRatio: "16/9",
  objectFit: "cover",
};

const gameTitleLink: React.CSSProperties = {
  color: "#f8fafc",
  textDecoration: "none",
  fontWeight: 700,
  fontSize: 16,
};

const playBtnStyle: React.CSSProperties = {
  padding: "6px 14px",
  background: "#00f2fe",
  color: "#0f172a",
  fontWeight: 700,
  fontSize: 12,
  borderRadius: 6,
  textDecoration: "none",
};

const viewBtnStyle: React.CSSProperties = {
  padding: "6px 14px",
  background: "#1e293b",
  color: "#f8fafc",
  fontWeight: 600,
  fontSize: 12,
  borderRadius: 6,
  textDecoration: "none",
  border: "1px solid #334155",
};

const removeBtnStyle: React.CSSProperties = {
  background: "transparent",
  border: "none",
  color: "#94a3b8",
  fontSize: 12,
  cursor: "pointer",
};

const emptyCardStyle: React.CSSProperties = {
  padding: "48px 24px",
  textAlign: "center",
  background: "#0f172a",
  borderRadius: 12,
  border: "1px solid #1e293b",
};

const browseBtnStyle: React.CSSProperties = {
  display: "inline-block",
  marginTop: 16,
  padding: "10px 20px",
  background: "#334155",
  color: "#f8fafc",
  borderRadius: 8,
  textDecoration: "none",
  fontSize: 13,
  fontWeight: 600,
};
