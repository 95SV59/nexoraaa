import React, { Component, ErrorInfo, ReactNode } from "react";

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Uncaught NEXORA React error:", error, errorInfo);
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null });
    window.location.href = "/";
  };

  private handleClearAndReload = () => {
    localStorage.clear();
    window.location.href = "/login";
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div style={styles.container}>
          <div style={styles.card}>
            <div style={styles.badge}>SYSTEM EXCEPTION</div>
            <h1 style={styles.title}>Something went wrong</h1>
            <p style={styles.subtitle}>
              An unexpected interface error occurred during rendering.
            </p>
            {this.state.error && (
              <pre style={styles.errorBox}>
                {this.state.error.name}: {this.state.error.message}
              </pre>
            )}
            <div style={styles.buttonGroup}>
              <button onClick={this.handleReset} style={styles.primaryButton}>
                Reload Application
              </button>
              <button onClick={this.handleClearAndReload} style={styles.secondaryButton}>
                Reset Session & Login
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    minHeight: "100vh",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    background: "#060913",
    padding: 20,
    fontFamily: "sans-serif",
  },
  card: {
    maxWidth: 520,
    width: "100%",
    padding: 32,
    borderRadius: 16,
    background: "#0f172a",
    border: "1px solid #1e293b",
    color: "#f8fafc",
    boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.7)",
  },
  badge: {
    display: "inline-block",
    fontSize: 10,
    fontWeight: 800,
    color: "#ef4444",
    background: "rgba(239, 68, 68, 0.15)",
    padding: "4px 8px",
    borderRadius: 4,
    letterSpacing: 1,
    marginBottom: 12,
  },
  title: {
    fontSize: 22,
    margin: "0 0 8px 0",
    fontWeight: 700,
  },
  subtitle: {
    fontSize: 14,
    color: "#94a3b8",
    margin: "0 0 20px 0",
    lineHeight: 1.5,
  },
  errorBox: {
    padding: 14,
    background: "#020617",
    border: "1px solid #334155",
    borderRadius: 8,
    color: "#fca5a5",
    fontSize: 12,
    overflowX: "auto",
    marginBottom: 24,
    whiteSpace: "pre-wrap",
    wordBreak: "break-all",
  },
  buttonGroup: {
    display: "flex",
    gap: 12,
    flexWrap: "wrap",
  },
  primaryButton: {
    padding: "10px 18px",
    borderRadius: 8,
    border: "none",
    background: "#00f2fe",
    color: "#0f172a",
    fontWeight: 700,
    fontSize: 13,
    cursor: "pointer",
  },
  secondaryButton: {
    padding: "10px 18px",
    borderRadius: 8,
    border: "1px solid #334155",
    background: "transparent",
    color: "#cbd5e1",
    fontWeight: 600,
    fontSize: 13,
    cursor: "pointer",
  },
};
