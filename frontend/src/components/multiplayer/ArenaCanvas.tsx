import { useEffect, useRef } from "react";
import {
  ARENA_HEIGHT,
  ARENA_WIDTH,
  PLAYER_RADIUS,
  PROJECTILE_RADIUS,
  type ArenaStateSnapshot,
  type InputRequest,
} from "../../types/multiplayer";

interface Props {
  state: ArenaStateSnapshot | null;
  currentPlayerId: string; // String(userId)
  sendInput: (req: InputRequest) => void;
}

const INPUT_SEND_HZ = 30;
const MAX_EXTRAPOLATION_FACTOR = 2;

interface SnapshotEntry {
  snapshot: ArenaStateSnapshot;
  receivedAt: number;
}

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  color: string;
  size: number;
}

function interpolate(history: SnapshotEntry[], now: number): ArenaStateSnapshot | null {
  if (history.length === 0) return null;
  if (history.length === 1) return history[0].snapshot;

  const [older, newer] = history;
  const gap = newer.receivedAt - older.receivedAt;
  const t = gap > 0 ? Math.min(MAX_EXTRAPOLATION_FACTOR, (now - older.receivedAt) / gap) : 1;

  const players = newer.snapshot.players.map((n) => {
    const o = older.snapshot.players.find((p) => p.playerId === n.playerId);
    if (!o) return n;
    return {
      ...n,
      x: o.x + (n.x - o.x) * t,
      y: o.y + (n.y - o.y) * t,
      aimAngleRad: o.aimAngleRad + (n.aimAngleRad - o.aimAngleRad) * t,
    };
  });

  const projectiles = newer.snapshot.projectiles.map((n) => {
    const o = older.snapshot.projectiles.find((p) => p.id === n.id);
    if (!o) return n;
    return { ...n, x: o.x + (n.x - o.x) * t, y: o.y + (n.y - o.y) * t };
  });

  return { ...newer.snapshot, players, projectiles };
}

