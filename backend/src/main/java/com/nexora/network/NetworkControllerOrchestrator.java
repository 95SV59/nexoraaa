package com.nexora.network;

import com.nexora.multiplayer.engine.StateElementMetadata;
import com.nexora.multiplayer.session.GameSession;
import com.nexora.multiplayer.websocket.dto.NetworkControllerSnapshotMessage;
import com.nexora.network.candidate.ActionType;
import com.nexora.network.candidate.CandidateAction;
import com.nexora.network.candidate.CandidateGenerator;
import com.nexora.network.consistency.ConsistencyEngine;
import com.nexora.network.consistency.ConsistencyRiskAssessment;
import com.nexora.network.decision.BroadcastRateRegistry;
import com.nexora.network.decision.ControllerDecisionResult;
import com.nexora.network.decision.DecisionController;
import com.nexora.network.feedback.FeedbackService;
import com.nexora.network.persistence.NetworkPersistenceService;
import com.nexora.network.prediction.NetworkPredictionResult;
import com.nexora.network.prediction.NetworkPredictor;
import com.nexora.network.telemetry.NetworkSample;
import com.nexora.network.telemetry.TelemetryCollector;
import com.nexora.network.whatif.WhatIfEvaluator;
import com.nexora.network.whatif.WhatIfResult;
import jakarta.annotation.PreDestroy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Runs the full Phase 10-15 pipeline on a fixed cycle for every active match:
 * telemetry -> prediction -> consistency -> candidates -> what-if -> decision -> broadcast
 * -> persist -> (after the horizon elapses) outcome measurement -> feedback -> persist.
 * This is the class that makes docs/PATENT_TECHNICAL_DOCUMENT.md's mechanism actually
 * run, end to end, against real telemetry from a real match rather than being a design
 * document only — and, via NetworkPersistenceService, leaves a real trail in the database
 * for Phase 20's baseline-vs-proposed experiments to compare.
 */
@Component
public class NetworkControllerOrchestrator {

    private static final long CYCLE_INTERVAL_MS = 3000;
    private static final long PREDICTION_HORIZON_MS = 3000;
    private static final int TELEMETRY_WINDOW_SIZE = 15;

    private record RoomContext(List<StateElementMetadata> stateMetadata, Long sessionId,
                                GameSession.ControllerMode controllerMode, ScheduledFuture<?> task) { }

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final Map<String, RoomContext> activeRooms = new ConcurrentHashMap<>();

    private final TelemetryCollector telemetryCollector;
    private final NetworkPredictor predictor;
    private final ConsistencyEngine consistencyEngine;
    private final CandidateGenerator candidateGenerator;
    private final WhatIfEvaluator whatIfEvaluator;
    private final DecisionController decisionController;
    private final FeedbackService feedbackService;
    private final NetworkPersistenceService persistenceService;
    private final BroadcastRateRegistry broadcastRateRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final PwncHorizonConfig horizonConfig;
    @org.springframework.beans.factory.annotation.Autowired
    public NetworkControllerOrchestrator(TelemetryCollector telemetryCollector, NetworkPredictor predictor,
                                          ConsistencyEngine consistencyEngine, CandidateGenerator candidateGenerator,
                                          WhatIfEvaluator whatIfEvaluator, DecisionController decisionController,
                                          FeedbackService feedbackService, NetworkPersistenceService persistenceService,
                                          BroadcastRateRegistry broadcastRateRegistry,
                                          SimpMessagingTemplate messagingTemplate,
                                          @org.springframework.beans.factory.annotation.Autowired(required = false) PwncHorizonConfig horizonConfig) {
        this.telemetryCollector = telemetryCollector;
        this.predictor = predictor;
        this.consistencyEngine = consistencyEngine;
        this.candidateGenerator = candidateGenerator;
        this.whatIfEvaluator = whatIfEvaluator;
        this.decisionController = decisionController;
        this.feedbackService = feedbackService;
        this.persistenceService = persistenceService;
        this.broadcastRateRegistry = broadcastRateRegistry;
        this.messagingTemplate = messagingTemplate;
        this.horizonConfig = horizonConfig != null ? horizonConfig : new PwncHorizonConfig();
    }

    public NetworkControllerOrchestrator(TelemetryCollector telemetryCollector, NetworkPredictor predictor,
                                          ConsistencyEngine consistencyEngine, CandidateGenerator candidateGenerator,
                                          WhatIfEvaluator whatIfEvaluator, DecisionController decisionController,
                                          FeedbackService feedbackService, NetworkPersistenceService persistenceService,
                                          BroadcastRateRegistry broadcastRateRegistry,
                                          SimpMessagingTemplate messagingTemplate) {
        this(telemetryCollector, predictor, consistencyEngine, candidateGenerator, whatIfEvaluator,
                decisionController, feedbackService, persistenceService, broadcastRateRegistry, messagingTemplate, new PwncHorizonConfig());
    }

    public void startForSession(String roomCode, List<StateElementMetadata> stateMetadata, Long sessionId,
                                 GameSession.ControllerMode controllerMode) {
        long cycleInterval = horizonConfig.getCycleIntervalMs();
        activeRooms.computeIfAbsent(roomCode, code -> {
            ScheduledFuture<?> task = executor.scheduleAtFixedRate(
                    () -> runCycle(code), cycleInterval, cycleInterval, TimeUnit.MILLISECONDS);
            return new RoomContext(stateMetadata, sessionId, controllerMode, task);
        });
    }

