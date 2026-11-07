import { useEffect, useRef } from "react";
import type { InputRequest, RacersStateSnapshot, RacersVehicleSnapshot } from "../../types/multiplayer";

interface Props {
  state: RacersStateSnapshot | null;
  currentPlayerId: string;
  sendInput: (req: InputRequest) => void;
}

const WIDTH = 900;
const HEIGHT = 600;
const INPUT_SEND_HZ = 30;

interface ExhaustParticle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  color: string;
}

export function RacersCanvas({ state, currentPlayerId, sendInput }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef(state);
  const sendInputRef = useRef(sendInput);
  const keysRef = useRef<Set<string>>(new Set());
  const sequenceRef = useRef(0);
  const particlesRef = useRef<ExhaustParticle[]>([]);

  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  useEffect(() => {
    sendInputRef.current = sendInput;
  }, [sendInput]);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "KeyW", "KeyA", "KeyS", "KeyD", "Space"].includes(e.code)) {
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
      let moveY = 0;
      let moveX = 0;

      if (keys.has("KeyW") || keys.has("ArrowUp")) moveY -= 1;
      if (keys.has("KeyS") || keys.has("ArrowDown")) moveY += 1;
      if (keys.has("KeyA") || keys.has("ArrowLeft")) moveX -= 1;
      if (keys.has("KeyD") || keys.has("ArrowRight")) moveX += 1;

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

      // 1. Infield / Stadium Background
      ctx.fillStyle = "#070c17";
      ctx.fillRect(0, 0, WIDTH, HEIGHT);

      // Infield Tech Pattern
      ctx.save();
      ctx.strokeStyle = "rgba(0, 242, 254, 0.04)";
      ctx.lineWidth = 1;
      for (let x = 0; x < WIDTH; x += 30) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, HEIGHT);
        ctx.stroke();
      }
      for (let y = 0; y < HEIGHT; y += 30) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(WIDTH, y);
        ctx.stroke();
      }
      ctx.restore();

      // 2. Asphalt Track Body
      ctx.save();
      ctx.strokeStyle = "#131a2b";
      ctx.lineWidth = 144;
      ctx.beginPath();
      ctx.ellipse(WIDTH / 2, HEIGHT / 2, 330, 180, 0, 0, Math.PI * 2);
      ctx.stroke();

      // Racing Line Dash Pattern
      ctx.strokeStyle = "rgba(255, 255, 255, 0.12)";
      ctx.lineWidth = 2;
      ctx.setLineDash([20, 25]);
      ctx.beginPath();
      ctx.ellipse(WIDTH / 2, HEIGHT / 2, 330, 180, 0, 0, Math.PI * 2);
      ctx.stroke();
      ctx.setLineDash([]);

      // Rumble Strips / Red-White Curbs on Outer & Inner track borders
      ctx.strokeStyle = "#ef4444";
      ctx.lineWidth = 5;
      ctx.setLineDash([14, 14]);
      ctx.beginPath();
      ctx.ellipse(WIDTH / 2, HEIGHT / 2, 402, 252, 0, 0, Math.PI * 2);
      ctx.stroke();

      ctx.beginPath();
      ctx.ellipse(WIDTH / 2, HEIGHT / 2, 258, 108, 0, 0, Math.PI * 2);
      ctx.stroke();
      ctx.setLineDash([]);

      // Outer Neon Barrier Glow
      ctx.shadowBlur = 10;
      ctx.shadowColor = "#00f2fe";
      ctx.strokeStyle = "#00f2fe";
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      ctx.ellipse(WIDTH / 2, HEIGHT / 2, 404, 254, 0, 0, Math.PI * 2);
      ctx.stroke();

      // Inner Neon Barrier Glow
      ctx.strokeStyle = "#00ff87";
      ctx.shadowColor = "#00ff87";
      ctx.beginPath();
      ctx.ellipse(WIDTH / 2, HEIGHT / 2, 256, 106, 0, 0, Math.PI * 2);
      ctx.stroke();
      ctx.shadowBlur = 0;

      // Checkpoints & Waypoint Gates
      const cps = [
        { x: 450, y: 100, label: "START / FINISH" },
        { x: 780, y: 300, label: "SECTOR 1" },
        { x: 450, y: 500, label: "SECTOR 2" },
        { x: 120, y: 300, label: "SECTOR 3" },
      ];

      cps.forEach((cp, idx) => {
        ctx.strokeStyle = idx === 0 ? "#00ff87" : "rgba(0, 242, 254, 0.5)";
        ctx.lineWidth = 3;
        ctx.setLineDash([6, 6]);
        ctx.beginPath();
        ctx.arc(cp.x, cp.y, 45, 0, Math.PI * 2);
        ctx.stroke();
        ctx.setLineDash([]);

        ctx.fillStyle = idx === 0 ? "#00ff87" : "#00f2fe";
        ctx.font = "bold 10px monospace";
        ctx.textAlign = "center";
        ctx.fillText(cp.label, cp.x, cp.y - 50);
      });

      // Checkered Finish Line
      for (let y = 30; y < 170; y += 12) {
        ctx.fillStyle = (y / 12) % 2 === 0 ? "#ffffff" : "#0f172a";
        ctx.fillRect(447, y, 6, 6);
        ctx.fillStyle = (y / 12) % 2 === 0 ? "#0f172a" : "#ffffff";
        ctx.fillRect(453, y, 6, 6);
      }
      ctx.restore();

      // 3. Exhaust Smoke & Nitro Particles
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
        ctx.arc(pt.x, pt.y, 2.5 * alpha, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        if (pt.life >= pt.maxLife) {
          particles.splice(i, 1);
        }
      }

      // 4. Draw Vehicles
      if (current && current.vehicles) {
        current.vehicles.forEach((v: RacersVehicleSnapshot) => {
          const isMe = v.playerId === currentPlayerId;

          // Emit exhaust particles if moving
          if (v.speed > 20 && Math.random() < 0.6) {
            const backAngle = v.headingAngleRad + Math.PI;
            particlesRef.current.push({
              x: v.x + Math.cos(backAngle) * 18,
              y: v.y + Math.sin(backAngle) * 18,
              vx: Math.cos(backAngle + (Math.random() - 0.5) * 0.4) * (1 + Math.random() * 2),
              vy: Math.sin(backAngle + (Math.random() - 0.5) * 0.4) * (1 + Math.random() * 2),
              life: 1,
              maxLife: 15 + Math.random() * 10,
              color: isMe ? "#00f2fe" : "#f59e0b",
            });
          }

          ctx.save();
          ctx.translate(v.x, v.y);
          ctx.rotate(v.headingAngleRad);

          // Headlight Beam Projection onto Tarmac
          const beamGrad = ctx.createRadialGradient(24, 0, 5, 80, 0, 70);
          beamGrad.addColorStop(0, "rgba(254, 240, 138, 0.4)");
          beamGrad.addColorStop(0.6, "rgba(254, 240, 138, 0.12)");
          beamGrad.addColorStop(1, "rgba(254, 240, 138, 0)");
          ctx.fillStyle = beamGrad;
          ctx.beginPath();
          ctx.moveTo(18, -6);
          ctx.lineTo(85, -28);
          ctx.lineTo(85, 28);
          ctx.lineTo(18, 6);
          ctx.closePath();
          ctx.fill();

          // Vehicle Shadow
          ctx.fillStyle = "rgba(0,0,0,0.55)";
          ctx.beginPath();
          ctx.roundRect(-20, -12, 40, 24, 6);
          ctx.fill();

          // Performance Tires
          ctx.fillStyle = "#0f172a";
          ctx.strokeStyle = "#475569";
          ctx.lineWidth = 1;
          const wheels = [
            [-16, -14], [8, -14],
            [-16, 10], [8, 10]
          ];
          for (const [wx, wy] of wheels) {
            ctx.fillRect(wx, wy, 8, 4);
            ctx.strokeRect(wx, wy, 8, 4);
          }

          // Aerodynamic Cyber-Chassis Body
          const bodyGrad = ctx.createLinearGradient(-18, 0, 18, 0);
          if (isMe) {
            bodyGrad.addColorStop(0, "#0284c7");
            bodyGrad.addColorStop(0.5, "#00f2fe");
            bodyGrad.addColorStop(1, "#38bdf8");
          } else {
            bodyGrad.addColorStop(0, "#be185d");
            bodyGrad.addColorStop(0.5, "#ec4899");
            bodyGrad.addColorStop(1, "#f472b6");
          }
          ctx.fillStyle = bodyGrad;
          ctx.beginPath();
          ctx.roundRect(-18, -10, 36, 20, 6);
          ctx.fill();

          ctx.strokeStyle = isMe ? "#e0f2fe" : "#fce7f3";
          ctx.lineWidth = 1.5;
          ctx.stroke();

          // Aerodynamic Rear Wing / Spoiler
          ctx.fillStyle = isMe ? "#0369a1" : "#9d174d";
          ctx.fillRect(-20, -11, 4, 22);

          // Tinted Cockpit Canopy
          const canopyGrad = ctx.createLinearGradient(-4, -6, 8, 6);
          canopyGrad.addColorStop(0, "#0f172a");
          canopyGrad.addColorStop(1, "#1e293b");
          ctx.fillStyle = canopyGrad;
          ctx.beginPath();
          ctx.roundRect(-5, -6, 14, 12, 3);
          ctx.fill();
          ctx.strokeStyle = isMe ? "#00f2fe" : "#ec4899";
          ctx.lineWidth = 1;
          ctx.stroke();

          // Twin Headlight LEDs
          ctx.fillStyle = "#fef08a";
          ctx.shadowBlur = 6;
          ctx.shadowColor = "#fef08a";
          ctx.fillRect(16, -7, 3, 3);
          ctx.fillRect(16, 4, 3, 3);
          ctx.shadowBlur = 0;

          ctx.restore();

          // Overhead Gamer Tag & Place
          ctx.fillStyle = isMe ? "#00f2fe" : "#ffffff";
          ctx.font = "bold 11px monospace";
          ctx.textAlign = "center";
          const tag = `${v.displayName} (Lap ${v.lap}/3${v.place > 0 ? ` · ${v.place}${ordinal(v.place)}` : ""})`;
          ctx.fillText(tag, v.x, v.y - 24);
        });

        // 5. High-Tech Arc Speedometer & Race Position HUD
        const myVehicle = current.vehicles.find((v) => v.playerId === currentPlayerId);
        if (myVehicle) {
          ctx.save();
          // HUD Card
          ctx.fillStyle = "rgba(7, 12, 23, 0.88)";
          ctx.strokeStyle = "rgba(0, 242, 254, 0.35)";
          ctx.lineWidth = 1.5;
          ctx.beginPath();
          ctx.roundRect(25, 25, 230, 85, 12);
          ctx.fill();
          ctx.stroke();

          // Labels
          ctx.fillStyle = "#64748b";
          ctx.font = "10px monospace";
          ctx.textAlign = "left";
          ctx.fillText("LAP", 42, 46);
          ctx.fillText("SPEED (KPH)", 105, 46);
          ctx.fillText("RANK", 192, 46);

          // Lap Value
          ctx.fillStyle = "#00f2fe";
          ctx.font = "bold 20px monospace";
          ctx.fillText(`${myVehicle.lap}/3`, 42, 74);

          // Speed Value
          const spd = Math.round(myVehicle.speed);
          ctx.fillStyle = spd > 100 ? "#00ff87" : "#f8fafc";
          ctx.fillText(`${spd}`, 105, 74);

          // Place Badge
          ctx.fillStyle = myVehicle.place === 1 ? "#00ff87" : myVehicle.place === 2 ? "#38bdf8" : "#f59e0b";
          ctx.fillText(myVehicle.place > 0 ? `${myVehicle.place}${ordinal(myVehicle.place)}` : "—", 192, 74);

          // Mini Speed Progress Arc Bar
          ctx.fillStyle = "#1e293b";
          ctx.fillRect(42, 88, 195, 6);
          const spdPct = Math.min(1, spd / 200);
          const spdGrad = ctx.createLinearGradient(42, 0, 237, 0);
          spdGrad.addColorStop(0, "#00f2fe");
          spdGrad.addColorStop(1, "#00ff87");
          ctx.fillStyle = spdGrad;
          ctx.fillRect(42, 88, 195 * spdPct, 6);

          ctx.restore();
        }

        // 6. Match Winner Banner
        if (current.matchOver && current.winnerPlayerId) {
          const winner = current.vehicles.find((v) => v.playerId === current.winnerPlayerId);
          ctx.save();
          ctx.fillStyle = "rgba(7, 12, 23, 0.88)";
          ctx.fillRect(0, 0, WIDTH, HEIGHT);

          ctx.fillStyle = "#00ff87";
          ctx.shadowBlur = 18;
          ctx.shadowColor = "#00ff87";
          ctx.font = "bold 36px monospace";
          ctx.textAlign = "center";
          ctx.fillText("🏆 RACE FINISHED! 🏆", WIDTH / 2, HEIGHT / 2 - 25);
          ctx.shadowBlur = 0;

          ctx.fillStyle = "#f8fafc";
          ctx.font = "bold 20px monospace";
          ctx.fillText(`WINNER: ${winner ? winner.displayName : "CHAMPION"}!`, WIDTH / 2, HEIGHT / 2 + 20);
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
        <span><b>Drive:</b> W / ↑</span>
        <span><b>Brake:</b> S / ↓</span>
        <span><b>Steer:</b> A / D or ← / →</span>
      </div>
    </div>
  );
}

function ordinal(n: number): string {
  if (n === 1) return "st";
  if (n === 2) return "nd";
  if (n === 3) return "rd";
  return "th";
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
