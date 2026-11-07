import { useEffect, useRef } from "react";
import type { InputRequest, TagPlayerSnapshot, TagStateSnapshot } from "../../types/multiplayer";

interface Props {
  state: TagStateSnapshot | null;
  currentPlayerId: string;
  sendInput: (req: InputRequest) => void;
}

const WIDTH = 800;
const HEIGHT = 600;
const INPUT_SEND_HZ = 30;

interface SparkParticle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  color: string;
}

export function TagCanvas({ state, currentPlayerId, sendInput }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef(state);
  const sendInputRef = useRef(sendInput);
  const keysRef = useRef<Set<string>>(new Set());
  const sequenceRef = useRef(0);
  const particlesRef = useRef<SparkParticle[]>([]);

  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  useEffect(() => {
    sendInputRef.current = sendInput;
  }, [sendInput]);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "KeyW", "KeyA", "KeyS", "KeyD"].includes(e.code)) {
        e.preventDefault();
      }
      keysRef.current.add(e.code);
    };

    const onKeyUp = (e: KeyboardEvent) => {
      keysRef.current.delete(e.code);
    };

    window.addEventListener("keydown", onKeyDown);
    window.addEventListener("keyup", onKeyUp);
    return () => {
      window.removeEventListener("keydown", onKeyDown);
      window.removeEventListener("keyup", onKeyUp);
    };
  }, []);

  // Input transmission loop
  useEffect(() => {
    const interval = setInterval(() => {
      const keys = keysRef.current;
      let moveX = 0;
      let moveY = 0;

      if (keys.has("KeyW") || keys.has("ArrowUp")) moveY -= 1;
      if (keys.has("KeyS") || keys.has("ArrowDown")) moveY += 1;
      if (keys.has("KeyA") || keys.has("ArrowLeft")) moveX -= 1;
      if (keys.has("KeyD") || keys.has("ArrowRight")) moveX += 1;

      const mag = Math.hypot(moveX, moveY);
      if (mag > 0) {
        moveX /= mag;
        moveY /= mag;
      }

      sendInputRef.current({
        sequence: ++sequenceRef.current,
        clientTimestamp: Date.now(),
        moveX,
        moveY,
        aimAngleRad: 0,
        firing: false,
        actionPressed: false,
      });
    }, 1000 / INPUT_SEND_HZ);

    return () => clearInterval(interval);
  }, []);

  // Animation render loop
  useEffect(() => {
    let animId: number;

    const render = () => {
      const canvas = canvasRef.current;
      if (!canvas) return;
      const ctx = canvas.getContext("2d");
      if (!ctx) return;

      const current = stateRef.current;

      // 1. Cyber Arena Grid Floor
      ctx.fillStyle = "#070b14";
      ctx.fillRect(0, 0, WIDTH, HEIGHT);

      ctx.save();
      ctx.strokeStyle = "rgba(0, 242, 254, 0.05)";
      ctx.lineWidth = 1;
      const gridSize = 40;
      for (let x = 0; x < WIDTH; x += gridSize) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, HEIGHT);
        ctx.stroke();
      }
      for (let y = 0; y < HEIGHT; y += gridSize) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(WIDTH, y);
        ctx.stroke();
      }

      // Center Arena Zone
      ctx.strokeStyle = "rgba(0, 242, 254, 0.15)";
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(WIDTH / 2, HEIGHT / 2, 90, 0, Math.PI * 2);
      ctx.stroke();

      // Glowing Neon Perimeter Barrier
      ctx.shadowBlur = 12;
      ctx.shadowColor = "#00f2fe";
      ctx.strokeStyle = "#00f2fe";
      ctx.lineWidth = 3;
      ctx.strokeRect(30, 30, WIDTH - 60, HEIGHT - 60);

      // Warning Chevron Corners
      ctx.shadowBlur = 0;
      ctx.fillStyle = "rgba(245, 158, 11, 0.35)";
      ctx.fillRect(30, 30, 25, 6);
      ctx.fillRect(30, 30, 6, 25);
      ctx.fillRect(WIDTH - 55, 30, 25, 6);
      ctx.fillRect(WIDTH - 36, 30, 6, 25);
      ctx.fillRect(30, HEIGHT - 36, 25, 6);
      ctx.fillRect(30, HEIGHT - 55, 6, 25);
      ctx.fillRect(WIDTH - 55, HEIGHT - 36, 25, 6);
      ctx.fillRect(WIDTH - 36, HEIGHT - 55, 6, 25);
      ctx.restore();

      // 2. Render & Update Flame Sparks for IT player
      const particles = particlesRef.current;
      for (let i = particles.length - 1; i >= 0; i--) {
        const pt = particles[i];
        pt.x += pt.vx;
        pt.y += pt.vy;
        pt.life++;
        const alpha = Math.max(0, 1 - pt.life / pt.maxLife);

        ctx.save();
        ctx.globalAlpha = alpha;
        ctx.fillStyle = pt.color;
        ctx.shadowBlur = 6;
        ctx.shadowColor = pt.color;
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 3 * alpha, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        if (pt.life >= pt.maxLife) {
          particles.splice(i, 1);
        }
      }

      if (current) {
        // 3. Draw Players
        current.players.forEach((p: TagPlayerSnapshot) => {
          const isMe = p.playerId === currentPlayerId;

          // Spawn flame embers for IT player
          if (p.isIt && Math.random() < 0.6) {
            const angle = Math.random() * Math.PI * 2;
            const speed = 1 + Math.random() * 2;
            particlesRef.current.push({
              x: p.x,
              y: p.y,
              vx: Math.cos(angle) * speed,
              vy: Math.sin(angle) * speed - 1, // upward draft
              life: 1,
              maxLife: 15 + Math.random() * 10,
              color: Math.random() > 0.4 ? "#ef4444" : "#f59e0b",
            });
          }

          ctx.save();

          // Ground shadow
          ctx.fillStyle = "rgba(0, 0, 0, 0.5)";
          ctx.beginPath();
          ctx.ellipse(p.x, p.y + 4, 16, 8, 0, 0, Math.PI * 2);
          ctx.fill();

          // Immune Hex-Shield Pulse
          if (p.immune) {
            ctx.save();
            ctx.strokeStyle = "#38bdf8";
            ctx.shadowBlur = 10;
            ctx.shadowColor = "#38bdf8";
            ctx.lineWidth = 2.5;
            ctx.setLineDash([6, 4]);
            ctx.beginPath();
            ctx.arc(p.x, p.y, 25, 0, Math.PI * 2);
            ctx.stroke();
            ctx.restore();
          }

          // IT Hunter Fire Aura
          if (p.isIt) {
            const pulse = 24 + Math.sin(Date.now() / 140) * 5;
            ctx.save();
            ctx.fillStyle = "rgba(239, 68, 68, 0.25)";
            ctx.beginPath();
            ctx.arc(p.x, p.y, pulse, 0, Math.PI * 2);
            ctx.fill();

            ctx.strokeStyle = "#ef4444";
            ctx.shadowBlur = 12;
            ctx.shadowColor = "#ef4444";
            ctx.lineWidth = 2.5;
            ctx.beginPath();
            ctx.arc(p.x, p.y, 21, 0, Math.PI * 2);
            ctx.stroke();
            ctx.restore();
          }

          // Player Avatar Sphere with Gradient
          const playerGrad = ctx.createRadialGradient(p.x - 3, p.y - 3, 2, p.x, p.y, 16);
          if (p.isIt) {
            playerGrad.addColorStop(0, "#f87171");
            playerGrad.addColorStop(0.7, "#dc2626");
            playerGrad.addColorStop(1, "#7f1d1d");
          } else if (isMe) {
            playerGrad.addColorStop(0, "#38bdf8");
            playerGrad.addColorStop(0.7, "#0284c7");
            playerGrad.addColorStop(1, "#0c4a6e");
          } else {
            playerGrad.addColorStop(0, "#34d399");
            playerGrad.addColorStop(0.7, "#059669");
            playerGrad.addColorStop(1, "#064e3b");
          }

          ctx.fillStyle = playerGrad;
          ctx.beginPath();
          ctx.arc(p.x, p.y, 16, 0, Math.PI * 2);
          ctx.fill();

          ctx.strokeStyle = p.isIt ? "#fca5a5" : isMe ? "#e0f2fe" : "#a7f3d0";
          ctx.lineWidth = 2;
          ctx.stroke();

          // Visor / Core Eye
          ctx.fillStyle = "#ffffff";
          ctx.shadowBlur = 4;
          ctx.shadowColor = p.isIt ? "#ef4444" : "#00f2fe";
          ctx.beginPath();
          ctx.arc(p.x - 4, p.y - 2, 2.5, 0, Math.PI * 2);
          ctx.arc(p.x + 4, p.y - 2, 2.5, 0, Math.PI * 2);
          ctx.fill();
          ctx.shadowBlur = 0;

          // Overhead Status & Badge
          ctx.font = "bold 11px monospace";
          ctx.textAlign = "center";
          if (p.isIt) {
            ctx.fillStyle = "#ef4444";
            ctx.fillText("🔥 IT HUNTER", p.x, p.y - 30);
          } else if (p.immune) {
            ctx.fillStyle = "#38bdf8";
            ctx.fillText("🛡 IMMUNE", p.x, p.y - 30);
          }
          ctx.fillStyle = isMe ? "#00f2fe" : "#f8fafc";
          ctx.fillText(`${p.displayName} [${Math.round(p.timeAsItSec)}s]`, p.x, p.y - 18);

          ctx.restore();
        });

        // 4. Overhead Survival HUD (Timer & Status)
        ctx.save();
        ctx.fillStyle = "rgba(7, 12, 23, 0.9)";
        ctx.strokeStyle = "rgba(0, 242, 254, 0.35)";
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.roundRect(WIDTH / 2 - 140, 12, 280, 52, 10);
        ctx.fill();
        ctx.stroke();

        ctx.font = "bold 10px monospace";
        ctx.fillStyle = "#64748b";
        ctx.textAlign = "center";
        ctx.fillText("EVASION MATCH CLOCK", WIDTH / 2, 28);

        ctx.font = "bold 22px monospace";
        const urgent = current.timeRemainingSec <= 10;
        ctx.fillStyle = urgent ? "#ef4444" : "#00ff87";
        if (urgent) {
          ctx.shadowBlur = 10;
          ctx.shadowColor = "#ef4444";
        }
        ctx.fillText(`⏱ ${current.timeRemainingSec}s REMAINING`, WIDTH / 2, 50);
        ctx.restore();

        // 5. Match Over Banner
        if (current.matchOver) {
          ctx.save();
          ctx.fillStyle = "rgba(7, 12, 23, 0.9)";
          ctx.fillRect(0, 0, WIDTH, HEIGHT);

          ctx.fillStyle = "#00ff87";
          ctx.font = "bold 36px monospace";
          ctx.textAlign = "center";
          ctx.shadowBlur = 18;
          ctx.shadowColor = "#00ff87";
          ctx.fillText("TAG MATCH CONCLUDED!", WIDTH / 2, HEIGHT / 2 - 20);
          ctx.shadowBlur = 0;

          const winner = current.players.find((p) => p.playerId === current.winnerPlayerId);
          ctx.fillStyle = "#f8fafc";
          ctx.font = "bold 20px monospace";
          ctx.fillText(`CHAMPION: ${winner ? winner.displayName : "Survivor"} (Least Time as IT)!`, WIDTH / 2, HEIGHT / 2 + 25);
          ctx.restore();
        }
      }

      animId = requestAnimationFrame(render);
    };

    animId = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animId);
  }, [currentPlayerId]);

  return (
    <div style={{ position: "relative", width: "100%", maxWidth: WIDTH, margin: "0 auto" }}>
      <canvas
        ref={canvasRef}
        width={WIDTH}
        height={HEIGHT}
        style={{
          width: "100%",
          aspectRatio: `${WIDTH}/${HEIGHT}`,
          display: "block",
          borderRadius: 12,
          border: "1px solid rgba(0, 242, 254, 0.3)",
          boxShadow: "0 0 35px rgba(0, 242, 254, 0.15)",
        }}
      />
      <div style={controlsHintStyle}>
        <span><b>Movement:</b> WASD / Arrow Keys</span>
        <span><b>Goal:</b> Tag other players if IT; evade and survive if runner!</span>
      </div>
    </div>
  );
}

const controlsHintStyle: React.CSSProperties = {
  display: "flex",
  justifyContent: "center",
  gap: 24,
  marginTop: 10,
  color: "#94a3b8",
  fontSize: 12,
  fontFamily: "monospace",
};
