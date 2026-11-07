package com.nexora.network.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.network.candidate.CandidateAction;
import com.nexora.network.decision.ControllerDecisionResult;
import com.nexora.network.feedback.PredictionErrorRecord;
import com.nexora.network.prediction.NetworkPredictionResult;
import com.nexora.network.telemetry.NetworkSample;
import com.nexora.network.whatif.WhatIfResult;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Writes the Phase 10-15 pipeline to the database tables docs/DATABASE_DESIGN.md already
 * defines for it, so Phase 20's baseline-vs-proposed experiments have real data to compare.
 *
 * Performance Enhancement (Production Hardening):
 * Implements an asynchronous, bounded write-behind buffer for high-frequency telemetry samples,
 * decoupling game-loop message processing from synchronous MySQL disk I/O.
 */
@Service
public class NetworkPersistenceService {

    /** IDs a caller needs to link a later prediction-error row back to this cycle's decision. */
    public record PersistedCycle(Long predictionId, Long decisionId) { }

    private static final int BATCH_QUEUE_CAPACITY = 10_000;
    private static final int BATCH_FLUSH_SIZE = 100;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private static class RetryableTelemetryItem {
        final NetworkTelemetryEntity entity;
        int attempts = 0;
        RetryableTelemetryItem(NetworkTelemetryEntity entity) { this.entity = entity; }
    }

    private final BlockingQueue<RetryableTelemetryItem> telemetryBatchQueue = new LinkedBlockingQueue<>(BATCH_QUEUE_CAPACITY);
    private final java.util.Queue<RetryableTelemetryItem> retryHoldingQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService batchExecutor = Executors.newSingleThreadScheduledExecutor();

    private final java.util.concurrent.atomic.AtomicLong totalEnqueued = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalPersisted = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalFailedRetries = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalDropped = new java.util.concurrent.atomic.AtomicLong(0);

    private final NetworkTelemetryRepository telemetryRepository;
    private final NetworkPredictionRepository predictionRepository;
    private final CandidateActionRepository candidateActionRepository;
    private final WhatIfEvaluationRepository whatIfEvaluationRepository;
    private final ControllerDecisionRepository controllerDecisionRepository;
    private final PredictionErrorRepository predictionErrorRepository;
    private final ObjectMapper objectMapper;

