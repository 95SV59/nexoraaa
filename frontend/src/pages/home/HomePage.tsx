import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { gameService } from "../../services/gameService";
import { recommendationService } from "../../services/recommendationService";
import { articleService } from "../../services/articleService";
import { GameSummary } from "../../types/catalog";
import { RecommendationDto } from "../../types/recommendation";
import { ArticleDto } from "../../types/article";

const MULTIPLAYER_CAROUSEL = [
  {
    slug: "arena",
    title: "NEXORA Arena",
    tagline: "AUTHORITATIVE TACTICAL SHOOTER",
    genre: "2D Tactical Shooter",
    players: "2 - 8 Players",
    desc: "Top-down competitive tactical arena with high-frequency projectile synchronization, real-time aiming prediction, and adaptive broadcast rate throttling.",
    img: "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=1200&auto=format&fit=crop&q=80",
    thumbnails: [
      "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=400&auto=format&fit=crop&q=80",
    ],
    accent: "#00f2fe",
    features: ["Server-Authoritative Loop", "Closed-Loop PWNC", "AI Bot Opponents", "Competitive Leaderboard"],
  },
  {
    slug: "racers",
    title: "NEXORA Racers",
    tagline: "HIGH-OCTANE KART SIMULATION",
    genre: "Arcade Racing",
    players: "1 - 8 Players",
    desc: "Precision kart racing featuring advanced drift mechanics, client dead-reckoning extrapolation, and checkpoint state reconciliation.",
    img: "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&auto=format&fit=crop&q=80",
    thumbnails: [
      "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=400&auto=format&fit=crop&q=80",
    ],
    accent: "#f472b6",
    features: ["Drift Physics Engine", "Checkpoints Verification", "Ghost Synchronization", "Real-Time Telemetry"],
  },
  {
    slug: "football",
    title: "NEXORA Football",
    tagline: "FAST ARCADE SOCCER CONFLICT",
    genre: "Arcade Sports",
    players: "1 - 6 Players",
    desc: "Fast-paced team football emphasizing ball physics consistency constraints, slide tackles, and goal-line state guarantees.",
    img: "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=1200&auto=format&fit=crop&q=80",
    thumbnails: [
      "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=400&auto=format&fit=crop&q=80",
    ],
    accent: "#00ff87",
    features: ["Critical Ball Consistency", "Dynamic Team Formation", "Goal Replays", "Zero-Lag Extrapolation"],
  },
  {
    slug: "tag",
    title: "NEXORA Tag",
    tagline: "EVASIVE AGILITY ARENA",
    genre: "Arena Tag",
    players: "1 - 10 Players",
    desc: "High-adrenaline evasive tag benchmarking instant state transition latency and jitter compensation under heavy network stress.",
    img: "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=1200&auto=format&fit=crop&q=80",
    thumbnails: [
      "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=400&auto=format&fit=crop&q=80",
    ],
    accent: "#f59e0b",
    features: ["Instant Tag Transitions", "Immunity Timers", "Speed Zones", "Multi-Client Benchmarks"],
  },
];

