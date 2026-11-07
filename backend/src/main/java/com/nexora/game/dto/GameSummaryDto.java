package com.nexora.game.dto;

import com.nexora.game.Game;
import com.nexora.game.GameMedia;

import java.time.LocalDate;
import java.util.List;

/** One row of GET /api/games. */
public class GameSummaryDto {
    public Long id;
    public String slug;
    public String title;
    public String genre; // null if uncategorized
    public boolean multiplayer;
    public Integer minPlayers;
    public Integer maxPlayers;
    public LocalDate releaseDate;
    public String thumbnailUrl; // first SCREENSHOT, if any

    public static GameSummaryDto from(Game g, List<GameMedia> media) {
        GameSummaryDto dto = new GameSummaryDto();
        dto.id = g.getId();
        dto.slug = g.getSlug();
        dto.title = g.getTitle();
        dto.genre = g.getGenre() != null ? g.getGenre().getName() : null;
        dto.multiplayer = g.isMultiplayer();
        dto.minPlayers = g.getMinPlayers();
        dto.maxPlayers = g.getMaxPlayers();
        dto.releaseDate = g.getReleaseDate();
        dto.thumbnailUrl = media.stream()
                .filter(m -> m.getMediaType() == GameMedia.MediaType.SCREENSHOT)
                .map(GameMedia::getUrl)
                .findFirst().orElse(null);
        return dto;
    }
}
