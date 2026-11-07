import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { ComparisonReport } from "../../types/experiments";

interface Props {
  report: ComparisonReport;
}

/** The three metrics that share a 0-100% scale, so they can sit on one axis honestly.
 *  RTT/jitter are ms and are shown in the table instead — they're set by the Network Lab
 *  profile, not by the controller, so they should match between the two runs and are
 *  there as a sanity check that the comparison was run under identical conditions. */
export function ComparisonChart({ report }: Props) {
  const { baseline, proposed } = report;
  const data = [
    {
      metric: "Broadcasts sent (% of ticks)",
      Baseline: baseline.broadcastRatioPct ?? 0,
      Proposed: proposed.broadcastRatioPct ?? 0,
    },
    {
      metric: "Controller intervened (% of cycles)",
      Baseline: baseline.interventionRatePct ?? 0,
      Proposed: proposed.interventionRatePct ?? 0,
    },
    {
      metric: "Confidence gate pass rate (%)",
      Baseline: baseline.confidenceGatePassRatePct ?? 0,
      Proposed: proposed.confidenceGatePassRatePct ?? 0,
    },
    {
      metric: "Avg packet loss (%)",
      Baseline: baseline.avgPacketLossPct ?? 0,
      Proposed: proposed.avgPacketLossPct ?? 0,
    },
  ];

  return (
    <div style={{ width: "100%", height: 300 }}>
      <ResponsiveContainer>
        <BarChart data={data} margin={{ top: 8, right: 16, left: 0, bottom: 8 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
          <XAxis dataKey="metric" tick={{ fill: "#94a3b8", fontSize: 12 }} />
          <YAxis domain={[0, 100]} tick={{ fill: "#94a3b8", fontSize: 12 }} unit="%" />
          <Tooltip formatter={(v) => (typeof v === "number" ? `${v.toFixed(1)}%` : String(v))} />
          <Legend />
          <Bar dataKey="Baseline" fill="#94a3b8" />
          <Bar dataKey="Proposed" fill="#4f46e5" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