export function HomePage() {
  const [carouselIndex, setCarouselIndex] = useState(0);
  const [recommendations, setRecommendations] = useState<RecommendationDto[]>([]);
  const [featuredGames, setFeaturedGames] = useState<GameSummary[]>([]);
  const [articles, setArticles] = useState<ArticleDto[]>([]);
  const [activeTab, setActiveTab] = useState<"trending" | "multiplayer" | "topRated">("trending");

  useEffect(() => {
    recommendationService.getRecommendations().then(setRecommendations).catch(console.error);
    gameService.listGames({ page: 0, size: 8 }).then((res) => setFeaturedGames(res.items)).catch(console.error);
    articleService.getArticles().then((res) => setArticles(res.slice(0, 3))).catch(console.error);
  }, []);

  // Auto-rotate carousel every 7 seconds
  useEffect(() => {
    const timer = setInterval(() => {
      setCarouselIndex((prev) => (prev + 1) % MULTIPLAYER_CAROUSEL.length);
    }, 7000);
    return () => clearInterval(timer);
  }, []);

  const currentHero = MULTIPLAYER_CAROUSEL[carouselIndex];

  return (
    <div style={containerStyle}>
      {/* STEAM-STYLE FEATURED & RECOMMENDED CAROUSEL */}
      <section style={{ marginBottom: 40 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
          <span style={sectionBadgeStyle}>FEATURED & RECOMMENDED</span>
          <div style={{ display: "flex", gap: 6 }}>
            <button
              onClick={() => setCarouselIndex((prev) => (prev === 0 ? MULTIPLAYER_CAROUSEL.length - 1 : prev - 1))}
              style={carouselNavBtnStyle}
              title="Previous"
            >
              ‹
            </button>
            <button
              onClick={() => setCarouselIndex((prev) => (prev + 1) % MULTIPLAYER_CAROUSEL.length)}
              style={carouselNavBtnStyle}
              title="Next"
            >
              ›
            </button>
          </div>
        </div>

        {/* Carousel Main Card */}
        <div style={{
          ...carouselCardStyle,
          boxShadow: `0 0 35px ${currentHero.accent}20`,
          borderColor: `${currentHero.accent}40`,
        }}>
          {/* Main Visual Left */}
          <div style={{ flex: "1.8", minWidth: 320, position: "relative", overflow: "hidden" }}>
            <img
              src={currentHero.img}
              alt={currentHero.title}
              style={{ width: "100%", height: "100%", minHeight: 360, objectFit: "cover", display: "block" }}
            />
            <div style={carouselGradientOverlayStyle} />
            <div style={{ position: "absolute", bottom: 24, left: 24, right: 24, zIndex: 2 }}>
              <span style={{ ...heroTagBadgeStyle, color: currentHero.accent, borderColor: currentHero.accent }}>
                {currentHero.tagline}
              </span>
              <h1 style={{ fontSize: 36, fontWeight: 900, color: "#f8fafc", margin: "8px 0 6px" }}>
                {currentHero.title}
              </h1>
              <p style={{ color: "#cbd5e1", fontSize: 13, margin: 0, maxWidth: 520, lineHeight: 1.5 }}>
                {currentHero.desc}
              </p>
            </div>
          </div>

          {/* Right Details Column (Steam-inspired thumbnail stack) */}
          <div style={carouselRightColStyle}>
            <div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                <span style={{ fontSize: 12, fontWeight: 700, color: currentHero.accent }}>{currentHero.genre}</span>
                <span style={{ fontSize: 11, color: "#94a3b8", background: "#1e293b", padding: "2px 8px", borderRadius: 4 }}>
                  {currentHero.players}
                </span>
              </div>
              <p style={{ fontSize: 13, color: "#94a3b8", lineHeight: 1.4, margin: "0 0 14px" }}>
                Ready to play directly in your browser with zero install and predictive network optimization.
              </p>
            </div>

            {/* Thumbnail Previews */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 16 }}>
              {currentHero.thumbnails.map((thumb, idx) => (
                <img
                  key={idx}
                  src={thumb}
                  alt=""
                  style={{ width: "100%", aspectRatio: "16/9", objectFit: "cover", borderRadius: 6, border: "1px solid #1e293b" }}
                />
              ))}
            </div>

            {/* Features Tags */}
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 18 }}>
              {currentHero.features.map((feat) => (
                <span key={feat} style={featureTagStyle}>
                  ✓ {feat}
                </span>
              ))}
            </div>

            {/* CTAs */}
            <div style={{ marginTop: "auto" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
                <span style={{ fontSize: 12, color: "#64748b" }}>PRICE</span>
                <span style={{ fontSize: 13, fontWeight: 800, color: "#00ff87", background: "rgba(0,255,135,0.1)", padding: "2px 8px", borderRadius: 4 }}>
                  FREE TO PLAY
                </span>
              </div>
              <div style={{ display: "flex", gap: 10 }}>
                <Link to={`/play/arena?game=${currentHero.slug}`} style={carouselPlayBtnStyle}>
                  ⚡ Play Now
                </Link>
                <Link to={`/games/${currentHero.slug}`} style={carouselDetailsBtnStyle}>
                  Store Page
                </Link>
              </div>
            </div>
          </div>
        </div>

        {/* Carousel Thumbnail Nav Bar (Steam layout) */}
        <div style={carouselThumbStripStyle}>
          {MULTIPLAYER_CAROUSEL.map((game, idx) => (
            <div
              key={game.slug}
              onClick={() => setCarouselIndex(idx)}
              style={{
                ...thumbNavCardStyle,
                borderColor: carouselIndex === idx ? game.accent : "#1e293b",
                background: carouselIndex === idx ? "rgba(15, 23, 42, 0.95)" : "rgba(15, 23, 42, 0.5)",
              }}
            >
              <img src={game.img} alt={game.title} style={{ width: 44, height: 28, objectFit: "cover", borderRadius: 4 }} />
              <div>
                <div style={{ fontSize: 12, fontWeight: 700, color: carouselIndex === idx ? "#f8fafc" : "#94a3b8" }}>
                  {game.title}
                </div>
                <div style={{ fontSize: 10, color: game.accent }}>{game.genre}</div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* STEAM-STYLE DISCOVERY SHELVES WITH TABS */}
      <section style={{ margin: "40px 0" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 18, flexWrap: "wrap", gap: 12 }}>
          <div style={{ display: "flex", gap: 10 }}>
            <button
              onClick={() => setActiveTab("trending")}
              style={{
                ...tabSelectBtnStyle,
                color: activeTab === "trending" ? "#00f2fe" : "#94a3b8",
                borderBottomColor: activeTab === "trending" ? "#00f2fe" : "transparent",
              }}
            >
              🔥 Trending in Catalogue
            </button>
            <button
              onClick={() => setActiveTab("multiplayer")}
              style={{
                ...tabSelectBtnStyle,
                color: activeTab === "multiplayer" ? "#00ff87" : "#94a3b8",
                borderBottomColor: activeTab === "multiplayer" ? "#00ff87" : "transparent",
              }}
            >
              ⚡ Live Multiplayer
            </button>
            <button
              onClick={() => setActiveTab("topRated")}
              style={{
                ...tabSelectBtnStyle,
                color: activeTab === "topRated" ? "#f59e0b" : "#94a3b8",
                borderBottomColor: activeTab === "topRated" ? "#f59e0b" : "transparent",
              }}
            >
              ⭐ Staff Picks & Recommendations
            </button>
          </div>
          <Link to="/games" style={{ color: "#00f2fe", fontSize: 13, textDecoration: "none", fontWeight: 600 }}>
            See All Games →
          </Link>
        </div>

        {/* Tab Content Display */}
        {activeTab === "multiplayer" && (
          <div style={multiplayerGridStyle}>
            {MULTIPLAYER_CAROUSEL.map((game) => (
              <div key={game.slug} style={multiplayerCardStyle}>
                <div style={{ position: "relative" }}>
                  <img src={game.img} alt={game.title} style={cardImgStyle} />
                  <span style={{ ...gameGenreBadge, borderColor: game.accent, color: game.accent }}>
                    {game.genre}
                  </span>
                </div>
                <div style={{ padding: 18 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <h3 style={{ margin: 0, color: "#f8fafc", fontSize: 18 }}>{game.title}</h3>
                    <span style={{ fontSize: 11, color: "#94a3b8" }}>{game.players}</span>
                  </div>
                  <p style={{ color: "#cbd5e1", fontSize: 13, margin: "8px 0 16px", lineHeight: 1.5, minHeight: 40 }}>
                    {game.desc}
                  </p>
                  <div style={{ display: "flex", gap: 10 }}>
                    <Link to={`/play/arena?game=${game.slug}`} style={playNowBtn}>
                      Play Match
                    </Link>
                    <Link to={`/games/${game.slug}`} style={detailsBtn}>
                      Details
                    </Link>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {activeTab === "trending" && (
          <div style={recGridStyle}>
            {featuredGames.map((game) => (
              <div key={game.id} style={recCardStyle}>
                <img
                  src={game.thumbnailUrl || "https://placehold.co/640x360"}
                  alt={game.title}
                  style={recImgStyle}
                />
                <div style={{ padding: 14 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <h4 style={{ margin: "4px 0 2px", color: "#f8fafc", fontSize: 15 }}>{game.title}</h4>
                    <span style={{ fontSize: 10, color: "#00ff87", fontWeight: 700 }}>FREE</span>
                  </div>
                  <div style={{ color: "#94a3b8", fontSize: 12, marginBottom: 12 }}>{game.genre ?? "Action"}</div>
                  <Link to={`/games/${game.slug}`} style={viewLinkStyle}>
                    Inspect Details →
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}

        {activeTab === "topRated" && (
          <div style={recGridStyle}>
            {recommendations.slice(0, 4).map((rec, idx) => (
              <div key={idx} style={recCardStyle}>
                <img
                  src={rec.game.thumbnailUrl || "https://placehold.co/640x360"}
                  alt={rec.game.title}
                  style={recImgStyle}
                />
                <div style={{ padding: 14 }}>
                  <span style={recReasonBadge}>{rec.reason}</span>
                  <h4 style={{ margin: "6px 0 2px", color: "#f8fafc", fontSize: 15 }}>{rec.game.title}</h4>
                  <div style={{ color: "#94a3b8", fontSize: 12, marginBottom: 10 }}>{rec.game.genre ?? "Action"}</div>
                  <Link to={`/games/${rec.game.slug}`} style={viewLinkStyle}>
                    View Title →
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Predictive Networking Architecture Banner */}
      <section style={patentBannerStyle}>
        <div>
          <span style={{ fontSize: 11, fontWeight: 800, color: "#00ff87", letterSpacing: 1.5 }}>
            THE PROPOSED COMPUTER NETWORKING INVENTION
          </span>
          <h2 style={{ color: "#f8fafc", margin: "8px 0", fontSize: 24 }}>
            Predictive What-If Network Control (PWNC)
          </h2>
          <p style={{ color: "#cbd5e1", fontSize: 14, lineHeight: 1.6, maxWidth: 680, margin: 0 }}>
            Standard gaming architectures react only after lag is observed. NEXORA forecasts impending
            telemetry spikes, performs virtual what-if evaluations on candidate synchronization strategies,
            and applies confidence-gated controls before user state violates consistency constraints.
          </p>
        </div>
        <Link to="/experiments" style={bannerCtaStyle}>
          Compare Baseline vs. Proposed
        </Link>
      </section>

      {/* Latest News & Dispatches */}
      {articles.length > 0 && (
        <section style={{ margin: "48px 0" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginBottom: 20 }}>
            <div>
              <span style={sectionBadgeStyle}>COMMUNITY & DISPATCHES</span>
              <h2 style={sectionTitleStyle}>Latest News & Insights</h2>
            </div>
            <Link to="/news" style={{ color: "#00f2fe", fontSize: 13, textDecoration: "none", fontWeight: 600 }}>
              Read All News →
            </Link>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))", gap: 16 }}>
            {articles.map((a) => (
              <div key={a.id} style={newsCardStyle}>
                <span style={{ fontSize: 11, color: "#00f2fe", fontWeight: 700 }}>{a.categoryName}</span>
                <h4 style={{ margin: "8px 0", color: "#f8fafc", fontSize: 16 }}>{a.title}</h4>
                <p style={{ color: "#94a3b8", fontSize: 13, lineHeight: 1.5, margin: "0 0 12px" }}>
                  {a.summary}
                </p>
                <div style={{ fontSize: 11, color: "#64748b" }}>
                  {new Date(a.publishedAt).toLocaleDateString()}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  maxWidth: 1200,
  margin: "0 auto",
  padding: "24px 20px",
  fontFamily: "system-ui, -apple-system, sans-serif",
};

const sectionBadgeStyle: React.CSSProperties = {
  fontSize: 11,
  fontWeight: 700,
  letterSpacing: 1.5,
  color: "#00f2fe",
};

const sectionTitleStyle: React.CSSProperties = {
  fontSize: 22,
  fontWeight: 800,
  color: "#f8fafc",
  margin: "4px 0 0",
};

const carouselNavBtnStyle: React.CSSProperties = {
  background: "#1e293b",
  border: "1px solid #334155",
  color: "#f8fafc",
  width: 32,
  height: 32,
  borderRadius: 6,
  cursor: "pointer",
  fontSize: 18,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
};

const carouselCardStyle: React.CSSProperties = {
  display: "flex",
  borderRadius: 16,
  overflow: "hidden",
  background: "#0d1627",
  border: "1px solid #1e293b",
  transition: "all 0.3s ease",
  flexWrap: "wrap",
};

const carouselGradientOverlayStyle: React.CSSProperties = {
  position: "absolute",
  inset: 0,
  background: "linear-gradient(to top, rgba(6, 9, 19, 0.95) 0%, rgba(6, 9, 19, 0.3) 60%, transparent 100%)",
};

const heroTagBadgeStyle: React.CSSProperties = {
  display: "inline-block",
  fontSize: 10,
  fontWeight: 800,
  letterSpacing: 1.5,
  border: "1px solid",
  padding: "2px 8px",
  borderRadius: 4,
  background: "rgba(15, 23, 42, 0.85)",
};

const carouselRightColStyle: React.CSSProperties = {
  flex: "1",
  minWidth: 280,
  padding: 24,
  display: "flex",
  flexDirection: "column",
  background: "#0b1220",
  borderLeft: "1px solid #1e293b",
};

const featureTagStyle: React.CSSProperties = {
  fontSize: 10,
  color: "#94a3b8",
  background: "#152033",
  padding: "3px 8px",
  borderRadius: 4,
  border: "1px solid #1e293b",
};

const carouselPlayBtnStyle: React.CSSProperties = {
  flex: 1,
  textAlign: "center",
  padding: "10px 16px",
  background: "linear-gradient(135deg, #00f2fe 0%, #00ff87 100%)",
  color: "#060913",
  fontWeight: 800,
  fontSize: 13,
  borderRadius: 8,
  textDecoration: "none",
  letterSpacing: 0.5,
};

const carouselDetailsBtnStyle: React.CSSProperties = {
  padding: "10px 16px",
  background: "#1e293b",
  border: "1px solid #334155",
  color: "#f8fafc",
  fontSize: 13,
  fontWeight: 600,
  borderRadius: 8,
  textDecoration: "none",
};

const carouselThumbStripStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
  gap: 12,
  marginTop: 14,
};

const thumbNavCardStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 10,
  padding: "8px 12px",
  borderRadius: 8,
  border: "1px solid #1e293b",
  cursor: "pointer",
  transition: "all 0.2s ease",
};

const tabSelectBtnStyle: React.CSSProperties = {
  background: "transparent",
  border: "none",
  borderBottom: "2px solid transparent",
  padding: "8px 14px",
  cursor: "pointer",
  fontSize: 15,
  fontWeight: 700,
  transition: "all 0.15s ease",
};

const multiplayerGridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
  gap: 20,
};

const multiplayerCardStyle: React.CSSProperties = {
  background: "#0f172a",
  borderRadius: 14,
  border: "1px solid #1e293b",
  overflow: "hidden",
};

const cardImgStyle: React.CSSProperties = {
  width: "100%",
  aspectRatio: "16/9",
  objectFit: "cover",
};

const gameGenreBadge: React.CSSProperties = {
  position: "absolute",
  top: 10,
  right: 10,
  background: "rgba(15, 23, 42, 0.85)",
  border: "1px solid",
  padding: "2px 8px",
  borderRadius: 4,
  fontSize: 11,
  fontWeight: 700,
};

const playNowBtn: React.CSSProperties = {
  flex: 1,
  textAlign: "center",
  padding: "8px 12px",
  background: "#00f2fe",
  color: "#0f172a",
  fontWeight: 700,
  fontSize: 13,
  borderRadius: 6,
  textDecoration: "none",
};

const detailsBtn: React.CSSProperties = {
  padding: "8px 14px",
  background: "#1e293b",
  border: "1px solid #334155",
  color: "#f8fafc",
  fontSize: 13,
  borderRadius: 6,
  textDecoration: "none",
};

const recGridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
  gap: 16,
};

const recCardStyle: React.CSSProperties = {
  background: "#0f172a",
  borderRadius: 12,
  border: "1px solid #1e293b",
  overflow: "hidden",
};

const recImgStyle: React.CSSProperties = {
  width: "100%",
  aspectRatio: "16/9",
  objectFit: "cover",
};

const recReasonBadge: React.CSSProperties = {
  display: "inline-block",
  fontSize: 10,
  fontWeight: 700,
  color: "#facc15",
  background: "rgba(250, 204, 21, 0.12)",
  padding: "2px 6px",
  borderRadius: 4,
  marginBottom: 4,
};

const viewLinkStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 600,
  color: "#00f2fe",
  textDecoration: "none",
};

const patentBannerStyle: React.CSSProperties = {
  padding: 28,
  borderRadius: 16,
  background: "linear-gradient(135deg, rgba(0, 242, 254, 0.08) 0%, rgba(123, 44, 191, 0.1) 100%)",
  border: "1px solid rgba(0, 242, 254, 0.3)",
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: 24,
  flexWrap: "wrap",
};

const bannerCtaStyle: React.CSSProperties = {
  padding: "12px 20px",
  background: "#00ff87",
  color: "#0f172a",
  fontWeight: 700,
  fontSize: 13,
  borderRadius: 8,
  textDecoration: "none",
  whiteSpace: "nowrap",
};

const newsCardStyle: React.CSSProperties = {
  background: "#0f172a",
  padding: 18,
  borderRadius: 12,
  border: "1px solid #1e293b",
};
