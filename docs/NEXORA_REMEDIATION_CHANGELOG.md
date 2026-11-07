# NEXORA — REMEDIATION CHANGELOG
## Master Engineering Remediation & Defect Resolution Log

**Platform:** NEXORA — The Ultimate Gaming Hub  
**Date:** November 2026  
**Auditor / Team:** Principal Systems & Network Research Engineering Organization  

---

### Remediation Entry 1: P0.1 — Per-Client Adaptive Broadcasting & Delivery Partitioning
- **Target Files:**
  - `backend/src/main/java/com/nexora/network/decision/BroadcastRateRegistry.java`
  - `backend/src/main/java/com/nexora/multiplayer/engine/GameLoopManager.java`
  - `backend/src/main/java/com/nexora/multiplayer/transport/StompWebSocketTransportGateway.java`
  - `frontend/src/websocket/roomSocket.ts`
  - `frontend/src/hooks/useArenaSocket.ts`
  - `frontend/src/pages/multiplayer/MultiplayerArenaPage.tsx`
  - `backend/src/test/java/com/nexora/multiplayer/PerClientAdaptiveBroadcastingTest.java`
- **Original Defect:**
  The network controller computed individualized client throttles via `setClientEveryNTicks()`, but `GameLoopManager` only consulted the room-wide `getEveryNTicks()` setting and broadcasted the identical frame to `/topic/room/{code}/state`, causing throttled clients to receive the full unmitigated frame rate and defeating per-client adaptation.
- **Remediation Implementation:**
  1. Updated `GameLoopManager.tick()` to check `hasClientThrottles(roomCode)`.
  2. For every active non-bot participant, scheduled individual delivery via `transportGateway.sendToPlayer(roomCode, playerId, "state", snapshot, channel)`.
  3. Enforced client-specific modulo scheduling: `tickCount % clientEveryNTicks == 0`.
  4. Updated frontend `roomSocket.ts` to subscribe to player-specific topic `/topic/room/{roomCode}/player/{playerId}/state` alongside general room state.
  5. Implemented player removal and counter cleanup on disconnect.
- **Verification Evidence:**
  Created `PerClientAdaptiveBroadcastingTest.java` (3 tests). Tested 3 concurrent clients (Player 1 at $N=1$, Player 2 at $N=2$, Player 3 at $N=3$) over 30 simulation ticks at 30 Hz. Verified: Player 1 received 30 snapshots, Player 2 received 15 snapshots, Player 3 received 10 snapshots with zero cross-contamination.

---

### Remediation Entry 2: P0.2 — Transport Selection, Sequencing & Protocol Decoupling
- **Target Files:**
  - `backend/src/main/java/com/nexora/multiplayer/transport/TransportChannel.java`
  - `backend/src/main/java/com/nexora/multiplayer/transport/MultiplayerTransportGateway.java`
  - `backend/src/main/java/com/nexora/multiplayer/transport/StompWebSocketTransportGateway.java`
  - `backend/src/test/java/com/nexora/multiplayer/transport/MultiplayerTransportGatewayTest.java`
- **Original Defect:**
  `TransportChannel` declared `RELIABLE_ORDERED` and `UNRELIABLE_UNORDERED`, but the gateway lacked sequence numbering, channel usage auditing, and clear documentation of WebSocket TCP characteristics versus datagram semantics.
- **Remediation Implementation:**
  1. Enhanced `StompWebSocketTransportGateway` to manage atomic sequence numbers (`sequenceCounters`) per room.
  2. Added independent accounting metrics (`reliableCounters`, `unreliableCounters`).
  3. Added sequence stamping and channel routing for reliable vs datagram semantics.
  4. Documented the browser TCP/WebSocket constraint and specified WebRTC DataChannel as the UDP datagram upgrade architecture.
- **Verification Evidence:**
  `MultiplayerTransportGatewayTest.java` (4 tests) passing. Verified channel switching, message sequencing, and delivery metrics.

---

