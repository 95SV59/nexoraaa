import { useEffect, useState } from "react";
import { reviewService } from "../../services/reviewService";
import { ReviewDto } from "../../types/review";
import { useAuth } from "../../context/AuthContext";

interface Props {
  gameSlug: string;
}

export function ReviewList({ gameSlug }: Props) {
  const { user } = useAuth();
  const [reviews, setReviews] = useState<ReviewDto[]>([]);
  const [rating, setRating] = useState(5);
  const [recommended, setRecommended] = useState(true);
  const [spoiler, setSpoiler] = useState(false);
  const [revealedSpoilers, setRevealedSpoilers] = useState<Record<number, boolean>>({});
  const [body, setBody] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    loadReviews();
  }, [gameSlug]);

  async function loadReviews() {
    try {
      const data = await reviewService.getGameReviews(gameSlug);
      setReviews(data);
    } catch (e) {
      console.error("Failed to load reviews", e);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!body.trim()) return;
    setSubmitting(true);
    setMessage(null);
    try {
      await reviewService.createReview(gameSlug, { rating, body, recommended, spoiler });
      setBody("");
      setMessage("Review submitted!");
      await loadReviews();
    } catch {
      setMessage("Failed to post review. Try again.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleLike(reviewId: number) {
    try {
      const res = await reviewService.toggleLike(reviewId);
      setReviews((prev) =>
        prev.map((r) =>
          r.id === reviewId
            ? { ...r, likedByCurrentUser: res.liked, likeCount: r.likeCount + (res.liked ? 1 : -1) }
            : r
        )
      );
    } catch (e) {
      console.error(e);
    }
  }

  async function handleReport(reviewId: number) {
    try {
      await reviewService.reportReview(reviewId);
      setReviews((prev) =>
        prev.map((r) => (r.id === reviewId ? { ...r, reported: true } : r))
      );
    } catch (e) {
      console.error(e);
    }
  }

  async function handleDelete(reviewId: number) {
    if (!confirm("Are you sure you want to delete this review?")) return;
    try {
      await reviewService.deleteReview(reviewId);
      setReviews((prev) => prev.filter((r) => r.id !== reviewId));
    } catch (e) {
      console.error(e);
    }
  }

  const toggleRevealSpoiler = (id: number) => {
    setRevealedSpoilers((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <div style={{ marginTop: 32 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
        <div>
          <span style={{ fontSize: 11, fontWeight: 700, color: "#00f2fe", letterSpacing: 1.5 }}>COMMUNITY FEEDBACK</span>
          <h2 style={{ fontSize: 22, color: "#f8fafc", margin: "4px 0 0" }}>Player Reviews & Playtime Verdicts</h2>
        </div>
        <div style={{ color: "#94a3b8", fontSize: 13 }}>
          {reviews.length} {reviews.length === 1 ? "Review" : "Reviews"} on Record
        </div>
      </div>

      {/* Steam Review Submission Form */}
      {user ? (
        <form onSubmit={handleSubmit} style={formStyle}>
          <div style={{ display: "flex", alignItems: "center", gap: 16, marginBottom: 14, flexWrap: "wrap" }}>
            {/* Recommended Toggle */}
            <div style={{ display: "flex", gap: 8 }}>
              <button
                type="button"
                onClick={() => { setRecommended(true); setRating(5); }}
                style={{
                  ...recToggleBtnStyle,
                  background: recommended ? "rgba(0, 242, 254, 0.2)" : "rgba(30, 41, 59, 0.5)",
                  borderColor: recommended ? "#00f2fe" : "#334155",
                  color: recommended ? "#00f2fe" : "#94a3b8",
                }}
              >
                👍 Recommended
              </button>
              <button
                type="button"
                onClick={() => { setRecommended(false); setRating(2); }}
                style={{
                  ...recToggleBtnStyle,
                  background: !recommended ? "rgba(239, 68, 68, 0.2)" : "rgba(30, 41, 59, 0.5)",
                  borderColor: !recommended ? "#ef4444" : "#334155",
                  color: !recommended ? "#ef4444" : "#94a3b8",
                }}
              >
                👎 Not Recommended
              </button>
            </div>

            {/* Star Rating */}
            <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  type="button"
                  key={star}
                  onClick={() => setRating(star)}
                  style={{
                    background: "none",
                    border: "none",
                    fontSize: 18,
                    cursor: "pointer",
                    color: star <= rating ? "#facc15" : "#475569",
                  }}
                >
                  ★
                </button>
              ))}
              <span style={{ fontSize: 13, fontWeight: 700, color: "#facc15" }}>{rating}/5</span>
            </div>

            {/* Spoiler Checkbox */}
            <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 13, color: "#94a3b8", cursor: "pointer", marginLeft: "auto" }}>
              <input
                type="checkbox"
                checked={spoiler}
                onChange={(e) => setSpoiler(e.target.checked)}
              />
              Contains Spoilers
            </label>
          </div>

          <textarea
            placeholder="Write your comprehensive verdict about this game. Your recorded playtime will be automatically attached..."
            value={body}
            onChange={(e) => setBody(e.target.value)}
            rows={4}
            style={textareaStyle}
            required
          />

          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 10 }}>
            {message && <span style={{ fontSize: 12, color: "#00ff87" }}>{message}</span>}
            <span style={{ flex: 1 }} />
            <button type="submit" disabled={submitting} style={submitButtonStyle}>
              {submitting ? "Publishing Review…" : "Publish Review"}
            </button>
          </div>
        </form>
      ) : (
        <div style={{ padding: 16, background: "#0f172a", borderRadius: 10, border: "1px solid #1e293b", color: "#64748b", fontSize: 13 }}>
          Log in to publish a community review with your verified playtime.
        </div>
      )}

      {/* Steam Reviews List */}
      <div style={{ display: "flex", flexDirection: "column", gap: 14, marginTop: 20 }}>
        {reviews.length === 0 ? (
          <p style={{ color: "#64748b", fontSize: 14 }}>No reviews yet. Be the first to publish your verdict!</p>
        ) : (
          reviews.map((r) => {
            const isSpoilerRevealed = revealedSpoilers[r.id];
            return (
              <div key={r.id} style={reviewCardStyle}>
                {/* Review Header Banner (Steam style) */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", paddingBottom: 12, borderBottom: "1px solid #1e293b", marginBottom: 12, flexWrap: "wrap", gap: 10 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <div style={{
                      width: 40, height: 40, borderRadius: 8,
                      background: r.recommended ? "rgba(0, 242, 254, 0.15)" : "rgba(239, 68, 68, 0.15)",
                      border: `1px solid ${r.recommended ? "#00f2fe" : "#ef4444"}`,
                      display: "flex", alignItems: "center", justifyContent: "center",
                      fontSize: 20,
                    }}>
                      {r.recommended ? "👍" : "👎"}
                    </div>
                    <div>
                      <div style={{ fontWeight: 800, fontSize: 15, color: r.recommended ? "#00f2fe" : "#ef4444", letterSpacing: 0.5 }}>
                        {r.recommended ? "RECOMMENDED" : "NOT RECOMMENDED"}
                      </div>
                      <div style={{ fontSize: 12, color: "#94a3b8" }}>
                        🕒 <strong style={{ color: "#00ff87" }}>
                          {r.hoursPlayed && r.hoursPlayed > 0 ? `${r.hoursPlayed.toFixed(1)} hrs` : "< 1 hr"}
                        </strong> on record
                      </div>
                    </div>
                  </div>

                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <img
                      src={r.userAvatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${r.username}`}
                      alt={r.username}
                      style={{ width: 28, height: 28, borderRadius: 14, background: "#1e293b" }}
                    />
                    <span style={{ fontSize: 13, fontWeight: 700, color: "#f8fafc" }}>{r.username}</span>
                    <span style={{ fontSize: 11, color: "#64748b" }}>· {new Date(r.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>

                {/* Spoiler Handling */}
                {r.spoiler && !isSpoilerRevealed ? (
                  <div
                    onClick={() => toggleRevealSpoiler(r.id)}
                    style={spoilerWarningStyle}
                  >
                    ⚠️ This review contains spoilers. Click to reveal.
                  </div>
                ) : (
                  <div>
                    {r.spoiler && (
                      <span style={{ fontSize: 10, color: "#f59e0b", background: "rgba(245,158,11,0.1)", border: "1px solid #f59e0b", padding: "1px 6px", borderRadius: 4, fontWeight: 700, display: "inline-block", marginBottom: 6 }}>
                        SPOILER WARNING
                      </span>
                    )}
                    <p style={{ margin: "4px 0 14px", color: "#cbd5e1", fontSize: 14, lineHeight: 1.6, whiteSpace: "pre-wrap" }}>
                      {r.body}
                    </p>
                  </div>
                )}

                {/* Action Bar */}
                <div style={{ display: "flex", alignItems: "center", gap: 14, paddingTop: 8 }}>
                  <button
                    type="button"
                    onClick={() => handleLike(r.id)}
                    style={{
                      background: r.likedByCurrentUser ? "rgba(0,242,254,0.1)" : "#1e293b",
                      border: `1px solid ${r.likedByCurrentUser ? "#00f2fe" : "#334155"}`,
                      color: r.likedByCurrentUser ? "#00f2fe" : "#94a3b8",
                      fontSize: 12,
                      padding: "4px 12px",
                      borderRadius: 6,
                      cursor: "pointer",
                      display: "flex",
                      alignItems: "center",
                      gap: 6,
                      fontWeight: 600,
                    }}
                  >
                    👍 Helpful ({r.likeCount})
                  </button>

                  {!r.reported ? (
                    <button
                      type="button"
                      onClick={() => handleReport(r.id)}
                      style={actionBtnStyle}
                    >
                      🚩 Report
                    </button>
                  ) : (
                    <span style={{ fontSize: 11, color: "#eab308" }}>Flagged for moderation</span>
                  )}

                  {(user?.userId === r.userId || user?.role === "ADMIN") && (
                    <button
                      type="button"
                      onClick={() => handleDelete(r.id)}
                      style={{ ...actionBtnStyle, color: "#ef4444" }}
                    >
                      🗑 Delete
                    </button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

const formStyle: React.CSSProperties = {
  background: "#0f172a",
  padding: 20,
  borderRadius: 14,
  border: "1px solid #1e293b",
};

const recToggleBtnStyle: React.CSSProperties = {
  padding: "6px 14px",
  borderRadius: 8,
  border: "1px solid",
  cursor: "pointer",
  fontSize: 13,
  fontWeight: 700,
  transition: "all 0.2s ease",
};

const textareaStyle: React.CSSProperties = {
  width: "100%",
  boxSizing: "border-box",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 8,
  padding: 12,
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
  resize: "vertical",
  fontFamily: "inherit",
};

const submitButtonStyle: React.CSSProperties = {
  padding: "10px 20px",
  background: "linear-gradient(135deg, #00f2fe 0%, #4facfe 100%)",
  color: "#0f172a",
  fontWeight: 800,
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
  fontSize: 13,
};

const reviewCardStyle: React.CSSProperties = {
  background: "#0f172a",
  border: "1px solid #1e293b",
  borderRadius: 12,
  padding: 18,
};

const spoilerWarningStyle: React.CSSProperties = {
  background: "rgba(30, 41, 59, 0.5)",
  border: "1px dashed #f59e0b",
  borderRadius: 8,
  padding: 14,
  color: "#f59e0b",
  fontSize: 13,
  fontWeight: 600,
  cursor: "pointer",
  textAlign: "center",
  margin: "8px 0 14px",
};

const actionBtnStyle: React.CSSProperties = {
  background: "transparent",
  border: "none",
  color: "#64748b",
  fontSize: 12,
  cursor: "pointer",
};
