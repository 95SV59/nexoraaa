package com.nexora.network.decision;

import com.nexora.multiplayer.engine.StateElementMetadata;
import com.nexora.multiplayer.engine.arena.NexoraArenaGame;
import com.nexora.network.candidate.ActionType;
import com.nexora.network.candidate.CandidateAction;
import com.nexora.network.candidate.CandidateGenerator;
import com.nexora.network.consistency.ConsistencyEngine;
import com.nexora.network.consistency.ConsistencyRiskAssessment;
import com.nexora.network.prediction.NetworkPredictionResult;
import com.nexora.network.whatif.WhatIfEvaluator;
import com.nexora.network.whatif.WhatIfResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DecisionControllerTest {

    private final DecisionController controller = new DecisionController(0.80);
    private final List<StateElementMetadata> arena = new NexoraArenaGame().getStateMetadata();

    private ControllerDecisionResult decide(double rtt, double jitter, double loss, double confidence,
                                             List<WhatIfResult>[] evalsOut) {
        NetworkPredictionResult p = new NetworkPredictionResult(
                System.currentTimeMillis(), rtt, jitter, loss, 60, confidence, 3000, 15);
        List<ConsistencyRiskAssessment> risks = new ConsistencyEngine().assess(p, arena);
        List<WhatIfResult> evals = new WhatIfEvaluator().evaluateAll(new CandidateGenerator().generate(risks), risks, p);
        if (evalsOut != null) evalsOut[0] = evals;
        return controller.decide(evals, p);
    }

    @Test
    void picksNoActionWhenNothingIsAtRisk() {
        ControllerDecisionResult d = decide(5, 2, 0, 0.95, null);
        assertEquals(ActionType.NO_ACTION, d.selected.actionType);
        assertTrue(d.confidenceGatePassed);
    }

    @Test
    void actsWhenAtRiskAndConfident() {
        ControllerDecisionResult d = decide(200, 30, 0, 0.95, null);
        assertEquals(ActionType.ENABLE_DEAD_RECKONING, d.selected.actionType);
        assertTrue(d.confidenceGatePassed);
    }

    @SuppressWarnings("unchecked")
    @Test
    void gateBlocksActionWhenConfidenceIsBelowThreshold() {
        List<WhatIfResult>[] evals = new List[1];
        ControllerDecisionResult d = decide(200, 30, 0, 0.50, evals);
        assertEquals(ActionType.NO_ACTION, d.selected.actionType);
        assertFalse(d.confidenceGatePassed);
        assertTrue(evals[0].stream().anyMatch(r -> r.candidate == d.selected));
    }

    @Test
    void stageARejectsInfeasibleCandidateEvenIfCostIsLower() {
        NetworkPredictionResult p = new NetworkPredictionResult(
                System.currentTimeMillis(), 150, 20, 0, 60, 0.95, 3000, 15);

        CandidateAction cheapInfeasible = new CandidateAction(
                ActionType.REDUCE_LOW_PRIORITY_RATE, List.of("player_pos"), Map.of());
        CandidateAction moreExpensiveFeasible = new CandidateAction(
                ActionType.ENABLE_DEAD_RECKONING, List.of("player_pos"), Map.of());
        CandidateAction noAction = CandidateAction.noAction();

        // cheapInfeasible has low totalScore (10.0) but feasible = false (hard constraint violation)
        WhatIfResult res1 = new WhatIfResult(cheapInfeasible, 2.0, 5.0, 1.0, 2.0, false, "Violates critical event delivery");
        // moreExpensiveFeasible has higher totalScore (15.0) but feasible = true
        WhatIfResult res2 = new WhatIfResult(moreExpensiveFeasible, 5.0, 6.0, 2.0, 2.0, true, "FEASIBLE");
        WhatIfResult resNoAction = new WhatIfResult(noAction, 20.0, 10.0, 0.0, 0.0, true, "FEASIBLE");

        List<WhatIfResult> evals = List.of(res1, res2, resNoAction);
        ControllerDecisionResult result = controller.decide(evals, p);

        // Stage A must reject cheapInfeasible and pick moreExpensiveFeasible
        assertEquals(ActionType.ENABLE_DEAD_RECKONING, result.selected.actionType);
        assertTrue(result.confidenceGatePassed);
    }

    @Test
    void stageAFallsBackToNoActionWhenAllActiveCandidatesInfeasible() {
        NetworkPredictionResult p = new NetworkPredictionResult(
                System.currentTimeMillis(), 150, 20, 0, 60, 0.95, 3000, 15);

        CandidateAction act1 = new CandidateAction(ActionType.REDUCE_LOW_PRIORITY_RATE, List.of("player_pos"), Map.of());
        CandidateAction act2 = new CandidateAction(ActionType.ENABLE_DEAD_RECKONING, List.of("player_pos"), Map.of());
        CandidateAction noAction = CandidateAction.noAction();

        WhatIfResult res1 = new WhatIfResult(act1, 2.0, 5.0, 1.0, 2.0, false, "Violates health invariant");
        WhatIfResult res2 = new WhatIfResult(act2, 5.0, 6.0, 2.0, 2.0, false, "Violates score invariant");
        WhatIfResult resNoAction = new WhatIfResult(noAction, 20.0, 10.0, 0.0, 0.0, false, "All violated");

        List<WhatIfResult> evals = List.of(res1, res2, resNoAction);
        ControllerDecisionResult result = controller.decide(evals, p);

        // Must fall back to safe default
        assertEquals(ActionType.NO_ACTION, result.selected.actionType);
        assertFalse(result.confidenceGatePassed);
        assertTrue(result.reason.contains("Stage A Gate"));
    }

    @Test
    void multiRoomIndependenceGuaranteesIsolatedHysteresis() {
        // Create controller with 10-second dwell time to test cross-room isolation
        DecisionController multiRoomCtrl = new DecisionController(0.80, 10000L);
        NetworkPredictionResult p = new NetworkPredictionResult(
                System.currentTimeMillis(), 200, 30, 0, 60, 0.95, 3000, 15);

        CandidateAction actDr = new CandidateAction(ActionType.ENABLE_DEAD_RECKONING, List.of("player_pos"), Map.of());
        CandidateAction actReduce = new CandidateAction(ActionType.REDUCE_LOW_PRIORITY_RATE, List.of("player_pos"), Map.of());
        CandidateAction actNo = CandidateAction.noAction();

        WhatIfResult resDr = new WhatIfResult(actDr, 2.0, 3.0, 1.0, 1.0, true, "FEASIBLE"); // best score: 7.0
        WhatIfResult resReduce = new WhatIfResult(actReduce, 4.0, 4.0, 1.0, 1.0, true, "FEASIBLE"); // score: 10.0
        WhatIfResult resNo = new WhatIfResult(actNo, 10.0, 5.0, 0.0, 0.0, true, "FEASIBLE");

        List<WhatIfResult> evals = List.of(resDr, resReduce, resNo);

        // Room A adopts ENABLE_DEAD_RECKONING
        ControllerDecisionResult dRoomA = multiRoomCtrl.decide("ROOM-A", evals, p);
        assertEquals(ActionType.ENABLE_DEAD_RECKONING, dRoomA.selected.actionType);

        // Within 100ms, Room B runs with REDUCE_LOW_PRIORITY_RATE as best candidate
        WhatIfResult resReduceBest = new WhatIfResult(actReduce, 1.0, 1.0, 1.0, 1.0, true, "FEASIBLE"); // score: 4.0
        List<WhatIfResult> evalsRoomB = List.of(resReduceBest, resDr, resNo);

        // Room B must NOT be blocked by Room A's dwell time!
        ControllerDecisionResult dRoomB = multiRoomCtrl.decide("ROOM-B", evalsRoomB, p);
        assertEquals(ActionType.REDUCE_LOW_PRIORITY_RATE, dRoomB.selected.actionType);
        assertFalse(dRoomB.reason.contains("Hysteresis guard"));

        // Now test within Room A: immediate switch attempt to REDUCE should be blocked by dwell time
        ControllerDecisionResult dRoomASecond = multiRoomCtrl.decide("ROOM-A", evalsRoomB, p);
        assertEquals(ActionType.ENABLE_DEAD_RECKONING, dRoomASecond.selected.actionType);
        assertTrue(dRoomASecond.reason.contains("Hysteresis guard"));

        // Clean room cleans up state
        multiRoomCtrl.clearRoom("ROOM-A");
        assertEquals(ActionType.NO_ACTION, multiRoomCtrl.getRoomState("ROOM-A").getCurrentActiveActionType());
    }
}
