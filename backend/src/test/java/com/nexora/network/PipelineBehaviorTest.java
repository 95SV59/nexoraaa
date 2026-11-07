package com.nexora.network;

import com.nexora.multiplayer.engine.StateElementMetadata;
import com.nexora.multiplayer.engine.arena.NexoraArenaGame;
import com.nexora.network.candidate.ActionType;
import com.nexora.network.candidate.CandidateGenerator;
import com.nexora.network.consistency.ConsistencyEngine;
import com.nexora.network.consistency.ConsistencyRiskAssessment;
import com.nexora.network.decision.ControllerDecisionResult;
import com.nexora.network.decision.DecisionController;
import com.nexora.network.feedback.FeedbackService;
import com.nexora.network.prediction.NetworkPredictionResult;
import com.nexora.network.prediction.NetworkPredictor;
import com.nexora.network.telemetry.NetworkSample;
import com.nexora.network.whatif.WhatIfEvaluator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end behaviour of the REAL prediction -> consistency -> candidates -> what-if -> decision
 * -> feedback classes, driven by telemetry generated the way TelemetryCollector + NetworkLabService
 * would produce it under each preset profile (a virtual clock; no Spring, no sockets).
 *
 * This is what caught the controller throttling broadcasts on a perfect network. The bounds below
 * encode the CURRENT Arena calibration; if you retune the metadata or the cost model, change them
 * deliberately rather than loosening them to make a failure go away.
 */
class PipelineBehaviorTest {

    private static final double BASE_RTT_MS = 4;       // localhost websocket + handler time
    private static final long SAMPLE_MS = 2000, CYCLE_MS = 3000, HORIZON_MS = 3000, RUN_MS = 120_000;
    private static final int WARMUP_CYCLES = 8;         // ignore the first ~24s (window still filling)
    private static final int SEEDS = 20, PLAYERS = 2;

    private record Rates(double acting, double blocked) { }

    private static Rates run(double latency, double jitter, double lossPct) {
        List<StateElementMetadata> meta = new NexoraArenaGame().getStateMetadata();
        NetworkPredictor predictor = new NetworkPredictor();
        ConsistencyEngine consistency = new ConsistencyEngine();
        CandidateGenerator generator = new CandidateGenerator();
        WhatIfEvaluator whatIf = new WhatIfEvaluator();
        DecisionController decider = new DecisionController(0.80);

        int total = 0, acting = 0, blocked = 0;
        for (int seed = 0; seed < SEEDS; seed++) {
            Random rng = new Random(seed * 31L + (long) (latency * 7 + jitter * 3 + lossPct));
            List<NetworkSample> samples = telemetry(rng, latency, jitter, lossPct);
            FeedbackService feedback = new FeedbackService();
            List<Long> dueTimes = new ArrayList<>();
            List<NetworkPredictionResult> duePreds = new ArrayList<>();

            int cycle = 0;
            for (long t = CYCLE_MS; t <= RUN_MS; t += CYCLE_MS, cycle++) {
                while (!dueTimes.isEmpty() && dueTimes.get(0) <= t) {
                    feedback.recordOutcome("r", duePreds.remove(0), latestAtOrBefore(samples, dueTimes.remove(0)));
                }
                long realNow = System.currentTimeMillis();
                List<NetworkSample> window = new ArrayList<>();
                for (NetworkSample s : samples) {
                    if (s.recordedAtMillis <= t) {
                        window.add(new NetworkSample(realNow - (t - s.recordedAtMillis), s.rttMs, s.jitterMs,
                                s.packetLossPct, s.throughputPacketsPerSec));
                    }
                }
                if (window.size() > 15) window = window.subList(window.size() - 15, window.size());

                NetworkPredictionResult pred = predictor.predict(window, HORIZON_MS, feedback.getRttBiasCorrection("r"));
                List<ConsistencyRiskAssessment> risks = consistency.assess(pred, meta);
                ControllerDecisionResult d = decider.decide(
                        whatIf.evaluateAll(generator.generate(risks), risks, pred), pred);
                dueTimes.add(t + HORIZON_MS);
                duePreds.add(pred);

                if (cycle < WARMUP_CYCLES) continue;
                total++;
                if (!d.confidenceGatePassed) blocked++;
                else if (d.selected.actionType != ActionType.NO_ACTION) acting++;
            }
        }
        return new Rates((double) acting / total, (double) blocked / total);
    }

    /** Mirrors TelemetryCollector.aggregate(): a dropped pong leaves that player's last RTT/jitter in place. */
    private static List<NetworkSample> telemetry(Random rng, double latency, double jitter, double lossPct) {
        double[] lastRtt = new double[PLAYERS], lastJitter = new double[PLAYERS];
        Arrays.fill(lastRtt, -1);
        List<NetworkSample> out = new ArrayList<>();
        for (long t = 500; t <= RUN_MS + HORIZON_MS; t += SAMPLE_MS) {
            int received = 0;
            double throughput = 0;
            for (int i = 0; i < PLAYERS; i++) {
                if (rng.nextDouble() * 100 >= lossPct) {
                    double rtt = BASE_RTT_MS + rng.nextDouble() * 3
                            + Math.max(0, latency + (rng.nextDouble() * 2 - 1) * jitter);
                    lastJitter[i] = lastRtt[i] >= 0 ? Math.abs(rtt - lastRtt[i]) : 0;
                    lastRtt[i] = rtt;
                    received++;
                }
                throughput += 30.0 * (1 - lossPct / 100.0);
            }
            double maxRtt = 0, maxJitter = 0;
            for (int i = 0; i < PLAYERS; i++) {
                if (lastRtt[i] >= 0) {
                    maxRtt = Math.max(maxRtt, lastRtt[i]);
                    maxJitter = Math.max(maxJitter, lastJitter[i]);
                }
            }
            out.add(new NetworkSample(t, maxRtt, maxJitter, 100.0 * (1.0 - (double) received / PLAYERS), throughput));
        }
        return out;
    }

    private static NetworkSample latestAtOrBefore(List<NetworkSample> samples, long t) {
        NetworkSample best = null;
        for (NetworkSample s : samples) if (s.recordedAtMillis <= t) best = s;
        return best;
    }

    @Test
    void healthyNetworksLeaveTheBroadcastRateAlone() {
        assertTrue(run(0, 0, 0).acting() <= 0.02, "EXCELLENT must not trigger adaptation");
        assertTrue(run(30, 5, 0).acting() <= 0.02, "GOOD must not trigger adaptation");
    }

    @Test
    void degradedNetworksTriggerAdaptation() {
        assertTrue(run(80, 20, 1).acting() >= 0.90, "MODERATE should adapt");
        assertTrue(run(180, 50, 5).acting() >= 0.90, "POOR should adapt");
    }

    @Test
    void theConfidenceGateStaysMostlyOpenEvenWhenUnstable() {
        Rates unstable = run(300, 120, 12);
        assertTrue(unstable.acting() >= 0.80, "UNSTABLE should mostly adapt, got " + unstable);
        assertTrue(unstable.blocked() <= 0.20, "the gate should not block most cycles, got " + unstable);
    }

    @Test
    void adaptationIsMonotonicInNetworkQuality() {
        double excellent = run(0, 0, 0).acting();
        double moderate = run(80, 20, 1).acting();
        double poor = run(180, 50, 5).acting();
        assertTrue(excellent <= moderate && moderate <= poor + 1e-9);
    }
}
