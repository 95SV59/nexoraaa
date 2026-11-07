import { useEffect, useState } from "react";
import { articleService } from "../../services/articleService";
import { ArticleCategory, ArticleDto } from "../../types/article";

export function NewsPage() {
  const [articles, setArticles] = useState<ArticleDto[]>([]);
  const [categories, setCategories] = useState<ArticleCategory[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | undefined>(undefined);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    articleService.getCategories().then(setCategories).catch(console.error);
  }, []);

  useEffect(() => {
    loadArticles();
  }, [selectedCategory, search]);

  async function loadArticles() {
    setLoading(true);
    try {
      const data = await articleService.getArticles(selectedCategory, search);
      setArticles(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={containerStyle}>
      <div style={{ marginBottom: 28 }}>
        <span style={badgeStyle}>DISPATCH</span>
        <h1 style={{ margin: "4px 0 0", color: "#f8fafc", fontSize: 28 }}>Gaming News & Tech Dispatches</h1>
        <p style={{ color: "#94a3b8", fontSize: 14, margin: "6px 0 0" }}>
          Latest updates on the NEXORA platform, predictive networking breakthroughs, and tournament events.
        </p>
      </div>

      {/* Filter and Search Bar */}
      <div style={{ display: "flex", gap: 12, flexWrap: "wrap", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button
            onClick={() => setSelectedCategory(undefined)}
            style={{
              ...tabBtnStyle,
              background: selectedCategory === undefined ? "rgba(0, 242, 254, 0.15)" : "#0f172a",
              color: selectedCategory === undefined ? "#00f2fe" : "#94a3b8",
              borderColor: selectedCategory === undefined ? "#00f2fe" : "#1e293b",
            }}
          >
            All News
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              onClick={() => setSelectedCategory(c.id)}
              style={{
                ...tabBtnStyle,
                background: selectedCategory === c.id ? "rgba(0, 242, 254, 0.15)" : "#0f172a",
                color: selectedCategory === c.id ? "#00f2fe" : "#94a3b8",
                borderColor: selectedCategory === c.id ? "#00f2fe" : "#1e293b",
              }}
            >
              {c.name}
            </button>
          ))}
        </div>

        <input
          placeholder="Search articles..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={searchBarStyle}
        />
      </div>

      {/* Articles Feed */}
      {loading ? (
        <p style={{ color: "#64748b" }}>Loading articles…</p>
      ) : articles.length === 0 ? (
        <p style={{ color: "#64748b" }}>No articles matching your criteria.</p>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          {articles.map((a) => {
            const isExpanded = expandedId === a.id;
            return (
              <article key={a.id} style={articleCardStyle}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 16 }}>
                  <div>
                    <span style={categoryTagStyle}>{a.categoryName}</span>
                    <h2 style={{ margin: "8px 0 6px", color: "#f8fafc", fontSize: 20 }}>{a.title}</h2>
                    <div style={{ fontSize: 12, color: "#64748b" }}>
                      By <b style={{ color: "#94a3b8" }}>{a.authorName}</b> · {new Date(a.publishedAt).toLocaleDateString()}
                    </div>
                  </div>
                </div>

                <p style={{ margin: "14px 0", color: "#cbd5e1", fontSize: 14, lineHeight: 1.6 }}>
                  {isExpanded ? a.body : a.summary}
                </p>

                <button
                  onClick={() => setExpandedId(isExpanded ? null : a.id)}
                  style={expandBtnStyle}
                >
                  {isExpanded ? "Collapse" : "Read Full Article →"}
                </button>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  maxWidth: 960,
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
  color: "#38bdf8",
  background: "rgba(56, 189, 248, 0.1)",
  border: "1px solid rgba(56, 189, 248, 0.25)",
};

const tabBtnStyle: React.CSSProperties = {
  padding: "6px 14px",
  borderRadius: 8,
  border: "1px solid #1e293b",
  cursor: "pointer",
  fontSize: 13,
  fontWeight: 600,
  transition: "all 0.2s",
};

const searchBarStyle: React.CSSProperties = {
  padding: "8px 14px",
  background: "#0f172a",
  border: "1px solid #334155",
  borderRadius: 8,
  color: "#f8fafc",
  fontSize: 13,
  outline: "none",
  minWidth: 220,
};

const articleCardStyle: React.CSSProperties = {
  background: "#0f172a",
  borderRadius: 12,
  padding: 20,
  border: "1px solid #1e293b",
};

const categoryTagStyle: React.CSSProperties = {
  fontSize: 11,
  fontWeight: 700,
  color: "#38bdf8",
  background: "rgba(56, 189, 248, 0.12)",
  padding: "2px 8px",
  borderRadius: 4,
};

const expandBtnStyle: React.CSSProperties = {
  background: "none",
  border: "none",
  color: "#00f2fe",
  cursor: "pointer",
  fontSize: 13,
  fontWeight: 600,
  padding: 0,
};
