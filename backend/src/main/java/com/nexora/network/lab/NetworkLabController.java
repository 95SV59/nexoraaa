package com.nexora.network.lab;

import com.nexora.network.NetworkControllerOrchestrator;
import com.nexora.network.lab.dto.LabProfileResponse;
import com.nexora.network.lab.dto.SetLabProfileRequest;
import com.nexora.network.persistence.NetworkPersistenceService;
import com.nexora.network.telemetry.TelemetryCollector;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Phase 16 REST surface & Operational Health Telemetry.
 * Provides controls to set artificial impairment profiles on rooms and exposes
 * real-time JVM, telemetry backlog, and active PWNC orchestration statistics.
 */
@RestController
@RequestMapping("/api/network-lab")
public class NetworkLabController {

    public record NetworkSystemHealthResponse(
            long timestamp,
            String status,
            int activeTrackedRooms,
            int activeOrchestratedRooms,
            int telemetryQueueBacklog,
            int telemetryQueueCapacity,
            long totalMemoryBytes,
            long freeMemoryBytes,
            long maxMemoryBytes,
            long totalTelemetryPersisted,
            long totalFailedRetries,
            long totalTelemetryDropped
    ) {}

    private final NetworkLabService labService;
    private final TelemetryCollector telemetryCollector;
    private final NetworkControllerOrchestrator orchestrator;
    private final NetworkPersistenceService persistenceService;

    public NetworkLabController(NetworkLabService labService,
                                TelemetryCollector telemetryCollector,
                                NetworkControllerOrchestrator orchestrator,
                                NetworkPersistenceService persistenceService) {
        this.labService = labService;
        this.telemetryCollector = telemetryCollector;
        this.orchestrator = orchestrator;
        this.persistenceService = persistenceService;
    }

    @GetMapping("/system-health")
    public ResponseEntity<NetworkSystemHealthResponse> getSystemHealth() {
        Runtime runtime = Runtime.getRuntime();
        NetworkSystemHealthResponse health = new NetworkSystemHealthResponse(
                System.currentTimeMillis(),
                "HEALTHY",
                telemetryCollector.getTrackedRoomCount(),
                orchestrator.getActiveRoomCount(),
                persistenceService.getQueueBacklog(),
                persistenceService.getQueueCapacity(),
                runtime.totalMemory(),
                runtime.freeMemory(),
                runtime.maxMemory(),
                persistenceService.getTotalPersisted(),
                persistenceService.getTotalFailedRetries(),
                persistenceService.getTotalDropped()
        );
        return ResponseEntity.ok(health);
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<LabProfileResponse> getProfile(@PathVariable String roomCode) {
        return ResponseEntity.ok(LabProfileResponse.from(roomCode, labService.getProfile(roomCode)));
    }

    @PostMapping("/{roomCode}")
    public ResponseEntity<LabProfileResponse> setProfile(@PathVariable String roomCode,
                                                           @RequestBody SetLabProfileRequest req) {
        NetworkLabProfile profile;
        if (req.getProfileName() != null && !req.getProfileName().isBlank()
                && !"CUSTOM".equalsIgnoreCase(req.getProfileName())) {
            profile = NetworkProfilePresets.byName(req.getProfileName());
        } else {
            profile = new NetworkLabProfile("CUSTOM",
                    orZero(req.getLatencyMs()), orZero(req.getJitterMs()),
                    orZero(req.getPacketLossPct()), orZero(req.getBandwidthKbps()));
        }
        labService.setProfile(roomCode, profile);
        return ResponseEntity.ok(LabProfileResponse.from(roomCode, profile));
    }

    @DeleteMapping("/{roomCode}")
    public ResponseEntity<LabProfileResponse> clearProfile(@PathVariable String roomCode) {
        labService.clearProfile(roomCode);
        return ResponseEntity.ok(LabProfileResponse.from(roomCode, NetworkLabProfile.off()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadProfile(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    private static double orZero(Double v) {
        return v == null ? 0 : v;
    }
}
