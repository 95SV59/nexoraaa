import { useEffect, useState } from "react";
import type { NetworkControllerSnapshot } from "../../types/multiplayer";
import { api } from "../../services/api";

interface Props {
  snapshot: NetworkControllerSnapshot | null;
}

interface SystemHealth {
  status: string;
  telemetryQueueBacklog: number;
  telemetryQueueCapacity: number;
  activeTrackedRooms: number;
}

export function NetworkStatusPanel({ snapshot }: Props) {
  const [history, setHistory] = useState<NetworkControllerSnapshot[]>([]);
  const [scrubberIndex, setScrubberIndex] = useState<number | null>(null);
  const [isPaused, setIsPaused] = useState(false);
  const [activeTab, setActiveTab] = useState<"overview" | "risks" | "whatif">("overview");
  const [systemHealth, setSystemHealth] = useState<SystemHealth | null>(null);

  // Buffer recent snapshots for session scrubber
  useEffect(() => {
    if (!snapshot) return;
    setHistory((prev) => {
      const updated = [...prev, snapshot];
      return updated.length > 30 ? updated.slice(updated.length - 30) : updated;
    });
  }, [snapshot]);

  // Periodic operational health poll
  useEffect(() => {
    let mounted = true;
    const fetchHealth = async () => {
      try {
        const res = await api.get<SystemHealth>("/network-lab/system-health");
        if (mounted) setSystemHealth(res.data);
      } catch {
        // Lab health is optional in solo dev mode
      }
    };
    fetchHealth();
    const interval = setInterval(fetchHealth, 5000);
    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, []);

  const activeSnapshot =
    scrubberIndex !== null && history[scrubberIndex]
      ? history[scrubberIndex]
      : snapshot;

  if (!activeSnapshot) {
    return (
      <div style={styles.panel}>
        <p style={styles.dim}>Gathering telemetry & awaiting initial PWNC controller cycle…</p>
      </div>
    );
  }

  const { prediction, risks = [], candidates = [], decision } = activeSnapshot;
  const atRisk = risks.filter((r) => r.risk === "HIGH");
  const acting = decision?.actionType !== "NO_ACTION" && decision?.confidenceGatePassed;

  return (
    <div style={styles.panel}>
      {/* Top Header & Tab Controls */}
      <div style={styles.header}>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <span style={styles.title}>PWNC Live Controller HUD</span>
          {systemHealth && (
            <span style={styles.healthBadge}>
              Queue: {systemHealth.telemetryQueueBacklog}/{systemHealth.telemetryQueueCapacity} | Sys: {systemHealth.status}
            </span>
          )}
        </div>

        <div style={styles.tabs}>
          <button
            onClick={() => setActiveTab("overview")}
            style={{ ...styles.tabBtn, ...(activeTab === "overview" ? styles.tabBtnActive : {}) }}
          >
            Overview
          </button>
          <button
            onClick={() => setActiveTab("risks")}
            style={{ ...styles.tabBtn, ...(activeTab === "risks" ? styles.tabBtnActive : {}) }}
          >
            Risks ({risks.length})
          </button>
          <button
            onClick={() => setActiveTab("whatif")}
            style={{ ...styles.tabBtn, ...(activeTab === "whatif" ? styles.tabBtnActive : {}) }}
          >
            What-If Sandbox ({candidates.length})
          </button>
        </div>
      </div>

      {/* Main Tab Content */}
      {activeTab === "overview" && (
        <>
          <div style={styles.row}>
            <Metric label="Forecasted RTT" value={`${prediction.rttMs.toFixed(0)} ms`} />
            <Metric label="Jitter" value={`${prediction.jitterMs.toFixed(0)} ms`} />
            <Metric label="Loss" value={`${prediction.lossPct.toFixed(1)}%`} />
            <Metric label="Confidence Gate" value={`${(prediction.confidence * 100).toFixed(0)}%`} />
            <Metric label="Horizon" value={`${prediction.horizonMs} ms`} />
            <Metric label="Broadcast Rate" value={`1/${decision?.appliedBroadcastEveryNTicks || 1} ticks`} />
          </div>

          <div
            style={{
              ...styles.status,
              background: acting ? "#065f46" : atRisk.length > 0 ? "#854d0e" : "#1e293b",
              border: acting ? "1px solid #10b981" : "1px solid #334155",
            }}
          >
            {acting
              ? `Action Active: ${decision.actionType} (${(decision.targetElementNames || []).join(", ")}) — Broadcasting every ${decision.appliedBroadcastEveryNTicks} tick(s)`
              : atRisk.length > 0
              ? `${atRisk.length} element(s) at risk — holding baseline (${decision?.reason?.slice(0, 70)}…)`
              : "Network within consistency tolerances — full 30 Hz broadcast maintained"}
          </div>
        </>
      )}

      {activeTab === "risks" && (
        <div style={styles.tableContainer}>
          <table style={styles.table}>
            <thead>
              <tr style={styles.thRow}>
                <th style={styles.th}>State Element</th>
                <th style={styles.th}>Criticality</th>
                <th style={styles.th}>Risk Level</th>
                <th style={styles.th}>Predicted Staleness</th>
                <th style={styles.th}>Divergence Est.</th>
                <th style={styles.th}>Violation Reason</th>
              </tr>
            </thead>
            <tbody>
              {risks.map((r, idx) => (
                <tr key={idx} style={{ ...styles.tr, background: r.risk === "HIGH" ? "rgba(239, 68, 68, 0.1)" : "transparent" }}>
                  <td style={styles.tdBold}>{r.elementName}</td>
                  <td style={styles.td}>{r.priority}</td>
                  <td style={{ ...styles.td, color: r.risk === "HIGH" ? "#ef4444" : "#10b981", fontWeight: 700 }}>
                    {r.risk}
                  </td>
                  <td style={styles.td}>{r.predictedStalenessMs?.toFixed(0) || 0} ms</td>
                  <td style={styles.td}>{r.predictedDivergenceEstimate?.toFixed(2) || "0.00"} units</td>
                  <td style={styles.tdDim}>{r.reason || "Within limits"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === "whatif" && (
        <div style={styles.tableContainer}>
          <table style={styles.table}>
            <thead>
              <tr style={styles.thRow}>
                <th style={styles.th}>Candidate Strategy</th>
                <th style={styles.th}>Consistency Cost</th>
                <th style={styles.th}>Network Cost</th>
                <th style={styles.th}>Processing Overhead</th>
                <th style={styles.th}>Total J(a) Score</th>
                <th style={styles.th}>Decision Status</th>
              </tr>
            </thead>
            <tbody>
              {candidates.map((c, idx) => {
                const isSelected = decision?.actionType === c.actionType;
                return (
                  <tr key={idx} style={{ ...styles.tr, background: isSelected ? "rgba(16, 185, 129, 0.15)" : "transparent" }}>
                    <td style={styles.tdBold}>{c.actionType}</td>
                    <td style={styles.td}>{c.consistencyCost?.toFixed(2) || "0.00"}</td>
                    <td style={styles.td}>{c.networkCost?.toFixed(2) || "0.00"}</td>
                    <td style={styles.td}>{(c.processingCost + c.controlOverhead)?.toFixed(2) || "0.00"}</td>
                    <td style={{ ...styles.tdBold, color: isSelected ? "#34d399" : "#e2e8f0" }}>
                      {c.totalScore?.toFixed(2) || "0.00"}
                    </td>
                    <td style={styles.td}>
                      {isSelected ? (
                        <span style={styles.selectedBadge}>SELECTED</span>
                      ) : (
                        <span style={styles.evaluatedBadge}>EVALUATED</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Historical Session Replay Scrubber */}
      {history.length > 1 && (
        <div style={styles.scrubberContainer}>
          <div style={styles.scrubberControls}>
            <button
              onClick={() => {
                setIsPaused(!isPaused);
                if (isPaused) setScrubberIndex(null);
                else setScrubberIndex(history.length - 1);
              }}
              style={styles.pauseBtn}
            >
              {isPaused ? "▶ Resume Live" : "⏸ Pause Replay"}
            </button>
            <span style={styles.scrubberLabel}>
              {scrubberIndex !== null
                ? `Replaying Cycle ${scrubberIndex + 1} of ${history.length} (${new Date(activeSnapshot.timestamp).toLocaleTimeString()})`
                : `Live Stream: ${history.length} cycles recorded`}
            </span>
          </div>

          <input
            type="range"
            min={0}
            max={history.length - 1}
            value={scrubberIndex !== null ? scrubberIndex : history.length - 1}
            onChange={(e) => {
              setIsPaused(true);
              setScrubberIndex(parseInt(e.target.value, 10));
            }}
            style={styles.slider}
          />
        </div>
      )}
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div style={styles.metric}>
      <div style={styles.metricValue}>{value}</div>
      <div style={styles.metricLabel}>{label}</div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  panel: {
    maxWidth: 960, margin: "8px auto 0", padding: 14, borderRadius: 12,
    background: "#0f172a", border: "1px solid #1e293b", fontFamily: "sans-serif", color: "#e2e8f0",
  },
  header: {
    display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12,
    borderBottom: "1px solid #1e293b", paddingBottom: 8, flexWrap: "wrap", gap: 8,
  },
  title: { fontSize: 14, fontWeight: 700, color: "#f8fafc", letterSpacing: "0.02em" },
  healthBadge: {
    fontSize: 10, padding: "2px 8px", borderRadius: 4, background: "#1e293b",
    color: "#38bdf8", border: "1px solid #0284c7", fontWeight: 600,
  },
  tabs: { display: "flex", gap: 6 },
  tabBtn: {
    padding: "4px 10px", fontSize: 11, fontWeight: 600, borderRadius: 6,
    background: "#1e293b", color: "#94a3b8", border: "1px solid transparent", cursor: "pointer",
  },
  tabBtnActive: { background: "#3b82f6", color: "#ffffff", borderColor: "#60a5fa" },
  row: { display: "flex", justifyContent: "space-around", marginBottom: 10, flexWrap: "wrap", gap: 6 },
  metric: { textAlign: "center", minWidth: 80 },
  metricValue: { fontSize: 16, fontWeight: 700, color: "#f1f5f9" },
  metricLabel: { fontSize: 10, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.04em" },
  status: { padding: "8px 12px", borderRadius: 8, fontSize: 12, textAlign: "center", fontWeight: 500 },
  dim: { color: "#94a3b8", textAlign: "center", margin: 0, fontSize: 13 },
  tableContainer: { overflowX: "auto", margin: "6px 0 10px" },
  table: { width: "100%", borderCollapse: "collapse", fontSize: 12 },
  thRow: { borderBottom: "1px solid #334155", textAlign: "left" },
  th: { padding: "6px 8px", color: "#94a3b8", fontWeight: 600, fontSize: 11 },
  tr: { borderBottom: "1px solid #1e293b" },
  td: { padding: "6px 8px", color: "#cbd5e1" },
  tdBold: { padding: "6px 8px", color: "#f8fafc", fontWeight: 600 },
  tdDim: { padding: "6px 8px", color: "#64748b", fontSize: 11 },
  selectedBadge: {
    padding: "2px 6px", borderRadius: 4, background: "#059669", color: "#ffffff",
    fontSize: 9, fontWeight: 700,
  },
  evaluatedBadge: {
    padding: "2px 6px", borderRadius: 4, background: "#334155", color: "#94a3b8",
    fontSize: 9, fontWeight: 600,
  },
  scrubberContainer: {
    marginTop: 10, paddingTop: 8, borderTop: "1px solid #1e293b",
    display: "flex", flexDirection: "column", gap: 6,
  },
  scrubberControls: { display: "flex", justifyContent: "space-between", alignItems: "center" },
  pauseBtn: {
    padding: "3px 8px", fontSize: 10, fontWeight: 600, borderRadius: 4,
    background: "#334155", color: "#f8fafc", border: "1px solid #475569", cursor: "pointer",
  },
  scrubberLabel: { fontSize: 11, color: "#94a3b8" },
  slider: { width: "100%", cursor: "pointer", accentColor: "#3b82f6" },
};
