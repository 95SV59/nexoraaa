import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { styles } from "./LoginPage";

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await register({ username, email, password });
      navigate("/");
    } catch (err: any) {
      if (err?.response?.data?.message) {
        setError(err.response.data.message);
      } else if (err?.message === "Network Error" || !err?.response) {
        setError("Cannot connect to backend server. Make sure Spring Boot is running on port 8080.");
      } else {
        setError("Could not register — check your details and try again.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.page}>
      <form onSubmit={handleSubmit} style={styles.card}>
        <h1 style={{ marginBottom: 4 }}>Create an account</h1>
        <input style={styles.input} placeholder="Username" value={username} onChange={(e) => setUsername(e.target.value)} />
        <input style={styles.input} type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <input style={styles.input} type="password" placeholder="Password (min. 8 characters)" value={password} onChange={(e) => setPassword(e.target.value)} />
        {error && <p style={{ color: "#f87171", fontSize: 14 }}>{error}</p>}
        <button style={styles.button} disabled={loading} type="submit">
          {loading ? "Creating account…" : "Register"}
        </button>
        <p style={styles.dim}>
          Already have an account? <Link to="/login" style={{ color: "#818cf8" }}>Log in</Link>
        </p>
      </form>
    </div>
  );
}
