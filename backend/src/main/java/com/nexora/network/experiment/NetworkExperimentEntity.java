package com.nexora.network.experiment;

import jakarta.persistence.*;
import java.time.Instant;

/** Maps to network_experiment (docs/DATABASE_DESIGN.md). A labeling/bookkeeping record —
 *  the actual comparison (ExperimentAnalysisService) works directly off two session IDs,
 *  so linking GameSession.experimentId to a row here is optional metadata, not required
 *  for analysis to work. */
@Entity
@Table(name = "network_experiment")
public class NetworkExperimentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "game_id", nullable = false)
    private String gameId;

    @Column(name = "network_profile")
    private String networkProfile;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "packet_loss_pct")
    private Double packetLossPct;

    @Column(name = "jitter_ms")
    private Integer jitterMs;

    @Column(name = "bandwidth_kbps")
    private Integer bandwidthKbps;

    @Column(name = "num_players")
    private Integer numPlayers;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected NetworkExperimentEntity() { }

    public NetworkExperimentEntity(String name, String gameId, String networkProfile, Integer latencyMs,
                                    Double packetLossPct, Integer jitterMs, Integer bandwidthKbps,
                                    Integer numPlayers, Integer durationSec) {
        this.name = name;
        this.gameId = gameId;
        this.networkProfile = networkProfile;
        this.latencyMs = latencyMs;
        this.packetLossPct = packetLossPct;
        this.jitterMs = jitterMs;
        this.bandwidthKbps = bandwidthKbps;
        this.numPlayers = numPlayers;
        this.durationSec = durationSec;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
