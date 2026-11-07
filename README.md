# NEXORA — The Ultimate Gaming Hub
### Full-Stack Responsive Gaming Platform with Predictive What-If Network Control

> **Tagline:** *Play. Connect. Compete. Adapt.*  
> **Academic Project:** Four-Member Capstone in Computer Networks & Distributed Systems  
> **Core Innovation:** Predictive What-If Network Controller (PWNC) for Real-Time State Consistency Optimization

---

## 1. System Overview

**NEXORA** is a full-stack web gaming platform combining:
1. **Steam-Inspired Gaming Ecosystem:** Game discovery, categorized game libraries, user reviews and ratings, wishlists, gamer profiles, community news, and administrative dashboards.
2. **Authoritative Browser Multiplayer Arena:** Real-time multiplayer gaming with 30 Hz server loops, room codes, lobbies, and autonomous AI bot players that enable instant play with any player count.
3. **Predictive What-If Network Controller (Core Patent Pipeline):** An experimental closed-loop networking system that predicts latency degradation before it causes desynchronization, evaluates mitigation policies in a virtual sandbox, passes a confidence gate, and dynamically modulates live broadcast rates to maintain gameplay consistency.

---

## 2. System Architecture & 5-Layer Stack

```
┌────────────────────────────────────────────────────────────────────────┐
│                        LAYER 1: PRESENTATION                           │
│   React 18 + TypeScript + Vite + Cyberpunk Design System + Recharts    │
│   Catalogue • Game Details • Multiplayer Arena • HUD • Network Lab •   │
│   Library • Wishlist • Reviews • Articles • Profile • Admin Dashboard  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ REST / STOMP over WebSocket
┌───────────────────────────────────▼────────────────────────────────────┐
│                    LAYER 2: APPLICATION & REST APIS                    │
│   Spring Boot 3 + Spring Security + Stateless JWT Authentication       │
│   User & Role RBAC • Catalogue Discovery • Wishlist • Library Tracking │
│   Community Reviews & Likes • News Articles • Rule Recommendations    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ STOMP Channel / SockJS
┌───────────────────────────────────▼────────────────────────────────────┐
│                  LAYER 3: REAL-TIME MULTIPLAYER                        │
│   Authoritative Game Loop (30 Hz Tick) • Room & Lobby Management       │
│   BotPlayerService • STOMP Broadcast Rate Modulation • Reconnection   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ State Metadata & Snapshots
┌───────────────────────────────────▼────────────────────────────────────┐
│                       LAYER 4: GAME ENGINE                             │
│   NEXORA Arena (Shooter)        • NEXORA Racers (Kart Racing)          │
│   NEXORA Football (Arcade)      • NEXORA Tag (Arena Tag)               │
│   Authoritative State • Collision / Rules • Dead Reckoning Extrapolation│
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Telemetry & State Constraints
┌───────────────────────────────────▼────────────────────────────────────┐
│               LAYER 5: PREDICTIVE NETWORK CONTROLLER                   │
│   1. Telemetry Collector (RTT, Jitter, Packet Loss, Throughput)        │
│   2. Future Network Condition Predictor (Linear Trend + Horizon)       │
│   3. Game-State Consistency Impact Assessor                            │
│   4. Candidate Network-Control Generator                               │
│   5. Virtual What-If Evaluator (Consistency vs. Resource Cost)         │
│   6. Confidence Gate Evaluation (Threshold Check, e.g. 0.80)           │
│   7. Live Network Controller (Dynamic Broadcast Rate Modulation)       │
│   8. Actual Outcome Measurement (Post-Control Telemetry)               │
│   9. Prediction-Error Feedback Loop (Model Adjustment)                 │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Steam-Platform Major Features

NEXORA provides a complete gaming community and library management layer:

### 3.1 Store & Discovery Catalogue (`/games`)
- **Search & Filter:** Search by title, filter by Genre (Action, Racing, Sports, Strategy, Party) and Platform (Web Browser, PC Desktop, Mobile).
- **Sort Options:** Sort alphabetically by title or release date with pagination.
- **Game Cards:** Displays player count limits, multiplayer status, genre tags, and instant access to detail pages.

### 3.2 Game Details Page (`/games/:slug`)
- **Hero Banners & Screenshots:** Full visual previews with responsive aspect ratio.
- **Direct Play Launcher:** Games with live engines display a **▶ Play Multiplayer Now** button that instantly directs to room creation.
- **Wishlist Toggle:** One-click add/remove from your personal wishlist.
- **Library Tracker:** Dropdown to set your personal play status (*Currently Playing*, *Completed*, *Plan to Play*, *On Hold*, *Dropped*).
- **Community Reviews Section:** Read, rate, and like reviews directly below game metadata.

### 3.3 Steam-Style Library (`/library`)
- **Personal Game Tracker:** Categorized tabs for **All Games**, **Playing**, **Completed**, **Plan to Play**, **On Hold**, and **Dropped**.
- **Playtime & Activity:** Displays accumulated hours played and last-played timestamps.
- **Quick Launch:** Jump straight into live matches from your library.

### 3.4 Wishlist (`/wishlist`)
- Save upcoming or favorite games with quick removal and direct links to play or browse.

### 3.5 Community Reviews & Ratings
- 1–10 numerical rating system + binary **Recommended / Not Recommended** indicator.
- Community like count on reviews to highlight high-value community feedback.
- Reviews calculate and update overall game rating scores in real time.

### 3.6 Gaming News & Tech Dispatches (`/news`)
- Platform announcements, networking research breakthroughs, tournament alerts, and patch notes.
- Filter by category (*Announcements*, *Network Research*, *Patch Notes*) or search keywords.

### 3.7 Gamer Profile (`/profile`)
- Steam-like player profile displaying username, member since date, role badge (`PLAYER` or `ADMIN`), and customizable avatar (integrated with DiceBear Bottts).
- Profile edit functionality for email and avatar URLs.

### 3.8 Admin Dashboard (`/admin`)
- Accessible to users with `ROLE_ADMIN` (`admin` / `admin123`).
- Create and publish new game entries and media.
- User management table displaying all registered users, roles, and account statuses.
- Platform analytics: total users, active games, total reviews, and published articles.

---

## 4. Real-Time Multiplayer Suite & AI Bot System

All games execute with a **server-authoritative loop** running at a fixed **30 Hz tick rate** ($33.\overline{3}\text{ ms}$ interval). Connected browser clients stream player input vectors and receive periodic state snapshots over STOMP WebSocket.

### 4.1 The 4 Original Playable Games
1. **NEXORA Arena (2D Tactical Shooter):**
   - 1 to 8 players.
   - Top-down shooter with player movement, obstacles, health packs, projectile ballistics, and score tracking.
   - Synchronization focus: High-frequency projectile and position consistency.
2. **NEXORA Racers (Multiplayer Kart Racing):**
   - 1 to 8 players.
   - High-octane kart racer with vehicle physics, steering drift, throttle/braking, checkpoint tracking, and lap times.
   - Synchronization focus: Dead reckoning extrapolation, velocity vectors, and angular heading prediction.
3. **NEXORA Football (Arcade Soccer):**
   - 1 to 6 players (2v2 or 3v3 team match).
   - Ball physics, carrier possession, tackling, goal detection, and match timer.
   - Synchronization focus: Strict hierarchy of consistency constraints (Ball position = CRITICAL, Score = VERY HIGH, Cosmetic = LOW).
4. **NEXORA Tag (Arena Tag):**
   - 1 to 10 players.
   - One player designated as "IT", tag transfer on contact, tag immunity cooldown, speed boosts, and timer.
   - Synchronization focus: Rapid state transitions and latency compensation.

### 4.2 Automated AI Bot Players (`BotPlayerService`)
- Matches do not require waiting for 8 human players. The host can click **"🤖 Fill with Bots"** or **"▶ Start Now"** in the lobby to start immediately with any number of joined players (including solo testing).
- Each game has an autonomous `BotBrain` implementation:
  - `ArenaBotBrain`: Tracks nearest living enemy, calculates aiming angle with slight humanized jitter, and fires when within 600px range.
  - `RacersBotBrain`: Steers toward the next sequential checkpoint on the track at 80% continuous throttle.
  - `FootballBotBrain`: Chases the ball, shoots toward the goal when in possession, and slide-tackles opponents carrying the ball.
  - `TagBotBrain`: Actively pursues human runners when "IT"; flees toward open space when hunted.
- **Academic Research Purity:** AI bots are simulated purely inside the game engine and do not emit fake network pings. The patent controller measures real human WebSocket traffic only.

---

## 5. Predictive What-If Network Controller (Core Patent Pipeline)

The system’s core research contribution is a 9-stage closed-loop pipeline running on the authoritative server:

```
[1. Telemetry Collector]
        │ (Server-originated PING/PONG probes: RTT, Jitter, Loss %, Throughput)
        ▼
