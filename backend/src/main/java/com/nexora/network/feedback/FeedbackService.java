package com.nexora.network.feedback;

import com.nexora.network.prediction.NetworkPredictionResult;
import com.nexora.network.telemetry.NetworkSample;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 15 — Feedback. Closes the loop described in docs/PATENT_TECHNICAL_DOCUMENT.md
 * §4.9: once enough real time has passed for a prior prediction's horizon to have
 * elapsed, compare it to what telemetry actually measured, store the error, and expose
 * a smoothed bias correction that NetworkPredictor applies to its *next* prediction.
 * This is deliberately a simple exponentially-smoothed bias term, not a full online
 * learning model — sufficient to demonstrate a genuine closed loop for Phase 20's
 * baseline-vs-proposed experiments, and swappable later.
 */
@Component
public class FeedbackService {

    private static final int MAX_HISTORY_PER_ROOM = 50;
    private static final double SMOOTHING_ALPHA = 0.3; // weight on the newest error

    private final Map<String, Deque<PredictionErrorRecord>> history = new ConcurrentHashMap<>();
    private final Map<String, Double> rttBiasCorrection = new ConcurrentHashMap<>();

    public PredictionErrorRecord recordOutcome(String roomCode, NetworkPredictionResult predicted, NetworkSample actual) {
        if (actual == null) return null; // no telemetry arrived in time to compare against; skip this cycle

        PredictionErrorRecord record = new PredictionErrorRecord(
                System.currentTimeMillis(), predicted.predictedRttMs, actual.rttMs,
                predicted.predictedLossPct, actual.packetLossPct);

        history.computeIfAbsent(roomCode, k -> new ArrayDeque<>()).addLast(record);
        Deque<PredictionErrorRecord> deque = history.get(roomCode);
        while (deque.size() > MAX_HISTORY_PER_ROOM) {
            deque.removeFirst();
        }

        // Exponentially smoothed correction: if predictions have been running consistently
        // low, nudge future predictions up by (a damped fraction of) that error, and vice
        // versa. previousCorrection already reflects prior cycles, so this compounds gently
        // rather than overreacting to one noisy sample.
        double previousCorrection = rttBiasCorrection.getOrDefault(roomCode, 0.0);
        double newCorrection = previousCorrection + SMOOTHING_ALPHA * (record.rttError - previousCorrection);
        rttBiasCorrection.put(roomCode, newCorrection);

        return record;
    }

    /** Additive correction (ms) NetworkPredictor should apply to its next raw RTT prediction. */
    public double getRttBiasCorrection(String roomCode) {
        return rttBiasCorrection.getOrDefault(roomCode, 0.0);
    }

    public List<PredictionErrorRecord> getHistory(String roomCode) {
        return List.copyOf(history.getOrDefault(roomCode, new ArrayDeque<>()));
    }

    public void clear(String roomCode) {
        history.remove(roomCode);
        rttBiasCorrection.remove(roomCode);
    }
}
