package com.nexora.network.lab;

import com.nexora.network.NetworkControllerOrchestrator;
import com.nexora.network.persistence.NetworkPersistenceService;
import com.nexora.network.telemetry.TelemetryCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PWNC Operational Health & Metrics REST Controller Test")
class NetworkLabControllerTest {

    private NetworkLabService labService;
    private TelemetryCollector telemetryCollector;
    private NetworkControllerOrchestrator orchestrator;
    private NetworkPersistenceService persistenceService;
    private NetworkLabController controller;

    @BeforeEach
    void setUp() {
        labService = mock(NetworkLabService.class);
        telemetryCollector = mock(TelemetryCollector.class);
        orchestrator = mock(NetworkControllerOrchestrator.class);
        persistenceService = mock(NetworkPersistenceService.class);

        controller = new NetworkLabController(labService, telemetryCollector, orchestrator, persistenceService);
    }

    @Test
    @DisplayName("Returns accurate system health, queue backlogs, and memory metrics")
    void testGetSystemHealthReturnsAccurateMetrics() {
        when(telemetryCollector.getTrackedRoomCount()).thenReturn(3);
        when(orchestrator.getActiveRoomCount()).thenReturn(2);
        when(persistenceService.getQueueBacklog()).thenReturn(14);
        when(persistenceService.getQueueCapacity()).thenReturn(10000);
        when(persistenceService.getTotalPersisted()).thenReturn(1500L);
        when(persistenceService.getTotalFailedRetries()).thenReturn(2L);
        when(persistenceService.getTotalDropped()).thenReturn(0L);

        ResponseEntity<NetworkLabController.NetworkSystemHealthResponse> response = controller.getSystemHealth();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        NetworkLabController.NetworkSystemHealthResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("HEALTHY", body.status());
        assertEquals(3, body.activeTrackedRooms());
        assertEquals(2, body.activeOrchestratedRooms());
        assertEquals(14, body.telemetryQueueBacklog());
        assertEquals(10000, body.telemetryQueueCapacity());
        assertEquals(1500L, body.totalTelemetryPersisted());
        assertEquals(2L, body.totalFailedRetries());
        assertEquals(0L, body.totalTelemetryDropped());
        assertTrue(body.totalMemoryBytes() > 0);
        assertTrue(body.freeMemoryBytes() > 0);
        assertTrue(body.timestamp() > 0);
    }
}