[2. Future Network Condition Predictor]
        │ (Linear trend extrapolation over prediction horizon H = 1000ms)
        ▼
[3. Game-State Consistency Impact Assessor]
        │ (Classifies per-element consistency risk: CRITICAL vs HIGH vs LOW)
        ▼
[4. Candidate Network-Control Generator]
        │ (NO_OP, THROTTLE_TO_15HZ, THROTTLE_TO_10HZ, PRIORITIZE_CRITICAL)
        ▼
[5. Virtual What-If Evaluator]
        │ (Scores candidates in virtual sandbox: J(a) = w_c*C_consistency + w_n*C_network)
        ▼
[6. Confidence Gate]
        │ (Enforces threshold check: Confidence Gamma >= 0.80)
        ▼
[7. Live Network Controller]
        │ (Modulates outbound STOMP GAME_STATE broadcast rate)
        ▼
[8. Actual Outcome Evaluator]
        │ (Measures post-control network metrics & state divergence)
        ▼
[9. Prediction-Error Feedback Loop]
          (Computes prediction error epsilon and updates model weights)
```

### 5.1 Conducting a Network Lab Experiment
1. Navigate to **Multiplayer Arena** (`/play/arena`).
2. Select any game (e.g., *NEXORA Arena* or *NEXORA Racers*).
3. **Session 1 — BASELINE:**
   - Choose **BASELINE** mode.
   - Create a room and click **Start Now (Fill Bots)**.
   - In the sidebar **Network Lab Panel**, select `POOR` (180ms RTT, 50ms Jitter, 5% Loss).
   - Play for 30–60 seconds, then exit. Note the Session ID (e.g., `#1`).
