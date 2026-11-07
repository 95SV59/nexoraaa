package com.nexora.multiplayer.session;

import jakarta.persistence.*;

/** Final, server-computed outcome of a GameSession. Maps to match_result. */
@Entity
@Table(name = "match_result")
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private GameSession session;

    @Column(name = "winner_user_id")
    private Long winnerUserId;

    @Column(name = "duration_sec")
    private Integer durationSec;

    /** Per-player scores/stats, engine-defined shape — stored as JSON text. */
    @Lob
    @Column(name = "result_summary")
    private String resultSummaryJson;

    protected MatchResult() { }

    public MatchResult(GameSession session, Long winnerUserId, Integer durationSec, String resultSummaryJson) {
        this.session = session;
        this.winnerUserId = winnerUserId;
        this.durationSec = durationSec;
        this.resultSummaryJson = resultSummaryJson;
    }

    public Long getId() { return id; }
    public GameSession getSession() { return session; }
    public Long getWinnerUserId() { return winnerUserId; }
    public Integer getDurationSec() { return durationSec; }
    public String getResultSummaryJson() { return resultSummaryJson; }
}
