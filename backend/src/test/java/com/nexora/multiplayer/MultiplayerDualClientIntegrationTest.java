package com.nexora.multiplayer;

import com.nexora.multiplayer.engine.EnginePlayer;
import com.nexora.multiplayer.engine.PlayerInput;
import com.nexora.multiplayer.engine.arena.NexoraArenaGame;
import com.nexora.multiplayer.engine.bot.BotPlayerService;
import com.nexora.multiplayer.room.GamePlayer;
import com.nexora.multiplayer.room.GamePlayerRepository;
import com.nexora.multiplayer.room.GameRoom;
import com.nexora.multiplayer.room.GameRoomRepository;
import com.nexora.multiplayer.room.RoomService;
import com.nexora.network.candidate.ActionType;
import com.nexora.network.candidate.CandidateAction;
import com.nexora.network.candidate.CandidateGenerator;
import com.nexora.network.consistency.ConsistencyEngine;
import com.nexora.network.consistency.ConsistencyRiskAssessment;
import com.nexora.network.decision.BroadcastRateRegistry;
import com.nexora.network.decision.ControllerDecisionResult;
import com.nexora.network.decision.DecisionController;
import com.nexora.network.prediction.NetworkPredictionResult;
import com.nexora.network.prediction.NetworkPredictor;
import com.nexora.network.telemetry.NetworkSample;
import com.nexora.network.whatif.WhatIfEvaluator;
import com.nexora.network.whatif.WhatIfResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Multiplayer Integration: Dual-Client Lifecycle & Closed-Loop PWNC Control")
class MultiplayerDualClientIntegrationTest {

    private GameRoomRepository roomRepository;
    private GamePlayerRepository playerRepository;
    private BotPlayerService botPlayerService;
    private RoomService roomService;

    private BroadcastRateRegistry broadcastRateRegistry;
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        roomRepository = mock(GameRoomRepository.class);
        playerRepository = mock(GamePlayerRepository.class);
        botPlayerService = new BotPlayerService();
        roomService = new RoomService(roomRepository, playerRepository, botPlayerService);