4. **Session 2 — PROPOSED:**
   - Create a room in **PROPOSED** mode with the same game.
   - Start match and select the identical `POOR` profile in the Network Lab Panel.
   - Observe the live **Network Status Panel**: the controller detects degradation, runs what-if evaluations, passes the confidence gate, and throttles the broadcast rate while client-side dead reckoning maintains visual smoothness.
   - Finish match. Note Session ID (e.g., `#2`).
5. **Analyze the Results (`/experiments`):**
   - Select Session `#1` as Baseline and Session `#2` as Proposed.
   - Click **Compare**.
   - Review comparative analytics: broadcast packet reduction (30%–58%), consistency score, confidence gate pass rate, and comparative Recharts time-series graphs.

---

## 6. Real-Time STOMP WebSocket Protocol Reference

All real-time communications operate over WebSocket with the STOMP subprotocol at `/ws` using SockJS.

### 6.1 Authentication
Clients supply their JWT token during the STOMP `CONNECT` frame:
```javascript
new Client({
  webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
  connectHeaders: { Authorization: `Bearer ${jwt}` }
});
```
`StompAuthChannelInterceptor` authenticates the token on the server and attaches the user's `Principal` to the session. Client messages do not pass spoofable user IDs; all handlers identify players via `principal.getName()`.

### 6.2 Topic Routing & Message Types