### Remediation Entry 3: P0.3 — Multi-Room DecisionController State Isolation
- **Target Files:**
  - `backend/src/main/java/com/nexora/network/decision/DecisionController.java`
  - `backend/src/main/java/com/nexora/network/NetworkControllerOrchestrator.java`
  - `backend/src/test/java/com/nexora/network/decision/DecisionControllerTest.java`
- **Original Defect:**
  The Spring singleton `DecisionController` stored `lastAdaptationTimeMillis` and `currentActiveActionType` in global instance fields. Adaptation in one room incorrectly triggered dwell-time hysteresis guards in separate rooms.
- **Remediation Implementation:**
  1. Introduced `RoomDecisionState` class encapsulating `lastAdaptationTimeMillis` and `currentActiveActionType`.
  2. Replaced global fields with a concurrent room-scoped map: `Map<String, RoomDecisionState> roomStates`.
  3. Provided `decide(roomCode, evaluations, prediction)` and `clearRoom(roomCode)`.
  4. Wired `stopForSession` in `NetworkControllerOrchestrator` to cleanly remove terminated room states.
- **Verification Evidence:**
  `DecisionControllerTest.java` (6 tests). Added `multiRoomIndependenceGuaranteesIsolatedHysteresis()` proving Room A's adaptation does not block Room B's policy updates.

---

### Remediation Entry 4: P0.4 — Virtual Queue Dimensional Consistency & Throughput Modeling
- **Target Files:**
  - `backend/src/main/java/com/nexora/network/whatif/VirtualQueueSimulator.java`
  - `backend/src/test/java/com/nexora/network/whatif/VirtualQueueSimulatorTest.java`
- **Original Defect:**
  Client incoming input packet volume was mistakenly substituted for outbound network link capacity in the virtual queue simulator without proper dimensional analysis.
- **Remediation Implementation:**
  1. Standardized physical units across queue simulations:
     - Backlog $Q(t)$: bytes [B]
     - Service capacity $\mu$: bytes/second [B/s]
     - Drain per tick: $\mu \times (\Delta t / 1000)$ [B/tick]
     - Queuing delay: $(Q / \mu) \times 1000$ [ms]
     - Total element staleness: $\Delta t_{\text{dispatch}} + \frac{\text{RTT}}{2} + \text{jitter} + d_{\text{queue}}$ [ms]
  2. Implemented TCP-friendly congestion loss backoff modeling when packet loss is detected.
- **Verification Evidence:**
  `VirtualQueueSimulatorTest.java` (3 tests). Added `dimensionalConsistencyUnderExtremeBandwidthConditions()` asserting backlog growth and queuing delay under bottleneck vs gigabit fiber.

---

### Remediation Entry 5: P0.5 — Centralized Horizon Configuration & Alignment
- **Target Files:**
  - `backend/src/main/java/com/nexora/network/PwncHorizonConfig.java`
  - `backend/src/main/java/com/nexora/network/NetworkControllerOrchestrator.java`
- **Original Defect:**
  Discrepancies existed between prediction horizons, simulation evaluation windows, and feedback measurement delays across different modules.
- **Remediation Implementation:**
  1. Created `PwncHorizonConfig` managing centralized configurable parameters:
     - `predictionHorizonMs` (default 500 ms, configurable via `nexora.network.prediction-horizon-ms`)
     - `simulationHorizonMs` (default 500 ms, aligned with prediction window)
     - `feedbackEvaluationDelayMs` (enforced equal to prediction horizon)
     - `cycleIntervalMs` (default 200 ms)
  2. Injected `PwncHorizonConfig` into `NetworkControllerOrchestrator`, preventing premature feedback evaluation before the intervention horizon has elapsed.
- **Verification Evidence:**
  Clean pipeline execution verified in `PipelineBehaviorTest` and `NetworkControllerOrchestratorTest`.

---

### Remediation Entry 6: P0.6 — Loss-Resistant Telemetry Persistence & Failure Observability
- **Target Files:**
  - `backend/src/main/java/com/nexora/network/persistence/NetworkPersistenceService.java`
  - `backend/src/main/java/com/nexora/network/lab/NetworkLabController.java`
  - `backend/src/test/java/com/nexora/network/lab/NetworkLabControllerTest.java`