    public NetworkPersistenceService(NetworkTelemetryRepository telemetryRepository,
                                      NetworkPredictionRepository predictionRepository,
                                      CandidateActionRepository candidateActionRepository,
                                      WhatIfEvaluationRepository whatIfEvaluationRepository,
                                      ControllerDecisionRepository controllerDecisionRepository,
                                      PredictionErrorRepository predictionErrorRepository,
                                      ObjectMapper objectMapper) {
        this.telemetryRepository = telemetryRepository;
        this.predictionRepository = predictionRepository;
        this.candidateActionRepository = candidateActionRepository;
        this.whatIfEvaluationRepository = whatIfEvaluationRepository;
        this.controllerDecisionRepository = controllerDecisionRepository;
        this.predictionErrorRepository = predictionErrorRepository;
        this.objectMapper = objectMapper;

        // Schedule write-behind batch flusher every 1000ms
        batchExecutor.scheduleWithFixedDelay(this::flushTelemetryBatch, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Bounded write-behind buffer: buffers telemetry sample in memory without blocking
     * the authoritative game loop.
     */
    public void recordTelemetry(Long sessionId, NetworkSample sample) {
        if (sample == null) return;
        NetworkTelemetryEntity entity = new NetworkTelemetryEntity(
                sessionId, sample.rttMs, sample.jitterMs, sample.packetLossPct, sample.throughputPacketsPerSec);
        totalEnqueued.incrementAndGet();
        boolean enqueued = telemetryBatchQueue.offer(new RetryableTelemetryItem(entity));
        if (!enqueued) {
            // Queue full under extreme load; drop or immediately flush to protect JVM memory
            totalDropped.incrementAndGet();
            flushTelemetryBatch();
            telemetryBatchQueue.offer(new RetryableTelemetryItem(entity));
        }
    }

    public int getQueueBacklog() {
        return telemetryBatchQueue.size() + retryHoldingQueue.size();
    }

    public int getQueueCapacity() {
        return BATCH_QUEUE_CAPACITY;
    }

    public long getTotalEnqueued() {
        return totalEnqueued.get();
    }

    public long getTotalPersisted() {
        return totalPersisted.get();
    }

    public long getTotalFailedRetries() {
        return totalFailedRetries.get();
    }

    public long getTotalDropped() {
        return totalDropped.get();
    }

    @Transactional
    public void flushTelemetryBatch() {
        // First re-enqueue any retry items
        RetryableTelemetryItem retryItem;
        while ((retryItem = retryHoldingQueue.poll()) != null) {
            if (!telemetryBatchQueue.offer(retryItem)) {
                totalDropped.incrementAndGet();
            }
        }

        if (telemetryBatchQueue.isEmpty()) return;

        List<RetryableTelemetryItem> batch = new ArrayList<>(BATCH_FLUSH_SIZE);
        telemetryBatchQueue.drainTo(batch, BATCH_FLUSH_SIZE);

        if (!batch.isEmpty()) {
            List<NetworkTelemetryEntity> entities = batch.stream().map(i -> i.entity).toList();
            try {
                telemetryRepository.saveAll(entities);
                totalPersisted.addAndGet(batch.size());
            } catch (Exception e) {
                totalFailedRetries.incrementAndGet();
                System.err.println("[NetworkPersistenceService] Batch telemetry save failed, rescheduling retry: " + e.getMessage());
                for (RetryableTelemetryItem item : batch) {
                    item.attempts++;
                    if (item.attempts <= MAX_RETRY_ATTEMPTS) {
                        retryHoldingQueue.offer(item);
                    } else {
                        totalDropped.incrementAndGet();
                        System.err.println("[NetworkPersistenceService] Telemetry item dropped after reaching max retries");
                    }
                }
            }
        }
    }

    @Transactional
    public PersistedCycle recordCycle(Long sessionId, NetworkPredictionResult prediction,
                                       ControllerDecisionResult decision) {
        NetworkPredictionEntity predictionEntity = predictionRepository.save(new NetworkPredictionEntity(
                sessionId, prediction.predictedRttMs, prediction.predictedJitterMs, prediction.predictedLossPct,
                prediction.predictedThroughput, prediction.confidence, prediction.horizonMs));
        Long predictionId = predictionEntity.getId();

        Map<CandidateAction, Long> candidateIds = new IdentityHashMap<>();
        for (WhatIfResult result : decision.allEvaluations) {
            Long candidateId = persistCandidate(predictionId, result);
            candidateIds.put(result.candidate, candidateId);
        }

        Long selectedCandidateId = candidateIds.get(decision.selected);
        ControllerDecisionEntity decisionEntity = controllerDecisionRepository.save(
                new ControllerDecisionEntity(sessionId, predictionId, selectedCandidateId, decision.confidenceGatePassed));

        return new PersistedCycle(predictionId, decisionEntity.getId());
    }

    @Transactional
    public void recordPredictionError(Long decisionId, PredictionErrorRecord error) {
        if (decisionId == null || error == null) return;
        predictionErrorRepository.save(new PredictionErrorEntity(
                decisionId, error.actualRttMs, error.actualLossPct, error.rttError, error.lossError));
    }

    private Long persistCandidate(Long predictionId, WhatIfResult result) {
        String parametersJson;
        try {
            parametersJson = objectMapper.writeValueAsString(result.candidate.parameters);
        } catch (Exception e) {
            parametersJson = "{}";
        }
        CandidateActionEntity candidateEntity = candidateActionRepository.save(
                new CandidateActionEntity(predictionId, result.candidate.actionType.name(), parametersJson));

        whatIfEvaluationRepository.save(new WhatIfEvaluationEntity(candidateEntity.getId(),
                result.consistencyCost, result.networkCost, result.processingCost,
                result.controlOverhead, result.totalScore));

        return candidateEntity.getId();
    }

    @PreDestroy
    public void shutdown() {
        // Drain pending queue on shutdown
        long deadline = System.currentTimeMillis() + 3000L;
        while (!telemetryBatchQueue.isEmpty() && System.currentTimeMillis() < deadline) {
            flushTelemetryBatch();
        }
        batchExecutor.shutdown();
        try {
            if (!batchExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
