package com.nexora.recommendation;

import com.nexora.game.dto.GameSummaryDto;

public class RecommendationDto {
    private GameSummaryDto game;
    private String ruleName;
    private String reason;

    public RecommendationDto() { }

    public RecommendationDto(GameSummaryDto game, String ruleName, String reason) {
        this.game = game;
        this.ruleName = ruleName;
        this.reason = reason;
    }

    public GameSummaryDto getGame() { return game; }
    public String getRuleName() { return ruleName; }
    public String getReason() { return reason; }
}