- **Original Defect:**
  `flushTelemetryBatch()` drained items from `telemetryBatchQueue` prior to executing `saveAll()`. A database failure or timeout resulted in unrecoverable telemetry data loss.
- **Remediation Implementation:**
  1. Implemented `RetryableTelemetryItem` with retry tracking (`MAX_RETRY_ATTEMPTS = 3`).
  2. Failed database writes are caught, retry counters incremented, and items preserved in `retryHoldingQueue` for exponential re-enqueueing.
  3. Added observable atomic counters: `totalEnqueued`, `totalPersisted`, `totalFailedRetries`, `totalDropped`.
  4. Added graceful shutdown draining loop (up to 3000 ms) in `@PreDestroy shutdown()`.
  5. Exposed persistence metrics in `/api/network-lab/system-health`.
- **Verification Evidence:**
  `NetworkLabControllerTest.java` asserting queue backlogs, persistence counts, and failure metrics.

---

### Remediation Entry 7: P0.7 — Database Schema & Relational Column Synchronization
- **Target Files:**
  - `database/schema.sql`
  - Live MySQL Database: `nexora`
  - `backend/src/main/java/com/nexora/review/Review.java`
  - `backend/src/main/java/com/nexora/wishlist/Wishlist.java`
  - `backend/src/main/java/com/nexora/multiplayer/session/GameSession.java`
  - `backend/src/main/java/com/nexora/network/persistence/NetworkTelemetryEntity.java`
- **Original Defect:**
  MySQL schema desynchronization caused runtime SQL exceptions across core features:
  - `game_player` lacked `reconnect_token`, `disconnected_at`, `is_spectator`.
  - `game_session` lacked `controller_mode`.
  - `wishlist` lacked `added_at`.
  - `review` lacked `body`, `hours_played`, `is_spoiler`, `is_reported`.
  - `network_telemetry` lacked `player_id`.
- **Remediation Implementation:**
  1. Synchronized all 44 relational tables with full column parity in `database/schema.sql`.
  2. Applied schema migrations directly to the live MySQL instance.
  3. Added slug-or-id fallback lookups in `WishlistService` and `LibraryService`.
- **Verification Evidence:**
  Full CRUD and flow diagnostics passing 100% across Reviews, Wishlists, Libraries, and Sessions.

---

### Remediation Entry 8: P0.8 — API Routing, Security Permissions & WebSocket Reconnection Hardening
- **Target Files:**
  - `backend/src/main/java/com/nexora/security/SecurityConfig.java`
  - `backend/src/main/java/com/nexora/security/StompAuthChannelInterceptor.java`
  - `backend/src/main/java/com/nexora/exception/GlobalExceptionHandler.java`
  - `backend/src/main/java/com/nexora/user/RoleSeeder.java`
  - `frontend/src/hooks/useArenaSocket.ts`
  - `frontend/src/websocket/roomSocket.ts`
- **Original Defect:**
  - `SecurityConfig` blocked `/api/genres`, `/api/platforms`, `/api/leaderboard`, and `/api/analysis` with 403 Forbidden.
  - `GlobalExceptionHandler` lacked a handler for `ResponseStatusException`, converting 401s into 500 errors.
  - `useArenaSocket` initiated connections with `__none__` and an empty JWT, causing an infinite 3s reconnection storm.
  - `RoleSeeder` left dummy password hashes on seeded accounts.
- **Remediation Implementation:**
  1. Permitted public GET routes for `/api/genres/**`, `/api/platforms/**`, `/api/leaderboards/**`, `/api/achievements/**`, `/api/analysis/**`, and `/error`.
  2. Added `@ExceptionHandler(ResponseStatusException.class)` in `GlobalExceptionHandler`.
  3. Guarded `useArenaSocket` to idle cleanly when `!roomCode || roomCode === "__none__" || !jwt`.
  4. Enhanced `StompAuthChannelInterceptor` to support case-insensitive `Authorization` headers.
  5. Updated `RoleSeeder` to reliably set BCrypt hashes (`player123`, `admin123`).
- **Verification Evidence:**
  Automated live test suite verifying all 8 major feature groups with 100% PASS rate.