    public void stopForSession(String roomCode) {
        RoomContext ctx = activeRooms.remove(roomCode);
        if (ctx != null) {
            ctx.task().cancel(false);
        }
        feedbackService.clear(roomCode);
        decisionController.clearRoom(roomCode);
    }

    public int getActiveRoomCount() {
        return activeRooms.size();
    }

    private void runCycle(String roomCode) {
        RoomContext ctx = activeRooms.get(roomCode);
        if (ctx == null) return;

        try {
            long horizonMs = horizonConfig.getPredictionHorizonMs();
            List<NetworkSample> recentSamples = telemetryCollector.getRecentSamples(roomCode, TELEMETRY_WINDOW_SIZE);
            double biasCorrection = feedbackService.getRttBiasCorrection(roomCode);
            NetworkPredictionResult prediction = predictor.predict(recentSamples, horizonMs, biasCorrection);

            List<ConsistencyRiskAssessment> risks = consistencyEngine.assess(prediction, ctx.stateMetadata());
            List<CandidateAction> candidates = candidateGenerator.generate(risks);
            List<WhatIfResult> evaluations = whatIfEvaluator.evaluateAll(candidates, risks, prediction);
            ControllerDecisionResult decision = decisionController.decide(roomCode, evaluations, prediction);

            // Phase 20: BASELINE mode runs the exact same prediction/candidate/what-if/decision
            // pipeline — so what it *would* have done is fully recorded for comparison — but
            // never actually changes the broadcast rate. That's what makes "run the same
            // NetworkLabProfile once as BASELINE, once as PROPOSED" a fair comparison rather
            // than comparing two different pipelines.
            boolean isProposed = ctx.controllerMode() == GameSession.ControllerMode.PROPOSED;
            int appliedEveryNTicks = isProposed ? mapActionToBroadcastThrottle(decision.selected.actionType) : 1;
            broadcastRateRegistry.setEveryNTicks(roomCode, appliedEveryNTicks);

            // Phase 14 & Patent Stage 207: Per-client rate adaptation
            Map<String, TelemetryCollector.ClientTelemetrySnapshot> clientSnapshots =
                    telemetryCollector.getClientSnapshots(roomCode);
            for (Map.Entry<String, TelemetryCollector.ClientTelemetrySnapshot> entry : clientSnapshots.entrySet()) {
                String playerId = entry.getKey();
                var snapshot = entry.getValue();
                int clientThrottle = appliedEveryNTicks;
                if (isProposed && snapshot.rtt() > 180.0) {
                    clientThrottle = Math.max(appliedEveryNTicks, 2);
                }
                if (isProposed && snapshot.rtt() > 300.0) {
                    clientThrottle = Math.max(appliedEveryNTicks, 3);
                }
                broadcastRateRegistry.setClientEveryNTicks(roomCode, playerId, clientThrottle);
            }

            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/network",
                    NetworkControllerSnapshotMessage.build(roomCode, prediction, risks, decision, appliedEveryNTicks));

            NetworkPersistenceService.PersistedCycle persisted =
                    persistenceService.recordCycle(ctx.sessionId(), prediction, decision);

            // Phase 15: once this prediction's horizon has actually elapsed, compare it to
            // what telemetry really measured, feed the error back for the next cycle, and
            // persist it against the decision this cycle just wrote.
            executor.schedule(() -> {
                NetworkSample actual = telemetryCollector.getLatestSample(roomCode);
                var errorRecord = feedbackService.recordOutcome(roomCode, prediction, actual);
                persistenceService.recordPredictionError(persisted.decisionId(), errorRecord);
            }, horizonConfig.getFeedbackEvaluationDelayMs(), TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            System.err.println("[NetworkControllerOrchestrator] cycle failed for room " + roomCode + ": " + e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    /**
     * The actual "Live Network Control" policy: how each ActionType translates into a
     * concrete broadcast throttle. REDUCE_LOW_PRIORITY_RATE and AGGREGATE_UPDATES send
     * GAME_STATE half as often; ENABLE_DEAD_RECKONING leans on client-side interpolation
     * (see ArenaCanvas) to go further. PRIORITIZE_CRITICAL maps to full rate and is no longer
     * proposed by CandidateGenerator at all — genuine packet-priority reordering isn't
     * implementable over STOMP's simple broker, and proposing an action the live system can't
     * apply lets the what-if model win with it (see CandidateGenerator.LIVE_APPLICABLE). This mapping
     * is the experimental design point, same spirit as the cost model in
     * docs/ARCHITECTURE.md §5 — tune it once Phase 20 has real measurements.
     */
    private int mapActionToBroadcastThrottle(ActionType actionType) {
        return switch (actionType) {
            case REDUCE_LOW_PRIORITY_RATE, AGGREGATE_UPDATES -> 2;
            case ENABLE_DEAD_RECKONING -> 3;
            case PRIORITIZE_CRITICAL, NO_ACTION -> 1;
        };
    }
}
