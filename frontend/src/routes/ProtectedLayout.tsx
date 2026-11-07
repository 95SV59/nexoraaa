import { Navigate, Outlet, Link, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { NotificationBell } from "../components/social/NotificationBell";
import { FriendsDrawer } from "../components/social/FriendsDrawer";

export function ProtectedLayout() {
  const { user, loading, logout } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "#060913", color: "#00f2fe" }}>
        <p style={{ letterSpacing: 2, fontSize: 14, fontWeight: 700 }}>INITIALIZING NEXORA…</p>
      </div>
    );
  }
  if (!user) return <Navigate to="/login" replace />;

  const isActive = (path: string) => {
    if (path === "/" && location.pathname === "/") return true;
    if (path !== "/" && location.pathname.startsWith(path)) return true;
    return false;
  };

  return (
    <div style={{ minHeight: "100vh", background: "#060913", color: "#f8fafc" }}>
      <header style={navHeaderStyle}>
        <div style={navInnerStyle}>
          {/* Logo */}
          <Link to="/" style={logoStyle}>
            <span style={{ color: "#00f2fe" }}>NEX</span>ORA
            <span style={logoDotStyle}>●</span>
          </Link>

          {/* Navigation Links */}
          <nav style={navLinksStyle}>
            <Link to="/" style={navLinkStyle(isActive("/"))}>Home</Link>
            <Link to="/games" style={navLinkStyle(isActive("/games"))}>Catalogue</Link>
            <Link to="/play/arena" style={navLinkStyle(isActive("/play"))}>Multiplayer Arena</Link>
            <Link to="/library" style={navLinkStyle(isActive("/library"))}>Library</Link>
            <Link to="/wishlist" style={navLinkStyle(isActive("/wishlist"))}>Wishlist</Link>
            <Link to="/achievements" style={navLinkStyle(isActive("/achievements"))}>Achievements</Link>
            <Link to="/leaderboard" style={navLinkStyle(isActive("/leaderboard"))}>Leaderboard</Link>
            <Link to="/groups" style={navLinkStyle(isActive("/groups"))}>Clans</Link>
            <Link to="/news" style={navLinkStyle(isActive("/news"))}>News</Link>
            <Link to="/experiments" style={navLinkStyle(isActive("/experiments"))}>Network Lab</Link>
            {user.role === "ADMIN" && (
              <Link to="/admin" style={{ ...navLinkStyle(isActive("/admin")), color: "#ec4899" }}>
                Admin
              </Link>
            )}
          </nav>

          {/* User Profile, Notifications & Logout */}
          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <NotificationBell />

            <Link to="/profile" style={userBadgeStyle}>
              <div style={avatarCircleStyle}>
                {user.username.charAt(0).toUpperCase()}
              </div>
              <div style={{ display: "flex", flexDirection: "column" }}>
                <span style={{ fontSize: 13, fontWeight: 700, color: "#f8fafc" }}>{user.username}</span>
                <span style={{ fontSize: 10, color: user.role === "ADMIN" ? "#ec4899" : "#00f2fe", fontWeight: 700 }}>
                  {user.role ?? "PLAYER"}
                </span>
              </div>
            </Link>

            <button onClick={logout} style={logoutBtnStyle} title="Log out">
              Log out
            </button>
          </div>
        </div>
      </header>

      <main style={{ paddingBottom: 60 }}>
        <Outlet />
      </main>

      {/* Persistent Steam-style Friends & Chat overlay */}
      <FriendsDrawer />
    </div>
  );
}

const navHeaderStyle: React.CSSProperties = {
  background: "rgba(11, 18, 32, 0.95)",
  backdropFilter: "blur(12px)",
  borderBottom: "1px solid #1e293b",
  position: "sticky",
  top: 0,
  zIndex: 100,
};

const navInnerStyle: React.CSSProperties = {
  maxWidth: 1360,
  margin: "0 auto",
  padding: "12px 20px",
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: 20,
};

const logoStyle: React.CSSProperties = {
  fontSize: 20,
  fontWeight: 900,
  letterSpacing: 2,
  color: "#f8fafc",
  textDecoration: "none",
  display: "flex",
  alignItems: "center",
  gap: 2,
};

const logoDotStyle: React.CSSProperties = {
  fontSize: 10,
  color: "#00f2fe",
  marginLeft: 2,
};

const navLinksStyle: React.CSSProperties = {
  display: "flex",
  gap: 18,
  alignItems: "center",
  flexWrap: "wrap",
};

const navLinkStyle = (active: boolean): React.CSSProperties => ({
  color: active ? "#00f2fe" : "#94a3b8",
  fontWeight: active ? 700 : 500,
  textDecoration: "none",
  fontSize: 13,
  padding: "4px 8px",
  borderRadius: 6,
  background: active ? "rgba(0, 242, 254, 0.1)" : "transparent",
  transition: "all 0.15s ease",
});

const userBadgeStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 8,
  textDecoration: "none",
  padding: "4px 10px",
  borderRadius: 20,
  background: "#1e293b",
  border: "1px solid #334155",
};

const avatarCircleStyle: React.CSSProperties = {
  width: 26,
  height: 26,
  borderRadius: 13,
  background: "linear-gradient(135deg, #00f2fe 0%, #7b2cbf 100%)",
  color: "#0f172a",
  fontWeight: 800,
  fontSize: 12,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
};

const logoutBtnStyle: React.CSSProperties = {
  padding: "6px 12px",
  borderRadius: 6,
  border: "1px solid #334155",
  background: "transparent",
  color: "#94a3b8",
  cursor: "pointer",
  fontSize: 12,
  fontWeight: 600,
};
