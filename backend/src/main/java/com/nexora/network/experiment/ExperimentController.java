package com.nexora.network.experiment;

import com.nexora.network.experiment.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Phase 20 REST surface. The intended workflow (see docs/ACADEMIC_RESEARCH_SPECIFICATION.md):
 *  1. POST /api/rooms with controllerMode=BASELINE, apply a NetworkLabProfile, play a match.
 *  2. Repeat with controllerMode=PROPOSED under the identical profile.
 *  3. GET /api/analysis/sessions to find the two session IDs (newest first).
 *  4. GET /api/analysis/compare?baselineSessionId=..&proposedSessionId=.. for the result.
 *  5. GET /api/analysis/compare/export/csv to download full comparative datasets.
 *  6. POST /api/analysis/batch-run for automated multi-model / multi-profile empirical benchmarking.
 */
@RestController
@RequestMapping("/api")
public class ExperimentController {

    private final NetworkExperimentRepository experimentRepository;
    private final ExperimentAnalysisService analysisService;

    public ExperimentController(NetworkExperimentRepository experimentRepository,
                                 ExperimentAnalysisService analysisService) {
        this.experimentRepository = experimentRepository;
        this.analysisService = analysisService;
    }

    @PostMapping("/experiments")
    public ResponseEntity<Long> createExperiment(@Valid @RequestBody CreateExperimentRequest req) {
        NetworkExperimentEntity saved = experimentRepository.save(new NetworkExperimentEntity(
                req.getName(), req.getGameId(), req.getNetworkProfile(), req.getLatencyMs(),
                req.getPacketLossPct(), req.getJitterMs(), req.getBandwidthKbps(),
                req.getNumPlayers(), req.getDurationSec()));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.getId());
    }

    @GetMapping("/analysis/sessions")
    public ResponseEntity<List<SessionListItemDto>> listSessions(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(analysisService.listRecentSessions(limit));
    }

    @GetMapping("/analysis/sessions/{sessionId}/summary")
    public ResponseEntity<SessionSummaryDto> sessionSummary(@PathVariable Long sessionId) {
        return ResponseEntity.ok(analysisService.summarize(sessionId));
    }

    @GetMapping(value = "/analysis/sessions/{sessionId}/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportSessionCsv(@PathVariable Long sessionId) {
        String csv = analysisService.exportSessionCsv(sessionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nexora-session-" + sessionId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/analysis/compare")
    public ResponseEntity<ComparisonReportDto> compare(@RequestParam Long baselineSessionId,
                                                         @RequestParam Long proposedSessionId) {
        return ResponseEntity.ok(analysisService.compare(baselineSessionId, proposedSessionId));
    }

    @GetMapping(value = "/analysis/compare/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportComparisonCsv(@RequestParam Long baselineSessionId,
                                                        @RequestParam Long proposedSessionId) {
        String csv = analysisService.exportComparisonCsv(baselineSessionId, proposedSessionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pwnc-comparison-" + baselineSessionId + "-vs-" + proposedSessionId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping("/analysis/batch-run")
    public ResponseEntity<BatchEvaluationResultDto> runBatchEvaluation(@RequestBody(required = false) BatchEvaluationRequest req) {
        if (req == null) {
            req = new BatchEvaluationRequest();
        }
        return ResponseEntity.ok(analysisService.runBatchEvaluation(req));
    }
}