export function ArenaCanvas({ state, currentPlayerId, sendInput }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef(state);
  const historyRef = useRef<SnapshotEntry[]>([]);
  const sendInputRef = useRef(sendInput);
  const keysRef = useRef<Set<string>>(new Set());
  const mouseRef = useRef({ x: 0, y: 0, down: false });
  const sequenceRef = useRef(0);
  const particlesRef = useRef<Particle[]>([]);
  const prevProjectilesCountRef = useRef(0);

  useEffect(() => {
    stateRef.current = state;
    if (state) {
      historyRef.current = [...historyRef.current, { snapshot: state, receivedAt: performance.now() }].slice(-2);

      // Check for bullet impacts or firing to spawn muzzle sparks
      if (state.projectiles.length < prevProjectilesCountRef.current) {
        for (let i = 0; i < 8; i++) {
          const angle = Math.random() * Math.PI * 2;
          const speed = 1.5 + Math.random() * 3.5;
          particlesRef.current.push({
            x: Math.random() * ARENA_WIDTH,
            y: Math.random() * ARENA_HEIGHT,
            vx: Math.cos(angle) * speed,
            vy: Math.sin(angle) * speed,
            life: 1,
            maxLife: 20 + Math.random() * 15,
            color: Math.random() > 0.5 ? "#00f2fe" : "#facc15",
            size: 2 + Math.random() * 3,
          });
        }
      }
      prevProjectilesCountRef.current = state.projectiles.length;
    }
  }, [state]);

  useEffect(() => {
    sendInputRef.current = sendInput;
  }, [sendInput]);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => keysRef.current.add(e.key.toLowerCase());
    const onKeyUp = (e: KeyboardEvent) => keysRef.current.delete(e.key.toLowerCase());
    const onMouseMove = (e: MouseEvent) => {
      const rect = canvasRef.current?.getBoundingClientRect();
      if (!rect) return;
      const scaleX = ARENA_WIDTH / rect.width;
      const scaleY = ARENA_HEIGHT / rect.height;
      mouseRef.current.x = (e.clientX - rect.left) * scaleX;
      mouseRef.current.y = (e.clientY - rect.top) * scaleY;
    };
    const onMouseDown = () => (mouseRef.current.down = true);
    const onMouseUp = () => (mouseRef.current.down = false);

    window.addEventListener("keydown", onKeyDown);
    window.addEventListener("keyup", onKeyUp);
    const canvas = canvasRef.current;
    canvas?.addEventListener("mousemove", onMouseMove);
    canvas?.addEventListener("mousedown", onMouseDown);
    window.addEventListener("mouseup", onMouseUp);

    return () => {
      window.removeEventListener("keydown", onKeyDown);
      window.removeEventListener("keyup", onKeyUp);
      canvas?.removeEventListener("mousemove", onMouseMove);
      canvas?.removeEventListener("mousedown", onMouseDown);
      window.removeEventListener("mouseup", onMouseUp);
    };
  }, []);

  // Fixed-rate input send loop
  useEffect(() => {
    const intervalMs = 1000 / INPUT_SEND_HZ;
    const id = setInterval(() => {
      const keys = keysRef.current;
      let moveX = 0;
      let moveY = 0;
      if (keys.has("w") || keys.has("arrowup")) moveY -= 1;
      if (keys.has("s") || keys.has("arrowdown")) moveY += 1;
      if (keys.has("a") || keys.has("arrowleft")) moveX -= 1;
      if (keys.has("d") || keys.has("arrowright")) moveX += 1;

      const mag = Math.hypot(moveX, moveY);
      if (mag > 0) {
        moveX /= mag;
        moveY /= mag;
      }

      const me = stateRef.current?.players.find((p) => p.playerId === currentPlayerId);
      const aimAngleRad = me ? Math.atan2(mouseRef.current.y - me.y, mouseRef.current.x - me.x) : 0;

      sendInputRef.current({
        sequence: sequenceRef.current++,
        clientTimestamp: Date.now(),
        moveX,
        moveY,
        aimAngleRad,
        firing: mouseRef.current.down,
        actionPressed: false,
      });
    }, intervalMs);
    return () => clearInterval(id);
  }, [currentPlayerId]);

  // Render loop
  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext("2d");
    if (!canvas || !ctx) return;
    let raf: number;

    const draw = () => {
      // 1. Deep Space Tech Floor Background
      ctx.fillStyle = "#070a13";
      ctx.fillRect(0, 0, ARENA_WIDTH, ARENA_HEIGHT);

      // Sci-fi grid
      ctx.save();
      ctx.strokeStyle = "rgba(0, 242, 254, 0.05)";
      ctx.lineWidth = 1;
      const gridSize = 50;
      for (let x = 0; x < ARENA_WIDTH; x += gridSize) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, ARENA_HEIGHT);
        ctx.stroke();
      }
      for (let y = 0; y < ARENA_HEIGHT; y += gridSize) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(ARENA_WIDTH, y);
        ctx.stroke();
      }

      // Tactical Crosshairs in corners and center
      ctx.strokeStyle = "rgba(0, 242, 254, 0.15)";
      ctx.lineWidth = 1.5;
      const cx = ARENA_WIDTH / 2;
      const cy = ARENA_HEIGHT / 2;

      // Center ring
      ctx.beginPath();
      ctx.arc(cx, cy, 120, 0, Math.PI * 2);
      ctx.stroke();

      ctx.beginPath();
      ctx.arc(cx, cy, 40, 0, Math.PI * 2);
      ctx.stroke();

      // Glowing Perimeter Border
      ctx.strokeStyle = "#00f2fe";
      ctx.lineWidth = 3;
      ctx.shadowBlur = 12;
      ctx.shadowColor = "#00f2fe";
      ctx.strokeRect(20, 20, ARENA_WIDTH - 40, ARENA_HEIGHT - 40);

      // Hazard chevrons on corners
      ctx.shadowBlur = 0;
      ctx.fillStyle = "rgba(245, 158, 11, 0.35)";
      ctx.fillRect(20, 20, 30, 8);
      ctx.fillRect(20, 20, 8, 30);
      ctx.fillRect(ARENA_WIDTH - 50, 20, 30, 8);
      ctx.fillRect(ARENA_WIDTH - 28, 20, 8, 30);
      ctx.fillRect(20, ARENA_HEIGHT - 28, 30, 8);
      ctx.fillRect(20, ARENA_HEIGHT - 50, 8, 30);
      ctx.fillRect(ARENA_WIDTH - 50, ARENA_HEIGHT - 28, 30, 8);
      ctx.fillRect(ARENA_WIDTH - 28, ARENA_HEIGHT - 50, 8, 30);
      ctx.restore();

      const snapshot = interpolate(historyRef.current, performance.now());

      // 2. Render and Update Particles
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
        ctx.arc(pt.x, pt.y, pt.size * alpha, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        if (pt.life >= pt.maxLife) {
          particles.splice(i, 1);
        }
      }

      if (snapshot) {
        // 3. Render Projectiles with Plasma Trails
        for (const proj of snapshot.projectiles) {
          ctx.save();
          ctx.shadowBlur = 14;
          ctx.shadowColor = "#facc15";

          const grad = ctx.createRadialGradient(proj.x, proj.y, 1, proj.x, proj.y, PROJECTILE_RADIUS * 2.5);
          grad.addColorStop(0, "#ffffff");
          grad.addColorStop(0.4, "#facc15");
          grad.addColorStop(1, "rgba(245, 158, 11, 0)");
          ctx.fillStyle = grad;
          ctx.beginPath();
          ctx.arc(proj.x, proj.y, PROJECTILE_RADIUS * 2.5, 0, Math.PI * 2);
          ctx.fill();

          ctx.fillStyle = "#ffffff";
          ctx.beginPath();
          ctx.arc(proj.x, proj.y, PROJECTILE_RADIUS, 0, Math.PI * 2);
          ctx.fill();
          ctx.restore();
        }

        // 4. Render Players (Operatives / Mechs)
        for (const p of snapshot.players) {
          const isSelf = p.playerId === currentPlayerId;
          ctx.save();
          ctx.globalAlpha = p.alive ? 1 : 0.25;

          // Ground shadow
          ctx.fillStyle = "rgba(0, 0, 0, 0.6)";
          ctx.beginPath();
          ctx.ellipse(p.x, p.y + 6, PLAYER_RADIUS * 1.3, PLAYER_RADIUS * 0.8, 0, 0, Math.PI * 2);
          ctx.fill();

          // Outer radar pulse for local player
          if (isSelf && p.alive) {
            ctx.strokeStyle = "rgba(0, 242, 254, 0.4)";
            ctx.lineWidth = 1.5;
            ctx.setLineDash([4, 4]);
            ctx.beginPath();
            ctx.arc(p.x, p.y, PLAYER_RADIUS + 10, 0, Math.PI * 2);
            ctx.stroke();
            ctx.setLineDash([]);
          }

          // Mech Direction Rotation
          ctx.save();
          ctx.translate(p.x, p.y);
          ctx.rotate(p.aimAngleRad);

          // Dual Weapon Cannons
          ctx.fillStyle = "#334155";
          ctx.fillRect(8, -8, 22, 4);
          ctx.fillRect(8, 4, 22, 4);

          // Cannon Barrels Muzzle Tip Glow
          ctx.fillStyle = isSelf ? "#00f2fe" : "#ef4444";
          ctx.fillRect(28, -8, 4, 4);
          ctx.fillRect(28, 4, 4, 4);

          // Armor Shoulder Pods
          ctx.fillStyle = isSelf ? "#1e40af" : "#991b1b";
          ctx.beginPath();
          ctx.arc(-4, -14, 7, 0, Math.PI * 2);
          ctx.arc(-4, 14, 7, 0, Math.PI * 2);
          ctx.fill();

          // Main Chassis Body
          const bodyGrad = ctx.createRadialGradient(0, 0, 3, 0, 0, PLAYER_RADIUS);
          if (isSelf) {
            bodyGrad.addColorStop(0, "#38bdf8");
            bodyGrad.addColorStop(0.7, "#0284c7");
            bodyGrad.addColorStop(1, "#0c4a6e");
          } else {
            bodyGrad.addColorStop(0, "#f87171");
            bodyGrad.addColorStop(0.7, "#dc2626");
            bodyGrad.addColorStop(1, "#7f1d1d");
          }
          ctx.fillStyle = bodyGrad;
          ctx.beginPath();
          ctx.arc(0, 0, PLAYER_RADIUS, 0, Math.PI * 2);
          ctx.fill();

          ctx.strokeStyle = isSelf ? "#00f2fe" : "#fca5a5";
          ctx.lineWidth = 2;
          ctx.stroke();

          // Center Arc Reactor Core
          ctx.shadowBlur = 8;
          ctx.shadowColor = isSelf ? "#00f2fe" : "#ef4444";
          ctx.fillStyle = "#ffffff";
          ctx.beginPath();
          ctx.arc(2, 0, 5, 0, Math.PI * 2);
          ctx.fill();
          ctx.restore(); // Exit rotation

          // Overhead HUD Tag & Health
          ctx.shadowBlur = 0;
          ctx.font = "bold 12px monospace";
          ctx.textAlign = "center";
          ctx.fillStyle = isSelf ? "#00f2fe" : "#f8fafc";
          ctx.fillText(`${p.displayName} [${p.score}]`, p.x, p.y - PLAYER_RADIUS - 18);

          // Modern Segmented Health Bar
          const barWidth = 44;
          const barHeight = 6;
          const barX = p.x - barWidth / 2;
          const barY = p.y - PLAYER_RADIUS - 12;

          ctx.fillStyle = "rgba(15, 23, 42, 0.85)";
          ctx.fillRect(barX - 1, barY - 1, barWidth + 2, barHeight + 2);
          ctx.strokeStyle = "#334155";
          ctx.lineWidth = 1;
          ctx.strokeRect(barX - 1, barY - 1, barWidth + 2, barHeight + 2);

          const hpPct = Math.max(0, Math.min(1, p.health / 100));
          const hpGrad = ctx.createLinearGradient(barX, 0, barX + barWidth, 0);
          if (hpPct > 0.5) {
            hpGrad.addColorStop(0, "#00ff87");
            hpGrad.addColorStop(1, "#00f2fe");
          } else if (hpPct > 0.2) {
            hpGrad.addColorStop(0, "#f59e0b");
            hpGrad.addColorStop(1, "#fbbf24");
          } else {
            hpGrad.addColorStop(0, "#dc2626");
            hpGrad.addColorStop(1, "#ef4444");
          }
          ctx.fillStyle = hpGrad;
          ctx.fillRect(barX, barY, barWidth * hpPct, barHeight);

          ctx.restore();
        }

        // 5. Tactical Corner HUD & Minimap
        const me = snapshot.players.find((p) => p.playerId === currentPlayerId);
        if (me) {
          ctx.save();
          ctx.fillStyle = "rgba(8, 13, 26, 0.85)";
          ctx.strokeStyle = "rgba(0, 242, 254, 0.35)";
          ctx.lineWidth = 1.5;
          ctx.beginPath();
          ctx.roundRect(35, ARENA_HEIGHT - 85, 220, 55, 8);
          ctx.fill();
          ctx.stroke();

          ctx.fillStyle = "#94a3b8";
          ctx.font = "10px monospace";
          ctx.textAlign = "left";
          ctx.fillText("TACTICAL MECH SHIELD INTEGRITY", 48, ARENA_HEIGHT - 65);

          ctx.fillStyle = "#1e293b";
          ctx.fillRect(48, ARENA_HEIGHT - 58, 194, 14);
          const fullHp = Math.max(0, me.health / 100);
          const bigHpGrad = ctx.createLinearGradient(48, 0, 242, 0);
          bigHpGrad.addColorStop(0, fullHp > 0.3 ? "#00ff87" : "#ef4444");
          bigHpGrad.addColorStop(1, fullHp > 0.3 ? "#00f2fe" : "#f59e0b");
          ctx.fillStyle = bigHpGrad;
          ctx.fillRect(48, ARENA_HEIGHT - 58, 194 * fullHp, 14);

          ctx.fillStyle = "#f8fafc";
          ctx.font = "bold 11px monospace";
          ctx.textAlign = "right";
          ctx.fillText(`${me.health} HP`, 236, ARENA_HEIGHT - 47);
          ctx.restore();

          // Top-Right Minimap
          const mapSize = 120;
          const mapX = ARENA_WIDTH - mapSize - 35;
          const mapY = 35;

          ctx.save();
          ctx.fillStyle = "rgba(8, 13, 26, 0.85)";
          ctx.strokeStyle = "rgba(0, 242, 254, 0.4)";
          ctx.lineWidth = 1.5;
          ctx.strokeRect(mapX, mapY, mapSize, (mapSize * ARENA_HEIGHT) / ARENA_WIDTH);
          ctx.fillRect(mapX, mapY, mapSize, (mapSize * ARENA_HEIGHT) / ARENA_WIDTH);

          const scanY = mapY + ((Date.now() / 15) % ((mapSize * ARENA_HEIGHT) / ARENA_WIDTH));
          ctx.strokeStyle = "rgba(0, 255, 135, 0.25)";
          ctx.beginPath();
          ctx.moveTo(mapX, scanY);
          ctx.lineTo(mapX + mapSize, scanY);
          ctx.stroke();

          const scaleW = mapSize / ARENA_WIDTH;
          const scaleH = ((mapSize * ARENA_HEIGHT) / ARENA_WIDTH) / ARENA_HEIGHT;

          for (const pl of snapshot.players) {
            if (!pl.alive) continue;
            ctx.fillStyle = pl.playerId === currentPlayerId ? "#00ff87" : "#ef4444";
            ctx.beginPath();
            ctx.arc(mapX + pl.x * scaleW, mapY + pl.y * scaleH, 3, 0, Math.PI * 2);
            ctx.fill();
          }
          ctx.restore();
        }
      }

      // 6. Tactical Crosshair at Mouse
      ctx.save();
      const mx = mouseRef.current.x;
      const my = mouseRef.current.y;
      ctx.strokeStyle = "#00f2fe";
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(mx, my, 8, 0, Math.PI * 2);
      ctx.stroke();

      ctx.beginPath();
      ctx.moveTo(mx - 14, my);
      ctx.lineTo(mx - 4, my);
      ctx.moveTo(mx + 4, my);
      ctx.lineTo(mx + 14, my);
      ctx.moveTo(mx, my - 14);
      ctx.lineTo(mx, my - 4);
      ctx.moveTo(mx, my + 4);
      ctx.lineTo(mx, my + 14);
      ctx.stroke();
      ctx.restore();

      raf = requestAnimationFrame(draw);
    };

    raf = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(raf);
  }, [currentPlayerId]);

  return (
    <div style={{ position: "relative", width: "100%", maxWidth: 960, margin: "0 auto" }}>
      <canvas
        ref={canvasRef}
        width={ARENA_WIDTH}
        height={ARENA_HEIGHT}
        style={{
          width: "100%",
          aspectRatio: `${ARENA_WIDTH}/${ARENA_HEIGHT}`,
          display: "block",
          borderRadius: 12,
          boxShadow: "0 0 35px rgba(0, 242, 254, 0.15)",
          border: "1px solid rgba(0, 242, 254, 0.3)",
          cursor: "none",
        }}
      />
    </div>
  );
}
