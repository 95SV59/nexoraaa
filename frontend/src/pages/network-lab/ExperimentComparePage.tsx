import { useEffect, useState } from "react";
import { experimentService } from "../../services/experimentService";
import { ComparisonChart } from "../../components/network/ComparisonChart";
import type { BatchEvaluationResult, ComparisonReport, SessionListItem, SessionSummary } from "../../types/experiments";

function fmt(v: number | null, digits = 1, suffix = ""): string {
  return v === null || v === undefined ? "—" : `${v.toFixed(digits)}${suffix}`;
}

function conditionsLookMismatched(a: SessionSummary, b: SessionSummary): boolean {
  if (a.avgRttMs === null || b.avgRttMs === null) return false;
  const larger = Math.max(a.avgRttMs, b.avgRttMs);
  return larger > 0 && Math.abs(a.avgRttMs - b.avgRttMs) / larger > 0.25;
}

export function ExperimentComparePage() {
  const [sessions, setSessions] = useState<SessionListItem[]>([]);
  const [baselineId, setBaselineId] = useState("");
  const [proposedId, setProposedId] = useState("");
  const [report, setReport] = useState<ComparisonReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Automated benchmark state
  const [selectedModel, setSelectedModel] = useState<"LINEAR_TREND" | "PERSISTENCE" | "EWMA">("LINEAR_TREND");
  const [benchmarkResult, setBenchmarkResult] = useState<BatchEvaluationResult | null>(null);
  const [benchmarkLoading, setBenchmarkLoading] = useState(false);

  useEffect(() => {
    experimentService.listSessions(20).then(setSessions).catch(() => setError("Could not load sessions. Is the backend running?"));
  }, []);

  async function runComparison() {
    setError(null);
    setReport(null);
    setLoading(true);
    try {
      setReport(await experimentService.compare(Number(baselineId), Number(proposedId)));
    } catch {
      setError("Comparison failed — check both session IDs exist.");
    } finally {
      setLoading(false);
    }
  }

  async function runBatchBenchmark() {
    setBenchmarkLoading(true);
    setError(null);
    try {
      const res = await experimentService.runBatchEvaluation({
        predictionModel: selectedModel,
        profiles: ["EXCELLENT", "GOOD", "MODERATE", "POOR", "UNSTABLE"],
        durationSec: 60,
      });
      setBenchmarkResult(res);
    } catch (err: any) {
      setError("Automated benchmark failed to execute.");
    } finally {
      setBenchmarkLoading(false);
    }
  }

  const canCompare = baselineId.trim() !== "" && proposedId.trim() !== "" && !loading;

  return (
    <div style={styles.page}>
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ margin: "0 0 8px 0", fontSize: 28, color: "#f8fafc", fontWeight: 800 }}>
          PWNC Empirical Research Laboratory
        </h1>
        <p style={{ margin: 0, color: "#94a3b8", fontSize: 14, lineHeight: 1.5 }}>
          Compare real-time multiplayer telemetry between <b>BASELINE</b> (uncontrolled TCP/WS) and <b>PROPOSED</b> (Predictive What-If Network Controller) under identical network degradation profiles.
        </p>
      </div>

      {/* Automated Multi-Profile Empirical Benchmark Section */}
      <div style={styles.benchmarkCard}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 12, marginBottom: 14 }}>
          <div>
            <h3 style={{ margin: "0 0 4px 0", color: "#00f2fe", fontSize: 16, fontWeight: 700 }}>
              ⚡ Automated Multi-Profile Scientific Benchmark
            </h3>
            <p style={{ margin: 0, color: "#94a3b8", fontSize: 12 }}>
              Execute virtual clock multi-trial evaluation across all 5 standard profiles with selected prediction model.
            </p>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <select
              value={selectedModel}
              onChange={(e) => setSelectedModel(e.target.value as any)}
              style={styles.select}
            >
              <option value="LINEAR_TREND">Linear Trend (OLS Slope over H)</option>
              <option value="PERSISTENCE">Persistence (Naive Last-Value)</option>
              <option value="EWMA">EWMA Baseline (α = 0.3)</option>
            </select>

            <button
              style={styles.benchmarkBtn}
              disabled={benchmarkLoading}
              onClick={runBatchBenchmark}
            >
              {benchmarkLoading ? "Running Benchmark..." : "🚀 Run 5-Profile Benchmark"}
            </button>
          </div>
        </div>

        {benchmarkResult && (
          <div style={{ marginTop: 16 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
              <span style={{ fontSize: 12, color: "#00ff87", fontWeight: 700 }}>
                BENCHMARK RESULTS — MODEL: {benchmarkResult.model} (Tested at {new Date(benchmarkResult.timestamp).toLocaleTimeString()})
              </span>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 12 }}>
              {benchmarkResult.profiles.map((p) => (
                <div key={p.profileName} style={styles.profileCard}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                    <span style={{ fontWeight: 800, fontSize: 13, color: "#f8fafc" }}>{p.profileName}</span>
                    <span style={{ fontSize: 10, background: "#1e293b", color: "#94a3b8", padding: "2px 6px", borderRadius: 4 }}>
                      +{p.latencyMs}ms / {p.packetLossPct}% loss
                    </span>
                  </div>

                  <div style={{ fontSize: 11, color: "#94a3b8", marginBottom: 4 }}>
                    Broadcast Reduction: <b style={{ color: "#00ff87" }}>{p.broadcastReductionPct}%</b>
                  </div>
                  <div style={{ fontSize: 11, color: "#94a3b8", marginBottom: 4 }}>
                    Ratio: {p.proposedBroadcastRatioPct}% vs 100%
                  </div>
                  <div style={{ fontSize: 11, color: "#94a3b8", marginBottom: 4 }}>
                    Interventions: {p.interventionCount}/{p.totalCycles} ({p.confidenceGatePassRatePct}% gate pass)
                  </div>
                  <div style={{ fontSize: 11, color: "#94a3b8" }}>
                    Avg RTT: {p.proposedAvgRttMs}ms (vs {p.baselineAvgRttMs}ms)
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <div style={{ marginTop: 24, marginBottom: 12, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h3 style={{ margin: 0, color: "#f8fafc", fontSize: 16 }}>Match Session Roster</h3>
        <span style={{ fontSize: 12, color: "#64748b" }}>Pick 1 BASELINE and 1 PROPOSED session to compare</span>
      </div>

      {sessions.length === 0 ? (
        <p style={styles.dim}>No sessions yet — finish a match first.</p>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Session ID</th>
                <th style={styles.th}>Controller Mode</th>
                <th style={styles.th}>Started</th>
                <th style={styles.th}>Status</th>
                <th style={styles.th}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {sessions.map((s) => (
                <tr key={s.sessionId}>
                  <td style={styles.td}>#{s.sessionId}</td>
                  <td style={styles.td}>
                    <span
                      style={{
                        padding: "2px 8px",
                        borderRadius: 4,
                        fontSize: 11,
                        fontWeight: 700,
                        background: s.controllerMode === "PROPOSED" ? "rgba(0, 242, 254, 0.15)" : "rgba(245, 158, 11, 0.15)",
                        color: s.controllerMode === "PROPOSED" ? "#00f2fe" : "#f59e0b",
                      }}
                    >
                      {s.controllerMode}
                    </span>
                  </td>
                  <td style={styles.td}>{new Date(s.startedAt).toLocaleString()}</td>
                  <td style={styles.td}>{s.endedAt ? "finished" : "in progress"}</td>
                  <td style={styles.td}>
                    <button style={styles.smallButton} onClick={() => setBaselineId(String(s.sessionId))}>
                      as baseline
                    </button>{" "}
                    <button style={styles.smallButton} onClick={() => setProposedId(String(s.sessionId))}>
                      as proposed
                    </button>{" "}
                    <button
                      style={{ ...styles.smallButton, borderColor: "#00f2fe", color: "#00f2fe" }}
                      onClick={() => experimentService.downloadSessionCsv(s.sessionId)}
                    >
                      📥 CSV
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Manual Pair Comparison Input */}
      <div style={{ display: "flex", gap: 10, margin: "20px 0", alignItems: "center", flexWrap: "wrap" }}>
        <input
          style={styles.input}
          placeholder="Baseline session ID (e.g. 1)"
          value={baselineId}
          onChange={(e) => setBaselineId(e.target.value)}
        />
        <input
          style={styles.input}
          placeholder="Proposed session ID (e.g. 2)"
          value={proposedId}
          onChange={(e) => setProposedId(e.target.value)}
        />
        <button style={styles.button} disabled={!canCompare} onClick={runComparison}>
          {loading ? "Comparing…" : "Compare Sessions"}
        </button>
      </div>

      {error && <p style={{ color: "#f87171", background: "rgba(239, 68, 68, 0.1)", padding: 10, borderRadius: 8 }}>{error}</p>}

      {report && (
        <div style={{ marginTop: 24 }}>
          {report.baseline.controllerMode !== "BASELINE" && (
            <p style={styles.warn}>Session #{report.baseline.sessionId} was recorded as {report.baseline.controllerMode ?? "unknown"}, not BASELINE.</p>
          )}
          {report.proposed.controllerMode !== "PROPOSED" && (
            <p style={styles.warn}>Session #{report.proposed.sessionId} was recorded as {report.proposed.controllerMode ?? "unknown"}, not PROPOSED.</p>
          )}
          {conditionsLookMismatched(report.baseline, report.proposed) && (
            <p style={styles.warn}>
              Average RTT differs significantly between the two runs. The controller does not alter static network latency — the Network Lab does — so
              these runs were likely not recorded under the identical network profile.
            </p>
          )}

          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 12, marginBottom: 16 }}>
            <div>
              <h3 style={{ margin: "0 0 4px 0", fontSize: 20, color: "#f8fafc" }}>Empirical Finding</h3>
              <p style={{ margin: 0, fontSize: 16, color: "#00ff87", fontWeight: 700 }}>
                {report.broadcastReductionPct === null
                  ? "Broadcast counts are not available for one of these sessions."
                  : `PROPOSED reduced network broadcast load by ${fmt(report.broadcastReductionPct, 1)} percentage points compared to BASELINE.`}
              </p>
            </div>

            <div style={{ display: "flex", gap: 8 }}>
              <button
                style={styles.exportBtn}
                onClick={() => experimentService.downloadComparisonCsv(Number(baselineId), Number(proposedId))}
              >
                📥 Export Comparison CSV
              </button>
            </div>
          </div>

          <ComparisonChart report={report} />

          <table style={{ ...styles.table, marginTop: 20 }}>
            <thead>
              <tr>
                <th style={styles.th}>Empirical Metric</th>
                <th style={styles.th}>Baseline #{report.baseline.sessionId}</th>
                <th style={styles.th}>Proposed #{report.proposed.sessionId}</th>
              </tr>
            </thead>
            <tbody>
              <Row label="Telemetry samples recorded" a={String(report.baseline.telemetrySampleCount)} b={String(report.proposed.telemetrySampleCount)} />
              <Row label="Avg RTT (Validation Sanity Check)" a={fmt(report.baseline.avgRttMs, 1, " ms")} b={fmt(report.proposed.avgRttMs, 1, " ms")} />
              <Row label="Avg Jitter" a={fmt(report.baseline.avgJitterMs, 1, " ms")} b={fmt(report.proposed.avgJitterMs, 1, " ms")} />
              <Row label="Avg Packet Loss" a={fmt(report.baseline.avgPacketLossPct, 1, "%")} b={fmt(report.proposed.avgPacketLossPct, 1, "%")} />
              <Row label="Avg Prediction Confidence" a={fmt(report.baseline.avgPredictionConfidence === null ? null : report.baseline.avgPredictionConfidence * 100, 1, "%")} b={fmt(report.proposed.avgPredictionConfidence === null ? null : report.proposed.avgPredictionConfidence * 100, 1, "%")} />
              <Row label="Avg |RTT Prediction Error|" a={fmt(report.baseline.avgAbsRttErrorMs, 1, " ms")} b={fmt(report.proposed.avgAbsRttErrorMs, 1, " ms")} />
              <Row label="Total Decision Cycles" a={String(report.baseline.totalDecisionCycles)} b={String(report.proposed.totalDecisionCycles)} />
              <Row label="Cycles Controller Recommended Action" a={fmt(report.baseline.interventionRatePct, 1, "%")} b={fmt(report.proposed.interventionRatePct, 1, "%")} />
              <Row label="Confidence Gate Pass Rate" a={fmt(report.baseline.confidenceGatePassRatePct, 1, "%")} b={fmt(report.proposed.confidenceGatePassRatePct, 1, "%")} />
              <Row label="Avg Consistency Cost (Lower is Better)" a={fmt(report.baseline.avgConsistencyCost, 2)} b={fmt(report.proposed.avgConsistencyCost, 2)} />
              <Row label="Avg What-If Score (Lower is Better)" a={fmt(report.baseline.avgTotalScore, 2)} b={fmt(report.proposed.avgTotalScore, 2)} />
              <Row label="Ticks / Broadcasts Sent" a={`${report.baseline.totalTicks ?? "—"} / ${report.baseline.totalBroadcasts ?? "—"}`} b={`${report.proposed.totalTicks ?? "—"} / ${report.proposed.totalBroadcasts ?? "—"}`} />
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function Row({ label, a, b }: { label: string; a: string; b: string }) {
  return (
    <tr>
      <td style={styles.td}>{label}</td>
      <td style={styles.td}>{a}</td>
      <td style={styles.td}>{b}</td>
    </tr>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: { maxWidth: 1040, margin: "0 auto", padding: "24px 20px 80px", fontFamily: "system-ui, -apple-system, sans-serif", color: "#e2e8f0", background: "#060913", minHeight: "100vh" },
  dim: { color: "#94a3b8" },
  warn: { color: "#fbbf24", background: "rgba(245, 158, 11, 0.15)", padding: "10px 14px", borderRadius: 8, border: "1px solid rgba(245, 158, 11, 0.3)", marginBottom: 12 },
  table: { width: "100%", borderCollapse: "collapse", background: "#0f172a", borderRadius: 8, overflow: "hidden" },
  th: { textAlign: "left", padding: "10px 12px", borderBottom: "1px solid #1e293b", color: "#94a3b8", fontWeight: 700, fontSize: 12, textTransform: "uppercase", letterSpacing: 0.5 },
  td: { padding: "10px 12px", borderBottom: "1px solid #1e293b", fontSize: 13, color: "#f8fafc" },
  input: { padding: "9px 12px", borderRadius: 8, border: "1px solid #334155", background: "#0f172a", color: "#f8fafc", fontSize: 13, outline: "none", minWidth: 200 },
  select: { padding: "8px 12px", borderRadius: 8, border: "1px solid #334155", background: "#0f172a", color: "#f8fafc", fontSize: 12, outline: "none" },
  button: { padding: "9px 18px", borderRadius: 8, border: "none", background: "linear-gradient(135deg, #00f2fe, #4facfe)", color: "#080d1a", fontWeight: 700, fontSize: 13, cursor: "pointer" },
  smallButton: { padding: "4px 10px", borderRadius: 6, border: "1px solid #334155", background: "rgba(15, 23, 42, 0.6)", color: "#cbd5e1", fontSize: 11, cursor: "pointer", fontWeight: 600 },
  benchmarkCard: { background: "#0b1120", border: "1px solid #1e293b", borderRadius: 12, padding: 18, marginBottom: 20, boxShadow: "0 8px 30px rgba(0,0,0,0.5)" },
  benchmarkBtn: { background: "linear-gradient(135deg, #00f2fe, #00c6ff)", border: "none", color: "#080d1a", fontWeight: 800, fontSize: 12, padding: "8px 16px", borderRadius: 8, cursor: "pointer" },
  profileCard: { background: "#0f172a", border: "1px solid #1e293b", borderRadius: 8, padding: 12 },
  exportBtn: { background: "rgba(0, 242, 254, 0.15)", border: "1px solid #00f2fe", color: "#00f2fe", fontWeight: 700, fontSize: 12, padding: "8px 14px", borderRadius: 8, cursor: "pointer" },
};
