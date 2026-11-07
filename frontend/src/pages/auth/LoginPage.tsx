import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login({ username, password });
      navigate("/");
    } catch (err: any) {
      if (err?.response?.data?.message) {
        setError(err.response.data.message);
      } else if (err?.message === "Network Error" || !err?.response) {
        setError("Cannot connect to backend server. Make sure Spring Boot is running on port 8080.");
      } else {
        setError("Invalid username or password.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.page}>
      <form onSubmit={handleSubmit} style={styles.card}>
        <h1 style={{ marginBottom: 4 }}>NEXORA</h1>
        <p style={styles.dim}>Log in to play.</p>
        <input style={styles.input} placeholder="Username" value={username} onChange={(e) => setUsername(e.target.value)} />
        <input style={styles.input} type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} />
        {error && <p style={{ color: "#f87171", fontSize: 14 }}>{error}</p>}
        <button style={styles.button} disabled={loading} type="submit">
          {loading ? "Logging in…" : "Log in"}
        </button>
        <p style={styles.dim}>
          No account? <Link to="/register" style={{ color: "#818cf8" }}>Register</Link>
        </p>
      </form>
    </div>
  );
}

export const styles: Record<string, React.CSSProperties> = {
  page: { minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "#0f172a", fontFamily: "sans-serif" },
  card: { width: 340, padding: 28, borderRadius: 12, background: "#1e293b", color: "#e2e8f0", display: "flex", flexDirection: "column", gap: 10 },
  dim: { color: "#94a3b8", fontSize: 13, margin: 0 },
  input: { padding: "10px 12px", borderRadius: 8, border: "1px solid #334155", background: "#0f172a", color: "#e2e8f0" },
  button: { padding: "10px 16px", borderRadius: 8, border: "none", background: "#4f46e5", color: "white", fontWeight: 600, cursor: "pointer" },
};
