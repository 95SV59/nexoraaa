import { useState } from "react";
import { labService } from "../../services/labService";

interface Props {
  roomCode: string;
}

const PRESETS = ["EXCELLENT", "GOOD", "MODERATE", "POOR", "UNSTABLE"] as const;

/** Phase 16 control surface: pick a preset (or turn it off) to inject artificial
 *  latency/jitter/loss into this room's live traffic and watch NetworkStatusPanel
 *  react. This is what makes the controller's adaptation actually visible on a
 *  healthy localhost connection, which otherwise never has anything to react to. */
export function NetworkLabPanel({ roomCode }: Props) {
  const [active, setActive] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function apply(profileName: string) {
    setBusy(true);
    try {
      await labService.setProfile(roomCode, { profileName });
      setActive(profileName);
    } finally {
      setBusy(false);
    }
  }

  async function turnOff() {
    setBusy(true);
    try {
      await labService.clearProfile(roomCode);
      setActive(null);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div style={styles.panel}>
      <span style={styles.label}>Network Lab:</span>
      {PRESETS.map((p) => (
        <button
          key={p}
          disabled={busy}
          onClick={() => apply(p)}
          style={{ ...styles.button, ...(active === p ? styles.buttonActive : {}) }}
        >
          {p}
        </button>
      ))}
      <button
        disabled={busy}
        onClick={turnOff}
        style={{ ...styles.button, ...(active === null ? styles.buttonActive : {}) }}
      >
        Off
      </button>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  panel: {
    maxWidth: 960, margin: "8px auto 0", padding: "8px 12px", borderRadius: 10,
    background: "#111827", display: "flex", alignItems: "center", gap: 6, flexWrap: "wrap",
    fontFamily: "sans-serif",
  },
  label: { color: "#94a3b8", fontSize: 12, marginRight: 4 },
  button: {
    padding: "4px 10px", borderRadius: 6, border: "1px solid #334155",
    background: "transparent", color: "#e2e8f0", fontSize: 12, cursor: "pointer",
  },
  buttonActive: { background: "#4f46e5", borderColor: "#4f46e5" },
};
