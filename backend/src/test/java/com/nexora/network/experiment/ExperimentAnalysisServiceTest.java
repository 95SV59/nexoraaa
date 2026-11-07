package com.nexora.network.experiment;

import com.nexora.multiplayer.session.GameSession;
import com.nexora.multiplayer.session.GameSessionRepository;
import com.nexora.network.experiment.dto.BatchEvaluationRequest;
import com.nexora.network.experiment.dto.BatchEvaluationResultDto;
import com.nexora.network.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PWNC Empirical Research & Experiment Analysis Service Tests")
class ExperimentAnalysisServiceTest {

    private GameSessionRepository sessionRepository;
    private NetworkTelemetryRepository telemetryRepository;
    private NetworkPredictionRepository predictionRepository;
    private ControllerDecisionRepository decisionRepository;
    private CandidateActionRepository candidateActionRepository;
    private WhatIfEvaluationRepository whatIfEvaluationRepository;
    private PredictionErrorRepository predictionErrorRepository;
    private SessionBroadcastStatsRepository broadcastStatsRepository;

    private ExperimentAnalysisService analysisService;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(GameSessionRepository.class);
        telemetryRepository = mock(NetworkTelemetryRepository.class);
        predictionRepository = mock(NetworkPredictionRepository.class);
        decisionRepository = mock(ControllerDecisionRepository.class);
        candidateActionRepository = mock(CandidateActionRepository.class);
        whatIfEvaluationRepository = mock(WhatIfEvaluationRepository.class);
        predictionErrorRepository = mock(PredictionErrorRepository.class);
        broadcastStatsRepository = mock(SessionBroadcastStatsRepository.class);

        analysisService = new ExperimentAnalysisService(
                sessionRepository,
                telemetryRepository,
                predictionRepository,
                decisionRepository,
                candidateActionRepository,
                whatIfEvaluationRepository,
                predictionErrorRepository,
                broadcastStatsRepository
        );
    }

    @Test
    @DisplayName("Batch evaluation benchmarks all 5 network profiles with reproducible metrics")
    void testRunBatchEvaluationAcrossAllProfiles() {
        BatchEvaluationRequest req = new BatchEvaluationRequest();
        req.setPredictionModel("LINEAR_TREND");
        req.setDurationSec(10); // 10s run for unit test speed

        BatchEvaluationResultDto result = analysisService.runBatchEvaluation(req);

        assertNotNull(result);
        assertEquals("LINEAR_TREND", result.model);
        assertTrue(result.timestamp > 0);
        assertNotNull(result.profiles);
        assertEquals(5, result.profiles.size(), "Must evaluate EXCELLENT, GOOD, MODERATE, POOR, and UNSTABLE");

        // Verify EXCELLENT preserves full baseline bandwidth
        BatchEvaluationResultDto.ProfileResult excellent = result.profiles.stream()
                .filter(p -> "EXCELLENT".equalsIgnoreCase(p.profileName))
                .findFirst()
                .orElseThrow();
        assertEquals(0, excellent.interventionCount, "EXCELLENT profile should not require broadcast throttling");
        assertEquals(0.0, excellent.broadcastReductionPct, 0.01);

        // Verify POOR/UNSTABLE profile benchmark exists and evaluates
        BatchEvaluationResultDto.ProfileResult poor = result.profiles.stream()
                .filter(p -> "POOR".equalsIgnoreCase(p.profileName))
                .findFirst()
                .orElseThrow();
        assertNotNull(poor);
        assertTrue(poor.baselineAvgRttMs > 50.0);
    }

    @Test
    @DisplayName("Generates structured CSV export for session telemetry data")
    void testExportSessionCsv() {
        Long sessionId = 42L;
        GameSession mockSession = mock(GameSession.class);
        when(mockSession.getControllerMode()).thenReturn(GameSession.ControllerMode.PROPOSED);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));

        NetworkTelemetryEntity sample1 = new NetworkTelemetryEntity(sessionId, 45.0, 3.2, 0.0, 58.0);
        NetworkTelemetryEntity sample2 = new NetworkTelemetryEntity(sessionId, 52.0, 4.1, 0.5, 55.0);

        when(telemetryRepository.findBySessionId(sessionId)).thenReturn(List.of(sample1, sample2));

        String csv = analysisService.exportSessionCsv(sessionId);

        assertNotNull(csv);
        assertTrue(csv.contains("SessionId,42"));
        assertTrue(csv.contains("RecordedAt,RttMs,JitterMs,PacketLossPct,PacketRate"));
        assertTrue(csv.contains("45.00"));
        assertTrue(csv.contains("52.0"));
    }
}