| Destination | Direction | Payload Schema | Purpose |
|---|---|---|---|
| `/app/room/{roomCode}/ready` | Client → Server | `{"ready": true}` | Toggle player ready status in lobby. |
| `/app/room/{roomCode}/input` | Client → Server | `PlayerInput` (`moveX, moveY, aimAngleRad, firing, actionPressed`) | High-frequency continuous player inputs. |
| `/app/room/{roomCode}/pong` | Client → Server | `{"clientTimestamp": 123456789}` | Server-timed RTT echo probe. |
| `/topic/room/{roomCode}/lobby` | Server → Client | `LobbyUpdateMessage` (`roomCode, status, players[]`) | Live lobby player roster & ready states. |
| `/topic/room/{roomCode}/state` | Server → Client | `GameStateSnapshot` (game-specific entity positions) | Authoritative game-state updates. |
| `/topic/room/{roomCode}/lifecycle` | Server → Client | `MatchLifecycleMessage` (`event, matchResult`) | `GAME_START` and `GAME_END` events. |
| `/topic/room/{roomCode}/ping` | Server → Client | `PingBroadcast` (`serverTimestamp`) | Server-initiated latency probe every 2s. |
| `/topic/room/{roomCode}/network` | Server → Client | `NetworkControllerSnapshotMessage` | Live 9-stage patent controller telemetry. |

---

## 7. REST API Surface Reference

| Category | Endpoint | Method | Access | Description |
|---|---|---|---|---|
| **Auth** | `/api/auth/register` | `POST` | Public | Register new account. |
| | `/api/auth/login` | `POST` | Public | Authenticate and obtain JWT token. |
| **User** | `/api/users/me` | `GET` | Authenticated | Retrieve authenticated user profile. |
| | `/api/users/profile` | `PUT` | Authenticated | Update user email and avatar. |
| **Catalogue** | `/api/games` | `GET` | Public | Browse, filter, search, sort, and paginate games. |
| | `/api/games/{slug}` | `GET` | Public | Detailed game metadata, media, and platform support. |
| | `/api/genres`, `/api/platforms` | `GET` | Public | Lookup lists for catalogue filtering. |
| **Library** | `/api/library` | `GET` | Authenticated | Retrieve user game library (filter by status). |
| | `/api/library/status` | `PUT` | Authenticated | Update game library status (*PLAYING*, *COMPLETED*, etc.). |
| | `/api/library/stats` | `GET` | Authenticated | Summary counts of games per category. |
| | `/api/library/{slug}` | `DELETE` | Authenticated | Remove game from personal library. |
| **Wishlist** | `/api/wishlist` | `GET` | Authenticated | Retrieve user wishlist items. |
| | `/api/wishlist/{slug}` | `POST` | Authenticated | Add game to wishlist. |
| | `/api/wishlist/{slug}` | `DELETE` | Authenticated | Remove game from wishlist. |
| **Reviews** | `/api/reviews/game/{slug}` | `GET` | Public | List reviews for a specific game. |
| | `/api/reviews/game/{slug}` | `POST` | Authenticated | Submit or update user review and rating. |
| | `/api/reviews/{id}/like` | `POST` | Authenticated | Like/unlike a review. |
| **News** | `/api/articles` | `GET` | Public | Browse published news articles (filter by category/search). |
| | `/api/articles/categories` | `GET` | Public | List article categories. |
| **Rooms** | `/api/rooms` | `POST` | Authenticated | Create a new multiplayer room. |
| | `/api/rooms/join` | `POST` | Authenticated | Join room via 6-character room code. |
| | `/api/rooms/{code}` | `GET` | Authenticated | Retrieve room status snapshot. |
| | `/api/rooms/{code}/leave` | `POST` | Authenticated | Leave active room. |
| | `/api/rooms/{code}/fill-bots` | `POST` | Host Only | Fill empty room slots with AI bots. |
| | `/api/rooms/{code}/start-now` | `POST` | Host Only | Fill remaining slots with bots and start immediately. |
| **Network Lab** | `/api/network-lab/{code}` | `POST` | Host Only | Inject network degradation profile (*POOR*, *UNSTABLE*, etc.). |
| | `/api/network-lab/{code}` | `DELETE` | Host Only | Reset room network condition to normal. |
| | `/api/experiments/compare` | `GET` | Authenticated | Compare baseline vs proposed session statistics. |
| **Admin** | `/api/admin/games` | `POST` | Admin Only | Create new game in the catalogue. |
| | `/api/admin/users` | `GET` | Admin Only | List all registered platform users. |
| | `/api/admin/stats` | `GET` | Admin Only | Platform statistics and engagement metrics. |