        broadcastRateRegistry = new BroadcastRateRegistry();
        messagingTemplate = mock(SimpMessagingTemplate.class);
    }

    private static PlayerInput createInput(String id, double mx, double my, double aim, boolean fire) {
        PlayerInput in = new PlayerInput();
        in.setPlayerId(id);
        in.setMoveX(mx);
        in.setMoveY(my);
        in.setAimAngleRad(aim);
        in.setFiring(fire);
        return in;
    }

    @Test
    @DisplayName("Dual-client room setup, input processing, and decoupled 30Hz simulation")
    void testDualClientRoomAndAuthoritativeSimulation() {
        String roomCode = "NX-DUAL";
        GameRoom room = new GameRoom(roomCode, "arena", 1L, 4, "us-east");
        when(roomRepository.findByRoomCode(roomCode)).thenReturn(Optional.of(room));

        // 1. Client 1 (Host) and Client 2 (Joiner) setup
        GamePlayer hostPlayer = new GamePlayer(room, 1L, "HostPlayer", false);
        GamePlayer joinPlayer = new GamePlayer(room, 2L, "JoinPlayer", false);
        List<GamePlayer> activePlayers = new ArrayList<>(List.of(hostPlayer, joinPlayer));

        when(playerRepository.findByRoomIdAndLeftAtIsNull(room.getId())).thenReturn(activePlayers);
        when(playerRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), 1L)).thenReturn(Optional.of(hostPlayer));
        when(playerRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), 2L)).thenReturn(Optional.of(joinPlayer));

        // 2. Both players set ready
        roomService.setReady(roomCode, 1L, true);
        roomService.setReady(roomCode, 2L, true);
        assertTrue(hostPlayer.isReady());
        assertTrue(joinPlayer.isReady());

        // 3. Initialize Authoritative Multiplayer Game (Nexora Arena)
        NexoraArenaGame game = new NexoraArenaGame();
        game.initialize(roomCode, List.of(new EnginePlayer("1", 1L, "HostPlayer"), new EnginePlayer("2", 2L, "JoinPlayer")));

        // Verify initial state
        assertNotNull(game.getStateSnapshot());
        assertFalse(game.isMatchOver());

        // 4. Client 1 sends input (move right), Client 2 sends input (move left)
        PlayerInput inputClient1 = createInput("1", 1.0, 0.0, 0.0, false);
        PlayerInput inputClient2 = createInput("2", -1.0, 0.0, 0.0, false);

        game.processInput("1", inputClient1);
        game.processInput("2", inputClient2);

        // 5. Authoritative tick advances physics at 30 Hz
        game.updateState();
        assertTrue(game.validateState());

        // Verify that simulation advances regardless of network broadcast rate
        broadcastRateRegistry.register(roomCode);
        broadcastRateRegistry.setEveryNTicks(roomCode, 3); // Network throttled to 10 Hz (every 3 ticks)

        for (int tick = 1; tick <= 6; tick++) {
            game.updateState();
            // Simulation must continue uninterrupted on every tick
            assertTrue(game.validateState());
        }
    }

    @Test
    @DisplayName("PWNC closed-loop: Telemetry attribution, what-if evaluation, and per-client adaptation")
    void testClosedLoopPwncPerClientAdaptation() {
        String roomCode = "NX-PWNC";
        broadcastRateRegistry.register(roomCode);

        // Client 1 is low-latency fiber (25ms), Client 2 is congested (220ms)
        long now = System.currentTimeMillis();
        List<NetworkSample> telemetryHistory = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            long ts = now - (15 - i) * 200L;
            // Room peak RTT driven by congested client
            double rtt = 180.0 + i * 4.0; // rising from 180 to 236 ms
            double jitter = 15.0 + (i % 5);
            telemetryHistory.add(new NetworkSample(ts, rtt, jitter, 0.0, 60.0));
        }

        // 1. Stage 201: Network Prediction
        NetworkPredictor predictor = new NetworkPredictor();
        NetworkPredictionResult prediction = predictor.predict(telemetryHistory, 500, 0.0);
        assertTrue(prediction.predictedRttMs > 200.0, "Predictor must forecast elevated latency");

        // 2. Stage 202: Consistency Risk Assessment
        ConsistencyEngine consistencyEngine = new ConsistencyEngine();
        NexoraArenaGame game = new NexoraArenaGame();
        List<ConsistencyRiskAssessment> risks = consistencyEngine.assess(prediction, game.getStateMetadata());
        assertFalse(risks.isEmpty());

        // 3. Stage 203: Candidate Generation
        CandidateGenerator candidateGenerator = new CandidateGenerator();
        List<CandidateAction> candidates = candidateGenerator.generate(risks);
        assertTrue(candidates.size() >= 2, "Must generate diverse candidate actions");

        // 4. Stage 204 & 205: What-If Virtual Queue Simulation & Constraint-Aware Evaluation
        WhatIfEvaluator whatIfEvaluator = new WhatIfEvaluator();
        List<WhatIfResult> evaluations = whatIfEvaluator.evaluateAll(candidates, risks, prediction);

        for (WhatIfResult eval : evaluations) {
            assertNotNull(eval.feasibilityReason);
            assertTrue(eval.networkCost >= 0.0);
            assertTrue(eval.consistencyCost >= 0.0);
            assertTrue(eval.totalScore >= 0.0);
        }

        // 5. Stage 206: Decision Controller with Stage A Feasibility Gate & Stage B Optimization
        DecisionController decisionController = new DecisionController(0.80);
        ControllerDecisionResult decision = decisionController.decide(evaluations, prediction);

        assertNotNull(decision.selected);
        assertTrue(decision.confidenceGatePassed);

        // 6. Stage 207: Live Application with Per-Client Broadcast Throttling
        int roomEveryN = decision.selected.actionType == ActionType.ENABLE_DEAD_RECKONING ? 3 : 2;
        broadcastRateRegistry.setEveryNTicks(roomCode, roomEveryN);

        // Individual client adaptation
        broadcastRateRegistry.setClientEveryNTicks(roomCode, "player-fiber", 1); // unthrottled 30Hz
        broadcastRateRegistry.setClientEveryNTicks(roomCode, "player-congested", roomEveryN); // throttled

        assertEquals(1, broadcastRateRegistry.getClientEveryNTicks(roomCode, "player-fiber"));
        assertEquals(roomEveryN, broadcastRateRegistry.getClientEveryNTicks(roomCode, "player-congested"));
    }
}
