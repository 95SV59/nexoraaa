import { useEffect, useState } from "react";
import { communityService } from "../../services/communityService";
import { CommunityPostDto, CommunityPostType } from "../../types/community";
import { useAuth } from "../../context/AuthContext";

interface Props {
  gameSlug: string;
  gameTitle: string;
}

export function GameCommunityHub({ gameSlug, gameTitle }: Props) {
  const { user } = useAuth();
  const [posts, setPosts] = useState<CommunityPostDto[]>([]);
  const [filterType, setFilterType] = useState<CommunityPostType | "ALL">("ALL");
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [postType, setPostType] = useState<CommunityPostType>("DISCUSSION");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadPosts();
  }, [gameSlug, filterType]);

  async function loadPosts() {
    setLoading(true);
    try {
      const typeParam = filterType !== "ALL" ? filterType : undefined;
      const data = await communityService.getPosts(gameSlug, typeParam);
      setPosts(data);
    } catch (e) {
      console.error("Failed to load community posts", e);
    } finally {
      setLoading(false);
    }
  }

  async function handleCreatePost(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;
    setSubmitting(true);
    try {
      await communityService.createPost(gameSlug, { title, content, postType });
      setTitle("");
      setContent("");
      setShowForm(false);
      await loadPosts();
    } catch (e) {
      console.error(e);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleLike(postId: number) {
    try {
      const updated = await communityService.likePost(postId);
      setPosts((prev) => prev.map((p) => (p.id === postId ? updated : p)));
    } catch (e) {
      console.error(e);
    }
  }

  return (
    <div style={{ marginTop: 28 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 18, flexWrap: "wrap", gap: 12 }}>
        <div>
          <span style={{ fontSize: 11, fontWeight: 700, color: "#00f2fe", letterSpacing: 1.5 }}>STEAM COMMUNITY HUB</span>
          <h2 style={{ fontSize: 22, color: "#f8fafc", margin: "4px 0 0" }}>
            {gameTitle} Discussions & Strategy Guides
          </h2>
        </div>
        {user && (
          <button
            onClick={() => setShowForm(!showForm)}
            style={createPostBtnStyle}
          >
            {showForm ? "Cancel" : "+ Start New Discussion"}
          </button>
        )}
      </div>

      {/* Filter Tabs */}
      <div style={{ display: "flex", gap: 8, marginBottom: 20, flexWrap: "wrap" }}>
        {(["ALL", "DISCUSSION", "GUIDE", "ANNOUNCEMENT"] as const).map((tab) => (
          <button
            key={tab}
            onClick={() => setFilterType(tab)}
            style={{
              padding: "6px 14px",
              borderRadius: 6,
              border: "1px solid",
              borderColor: filterType === tab ? "#00f2fe" : "#1e293b",
              background: filterType === tab ? "rgba(0, 242, 254, 0.15)" : "transparent",
              color: filterType === tab ? "#00f2fe" : "#94a3b8",
              cursor: "pointer",
              fontSize: 12,
              fontWeight: 700,
            }}
          >
            {tab === "ALL" ? "All Topics" : tab === "DISCUSSION" ? "💬 Discussions" : tab === "GUIDE" ? "📖 Player Guides" : "📢 Announcements"}
          </button>
        ))}
      </div>

      {/* Post Creation Modal / Box */}
      {showForm && (
        <form onSubmit={handleCreatePost} style={postFormStyle}>
          <h3 style={{ margin: "0 0 12px", color: "#f8fafc", fontSize: 16 }}>Start Community Discussion</h3>
          <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: 12, marginBottom: 10 }}>
            <input
              placeholder="Topic Title (e.g. Optimal Drift Angles in Racers)"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              style={inputStyle}
              required
            />
            <select
              value={postType}
              onChange={(e) => setPostType(e.target.value as CommunityPostType)}
              style={selectStyle}
            >
              <option value="DISCUSSION">Discussion</option>
              <option value="GUIDE">Strategy Guide</option>
            </select>
          </div>
          <textarea
            placeholder="Share strategies, tactical feedback, or match setups with the community…"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={4}
            style={textareaStyle}
            required
          />
          <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 10 }}>
            <button type="button" onClick={() => setShowForm(false)} style={cancelBtnStyle}>
              Cancel
            </button>
            <button type="submit" disabled={submitting} style={publishBtnStyle}>
              {submitting ? "Publishing…" : "Post to Hub"}
            </button>
          </div>
        </form>
      )}

      {/* Posts List */}
      {loading ? (
        <p style={{ color: "#64748b", margin: "20px 0" }}>Loading community topics…</p>
      ) : posts.length === 0 ? (
        <div style={emptyCardStyle}>
          <div style={{ fontSize: 32, marginBottom: 8 }}>💬</div>
          <p style={{ margin: 0, color: "#f8fafc", fontWeight: 700 }}>No discussions yet in this hub</p>
          <p style={{ color: "#64748b", fontSize: 13, marginTop: 4 }}>Be the first player to start a discussion or post a strategy guide!</p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          {posts.map((post) => (
            <div key={post.id} style={postCardStyle}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 8 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <span style={{
                    fontSize: 10,
                    fontWeight: 800,
                    padding: "2px 8px",
                    borderRadius: 4,
                    background: post.postType === "GUIDE" ? "rgba(0,255,135,0.15)" : "rgba(0,242,254,0.15)",
                    color: post.postType === "GUIDE" ? "#00ff87" : "#00f2fe",
                    border: `1px solid ${post.postType === "GUIDE" ? "#00ff87" : "#00f2fe"}`,
                  }}>
                    {post.postType}
                  </span>
                  <h4 style={{ margin: 0, color: "#f8fafc", fontSize: 16, fontWeight: 700 }}>
                    {post.title}
                  </h4>
                </div>
                <span style={{ fontSize: 11, color: "#64748b" }}>
                  {new Date(post.createdAt).toLocaleDateString()}
                </span>
              </div>

              <p style={{ margin: "6px 0 14px", color: "#cbd5e1", fontSize: 13, lineHeight: 1.6, whiteSpace: "pre-wrap" }}>
                {post.content}
              </p>

              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderTop: "1px solid #1e293b", paddingTop: 10 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <img
                    src={post.authorAvatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${post.authorName}`}
                    alt={post.authorName}
                    style={{ width: 22, height: 22, borderRadius: 11, background: "#1e293b" }}
                  />
                  <span style={{ fontSize: 12, color: "#94a3b8", fontWeight: 600 }}>By {post.authorName}</span>
                </div>
                <button
                  type="button"
                  onClick={() => handleLike(post.id)}
                  style={likeBtnStyle}
                >
                  ▲ Upvote ({post.likesCount})
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const createPostBtnStyle: React.CSSProperties = {
  padding: "8px 16px",
  background: "#1e293b",
  border: "1px solid #334155",
  color: "#00f2fe",
  fontWeight: 700,
  fontSize: 13,
  borderRadius: 8,
  cursor: "pointer",
};

const postFormStyle: React.CSSProperties = {
  background: "#0f172a",
  border: "1px solid #1e293b",
  borderRadius: 12,
  padding: 18,
  marginBottom: 20,
};

const inputStyle: React.CSSProperties = {
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 6,
  padding: "8px 12px",
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
};

const selectStyle: React.CSSProperties = {
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 6,
  padding: "8px 10px",
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
};

const textareaStyle: React.CSSProperties = {
  width: "100%",
  boxSizing: "border-box",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: 6,
  padding: 10,
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
  resize: "vertical",
  fontFamily: "inherit",
};

const publishBtnStyle: React.CSSProperties = {
  padding: "8px 18px",
  background: "linear-gradient(135deg, #00f2fe 0%, #00ff87 100%)",
  color: "#060913",
  fontWeight: 800,
  fontSize: 13,
  borderRadius: 6,
  border: "none",
  cursor: "pointer",
};

const cancelBtnStyle: React.CSSProperties = {
  padding: "8px 14px",
  background: "transparent",
  border: "1px solid #334155",
  color: "#94a3b8",
  fontSize: 13,
  borderRadius: 6,
  cursor: "pointer",
};

const postCardStyle: React.CSSProperties = {
  background: "#0f172a",
  border: "1px solid #1e293b",
  borderRadius: 10,
  padding: 16,
};

const likeBtnStyle: React.CSSProperties = {
  background: "rgba(0, 242, 254, 0.08)",
  border: "1px solid rgba(0, 242, 254, 0.25)",
  color: "#00f2fe",
  fontSize: 12,
  fontWeight: 700,
  padding: "4px 10px",
  borderRadius: 6,
  cursor: "pointer",
};

const emptyCardStyle: React.CSSProperties = {
  padding: "36px 20px",
  textAlign: "center",
  background: "#0f172a",
  borderRadius: 10,
  border: "1px solid #1e293b",
};