---

## 8. Unified Database Schema (`database/schema.sql`)

All persistence is defined in a single authoritative script: [`database/schema.sql`](file:///D:/NEXORA/database/schema.sql).

### Relational Schema Summary (28 Tables):
- **User & Security:** `role`, `user`.
- **Catalogue & Media:** `genre`, `platform`, `game`, `game_platform`, `game_media`, `game_tag`, `game_game_tag`.
- **Community & Steam Features:** `review`, `review_like`, `review_report`, `wishlist`, `user_library`.
- **News Dispatches:** `article_category`, `article`.
- **Multiplayer Arena:** `game_room`, `game_player` (with `is_bot` flag), `game_session`, `session_broadcast_stats`.
- **Predictive Network Controller:** `network_experiment`, `network_telemetry`, `network_prediction`, `candidate_action`, `whatif_evaluation`, `controller_decision`, `prediction_error`.

---

## 9. Setup, Execution & Testing Guide (Zero Docker)

### Prerequisites
- **Java Development Kit (JDK):** Version 17 or higher (Java 21/26 compatible).
- **Node.js:** Version 18 or higher (with npm).
- **MySQL Database Server:** Version 8.0 or higher running on port 3306.

---

### Step 1: Database Setup
Execute the unified schema script against your local MySQL instance:
```powershell
mysql -u nexora -pnexora < D:\NEXORA\database\schema.sql
```
*(Or open MySQL Workbench, connect to `localhost:3306`, and run `D:\NEXORA\database\schema.sql`)*

---

### Step 2: Run Backend (Spring Boot 3)
```powershell
cd D:\NEXORA\backend

# Option A: Run the pre-compiled production fat jar
java --enable-native-access=ALL-UNNAMED -jar target\nexora-backend-0.0.1.jar

# Option B: Run via Maven wrapper
.\mvnw.cmd spring-boot:run
```
- REST API: `http://localhost:8080/api`
- WebSocket STOMP endpoint: `http://localhost:8080/ws`

---

### Step 3: Run Frontend (React 18 + Vite)
```powershell
cd D:\NEXORA\frontend

# Start Vite development server
npm run dev
```
- Web Application: `http://localhost:5173`

---

### Step 4: Default Pre-Seeded Accounts

| Username | Password | Role | Privileges |
|---|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` | Full Admin Dashboard access, user moderation, article publishing, game creation. |
| `player1` | `player123` | `ROLE_PLAYER` | Standard multiplayer player account. |
| `player2` | `player123` | `ROLE_PLAYER` | Secondary player for multi-browser testing. |

---

### Step 5: Automated Test Suite

Run all automated unit, integration, and game-engine tests:
```powershell
cd D:\NEXORA\backend
.\mvnw.cmd test
```
The test suite validates:
- **`PipelineBehaviorTest`:** Full 9-stage patent controller closed-loop behavior under varying simulated telemetry.
- **`NexoraArenaGameTest`, `NexoraRacersGameTest`, `NexoraFootballGameTest`, `NexoraTagGameTest`:** Authoritative physics, input processing, boundary collisions, and scoring logic.
- **`CandidateGeneratorTest`, `ConsistencyEngineTest`, `DecisionControllerTest`, `NetworkPredictorTest`:** What-if virtual scoring, consistency risk classification, confidence gating, and linear trend projection.
- **`JwtServiceTest`:** Token generation, claims extraction, HMAC-SHA256 signature verification, tamper rejection, and expiration handling.

---

## 10. Project Documentation Index

The platform documentation is maintained in two master documents:
1. **[`README.md`](file:///D:/NEXORA/README.md) (This Document):** Master system overview, engineering architecture, Steam platform features, multiplayer games, AI bot design, WebSocket protocols, REST APIs, database schema, and operational guide.
2. **[`docs/ACADEMIC_RESEARCH_SPECIFICATION.md`](file:///D:/NEXORA/docs/ACADEMIC_RESEARCH_SPECIFICATION.md):** Formal capstone academic report, patent technical disclosure, mathematical formulations, multi-objective cost functions, and empirical experimental evaluation.
