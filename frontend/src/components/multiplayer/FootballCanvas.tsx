import { useEffect, useRef } from "react";
import type { FootballPlayerSnapshot, FootballStateSnapshot, InputRequest } from "../../types/multiplayer";

interface Props {
  state: FootballStateSnapshot | null;
  currentPlayerId: string;
  sendInput: (req: InputRequest) => void;
}

const WIDTH = 900;
const HEIGHT = 550;
const INPUT_SEND_HZ = 30;

export function FootballCanvas({ state, currentPlayerId, sendInput }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef(state);
  const sendInputRef = useRef(sendInput);
  const keysRef = useRef<Set<string>>(new Set());
  const mouseRef = useRef({ x: WIDTH / 2, y: HEIGHT / 2, kick: false });
  const sequenceRef = useRef(0);

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

    const onMouseMove = (e: MouseEvent) => {
      const canvas = canvasRef.current;
      if (!canvas) return;
      const rect = canvas.getBoundingClientRect();
      mouseRef.current.x = (e.clientX - rect.left) * (WIDTH / rect.width);
      mouseRef.current.y = (e.clientY - rect.top) * (HEIGHT / rect.height);
    };

    const onMouseDown = (e: MouseEvent) => {
      if (e.button === 0) mouseRef.current.kick = true;
    };

    const onMouseUp = (e: MouseEvent) => {
      if (e.button === 0) mouseRef.current.kick = false;
    };

    window.addEventListener("keydown", onKeyDown);
    window.addEventListener("keyup", onKeyUp);
    window.addEventListener("mousemove", onMouseMove);
    window.addEventListener("mousedown", onMouseDown);
    window.addEventListener("mouseup", onMouseUp);

    return () => {
      window.removeEventListener("keydown", onKeyDown);
      window.removeEventListener("keyup", onKeyUp);
      window.removeEventListener("mousemove", onMouseMove);
      window.removeEventListener("mousedown", onMouseDown);
      window.removeEventListener("mouseup", onMouseUp);
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

      let aimAngle = 0;
      const current = stateRef.current;
      if (current && current.players) {
        const me = current.players.find((p) => p.playerId === currentPlayerId);
        if (me) {
          aimAngle = Math.atan2(mouseRef.current.y - me.y, mouseRef.current.x - me.x);
        }
      }

      const firing = mouseRef.current.kick || keys.has("Space");

      sendInputRef.current({
        sequence: ++sequenceRef.current,
        clientTimestamp: Date.now(),
        moveX,
        moveY,
        aimAngleRad: aimAngle,
        firing,
        actionPressed: firing,
      });
    }, 1000 / INPUT_SEND_HZ);

    return () => clearInterval(interval);
  }, [currentPlayerId]);

  // Animation render loop
  useEffect(() => {
    let animId: number;

    const render = () => {
      const canvas = canvasRef.current;
      if (!canvas) return;
      const ctx = canvas.getContext("2d");
      if (!ctx) return;

      const current = stateRef.current;

      // 1. Stadium Surround & Pitch Grass
      ctx.fillStyle = "#0c150c";
      ctx.fillRect(0, 0, WIDTH, HEIGHT);

      // Pitch Grass Lawn Stripes (Alternating Dark & Light Emerald Green)
      const fieldX = 60;
      const fieldY = 40;
      const fieldW = 780;
      const fieldH = HEIGHT - 80;
      const stripeW = fieldW / 12;

      for (let i = 0; i < 12; i++) {
        ctx.fillStyle = i % 2 === 0 ? "#14361b" : "#1a4223";
        ctx.fillRect(fieldX + i * stripeW, fieldY, stripeW, fieldH);
      }

      // Pitch Boundary Markings
      ctx.save();
      ctx.strokeStyle = "rgba(255, 255, 255, 0.75)";
      ctx.lineWidth = 2.5;

      // Outer boundary line
      ctx.strokeRect(fieldX, fieldY, fieldW, fieldH);

      // Halfway line & Center Circle
      ctx.beginPath();
      ctx.moveTo(WIDTH / 2, fieldY);
      ctx.lineTo(WIDTH / 2, fieldY + fieldH);
      ctx.stroke();

      ctx.beginPath();
      ctx.arc(WIDTH / 2, HEIGHT / 2, 70, 0, Math.PI * 2);
      ctx.stroke();

      // Center Spot
      ctx.fillStyle = "#ffffff";
      ctx.beginPath();
      ctx.arc(WIDTH / 2, HEIGHT / 2, 4, 0, Math.PI * 2);
      ctx.fill();

      // Penalty Box - Left (Red Side)
      ctx.strokeRect(fieldX, HEIGHT / 2 - 85, 90, 170);
      ctx.strokeRect(fieldX, HEIGHT / 2 - 45, 40, 90);

      // Goal Nets Left
      ctx.fillStyle = "rgba(239, 68, 68, 0.12)";
      ctx.fillRect(12, HEIGHT / 2 - 60, 48, 120);
      ctx.strokeStyle = "rgba(239, 68, 68, 0.6)";
      ctx.strokeRect(12, HEIGHT / 2 - 60, 48, 120);

      // Penalty Box - Right (Blue Side)
      ctx.strokeStyle = "rgba(255, 255, 255, 0.75)";
      ctx.strokeRect(fieldX + fieldW - 90, HEIGHT / 2 - 85, 90, 170);
      ctx.strokeRect(fieldX + fieldW - 40, HEIGHT / 2 - 45, 40, 90);

      // Goal Nets Right
      ctx.fillStyle = "rgba(56, 189, 248, 0.12)";
      ctx.fillRect(fieldX + fieldW, HEIGHT / 2 - 60, 48, 120);
      ctx.strokeStyle = "rgba(56, 189, 248, 0.6)";
      ctx.strokeRect(fieldX + fieldW, HEIGHT / 2 - 60, 48, 120);
      ctx.restore();

      if (current) {
        // 2. Soccer Ball with 3D Shader & Shadow
        if (current.ball) {
          ctx.save();
          const bx = current.ball.x;
          const by = current.ball.y;

          // Realistic Ground Shadow
          ctx.fillStyle = "rgba(0, 0, 0, 0.45)";
          ctx.beginPath();
          ctx.ellipse(bx + 2, by + 5, 11, 6, 0, 0, Math.PI * 2);
          ctx.fill();

          // Ball Body Gradient
          const ballGrad = ctx.createRadialGradient(bx - 3, by - 3, 2, bx, by, 10);
          ballGrad.addColorStop(0, "#ffffff");
          ballGrad.addColorStop(0.7, "#e2e8f0");
          ballGrad.addColorStop(1, "#94a3b8");
          ctx.fillStyle = ballGrad;
          ctx.beginPath();
          ctx.arc(bx, by, 10, 0, Math.PI * 2);
          ctx.fill();

          ctx.strokeStyle = "#0f172a";
          ctx.lineWidth = 1.2;
          ctx.stroke();

          // Hexagon / Pentagon Patterns
          ctx.fillStyle = "#1e293b";
          ctx.beginPath();
          ctx.arc(bx, by, 4, 0, Math.PI * 2);
          ctx.fill();

          // Ball Speed Trail if moving fast
          const ballSpeed = Math.hypot(current.ball.vx || 0, current.ball.vy || 0);
          if (ballSpeed > 50) {
            ctx.strokeStyle = "rgba(255, 255, 255, 0.4)";
            ctx.lineWidth = 4;
            ctx.beginPath();
            ctx.moveTo(bx, by);
            ctx.lineTo(bx - (current.ball.vx || 0) * 0.15, by - (current.ball.vy || 0) * 0.15);
            ctx.stroke();
          }
          ctx.restore();
        }

        // 3. Render Players with Team Kits
        current.players.forEach((p: FootballPlayerSnapshot) => {
          const isMe = p.playerId === currentPlayerId;
          const isRed = p.team === "RED";

          ctx.save();

          // Player Aim Guide for current user
          if (isMe) {
            ctx.strokeStyle = "#facc15";
            ctx.lineWidth = 2;
            ctx.setLineDash([4, 4]);
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(p.x + 40 * Math.cos(p.aimAngleRad), p.y + 40 * Math.sin(p.aimAngleRad));
            ctx.stroke();
            ctx.setLineDash([]);
          }

          // Active User Target Reticle
          if (isMe) {
            ctx.strokeStyle = "#00f2fe";
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.arc(p.x, p.y, 25, 0, Math.PI * 2);
            ctx.stroke();
          }

          // Ball Possession Aura Ring
          if (p.hasBall) {
            ctx.strokeStyle = "#00ff87";
            ctx.shadowBlur = 10;
            ctx.shadowColor = "#00ff87";
            ctx.lineWidth = 3;
            ctx.beginPath();
            ctx.arc(p.x, p.y, 23, 0, Math.PI * 2);
            ctx.stroke();
            ctx.shadowBlur = 0;
          }

          // Player Shadow
          ctx.fillStyle = "rgba(0, 0, 0, 0.45)";
          ctx.beginPath();
          ctx.ellipse(p.x, p.y + 4, 18, 9, 0, 0, Math.PI * 2);
          ctx.fill();

          // Player Jersey Kit with Dual Shading
          const kitGrad = ctx.createRadialGradient(p.x - 3, p.y - 3, 3, p.x, p.y, 18);
          if (isRed) {
            kitGrad.addColorStop(0, "#f87171");
            kitGrad.addColorStop(0.7, "#dc2626");
            kitGrad.addColorStop(1, "#991b1b");
          } else {
            kitGrad.addColorStop(0, "#38bdf8");
            kitGrad.addColorStop(0.7, "#0284c7");
            kitGrad.addColorStop(1, "#0369a1");
          }
          ctx.fillStyle = kitGrad;
          ctx.beginPath();
          ctx.arc(p.x, p.y, 18, 0, Math.PI * 2);
          ctx.fill();

          ctx.strokeStyle = "#ffffff";
          ctx.lineWidth = 2;
          ctx.stroke();

          // Team Badge / Number
          ctx.fillStyle = "#ffffff";
          ctx.font = "bold 12px monospace";
          ctx.textAlign = "center";
          ctx.textBaseline = "middle";
          ctx.fillText(isRed ? "R" : "B", p.x, p.y);

          // Overhead Name Tag
          ctx.font = "bold 11px monospace";
          ctx.fillStyle = isMe ? "#facc15" : "#f8fafc";
          ctx.fillText(p.displayName, p.x, p.y - 24);

          ctx.restore();
        });

        // 4. Overhead Stadium Scoreboard
        ctx.save();
        ctx.fillStyle = "rgba(7, 12, 23, 0.9)";
        ctx.strokeStyle = "rgba(0, 242, 254, 0.35)";
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.roundRect(WIDTH / 2 - 170, 10, 340, 56, 12);
        ctx.fill();
        ctx.stroke();

        // Team Labels
        ctx.font = "bold 13px monospace";
        ctx.textAlign = "center";
        ctx.fillStyle = "#ef4444";
        ctx.fillText("RED SQUAD", WIDTH / 2 - 110, 32);

        ctx.fillStyle = "#38bdf8";
        ctx.fillText("BLUE SQUAD", WIDTH / 2 + 110, 32);

        // Live Match Score
        ctx.font = "bold 24px monospace";
        ctx.fillStyle = "#ffffff";
        ctx.fillText(`${current.redScore}  -  ${current.blueScore}`, WIDTH / 2, 38);

        // Game Clock
        ctx.font = "11px monospace";
        ctx.fillStyle = "#00ff87";
        const mins = Math.floor(current.timeRemainingSec / 60);
        const secs = current.timeRemainingSec % 60;
        ctx.fillText(`⏱ ${mins}:${secs < 10 ? "0" : ""}${secs}`, WIDTH / 2, 55);
        ctx.restore();

        // 5. Goal Event Splash
        if (current.lastEvent && current.lastEvent.startsWith("GOAL")) {
          ctx.save();
          ctx.fillStyle = "rgba(7, 12, 23, 0.75)";
          ctx.fillRect(0, HEIGHT / 2 - 45, WIDTH, 90);

          const isRedGoal = current.lastEvent.includes("RED");
          ctx.fillStyle = isRedGoal ? "#ef4444" : "#38bdf8";
          ctx.shadowBlur = 20;
          ctx.shadowColor = isRedGoal ? "#ef4444" : "#38bdf8";
          ctx.font = "bold 42px monospace";
          ctx.textAlign = "center";
          ctx.fillText("⚽ GOAL! GOAL! GOAL! ⚽", WIDTH / 2, HEIGHT / 2 + 15);
          ctx.restore();
        }

        // 6. Match Over Screen
        if (current.matchOver) {
          ctx.save();
          ctx.fillStyle = "rgba(7, 12, 23, 0.9)";
          ctx.fillRect(0, 0, WIDTH, HEIGHT);

          ctx.fillStyle = "#00ff87";
          ctx.font = "bold 36px monospace";
          ctx.textAlign = "center";
          ctx.shadowBlur = 18;
          ctx.shadowColor = "#00ff87";
          ctx.fillText("FULL TIME", WIDTH / 2, HEIGHT / 2 - 20);
          ctx.shadowBlur = 0;

          ctx.fillStyle = "#f8fafc";
          ctx.font = "bold 22px monospace";
          const winnerText = current.winnerTeam === "DRAW" ? "MATCH DRAW!" : `${current.winnerTeam} TEAM VICTORY!`;
          ctx.fillText(winnerText, WIDTH / 2, HEIGHT / 2 + 25);
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
        <span><b>Move:</b> WASD / Arrow Keys</span>
        <span><b>Aim:</b> Mouse Cursor</span>
        <span><b>Kick:</b> Space / Left Click</span>
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
