package com.nexora.game.dto;

import com.nexora.game.Game;
import com.nexora.game.GameMedia;
import com.nexora.game.Platform;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** GET /api/games/{slug}. */
public class GameDetailDto {
    public Long id;
    public String slug;
    public String title;
    public String description;
    public String developer;
    public String publisher;
    public String genre;
    public List<String> platforms;
    public boolean multiplayer;
    public Integer minPlayers;
    public Integer maxPlayers;
    public LocalDate releaseDate;
    public List<MediaDto> media;

    public static class MediaDto {
        public String type;
        public String url;
        public MediaDto(String type, String url) { this.type = type; this.url = url; }
    }

    public static GameDetailDto from(Game g, List<GameMedia> media) {
        GameDetailDto dto = new GameDetailDto();
        dto.id = g.getId();
        dto.slug = g.getSlug();
        dto.title = g.getTitle();
        dto.description = g.getDescription();
        dto.developer = g.getDeveloper();
        dto.publisher = g.getPublisher();
        dto.genre = g.getGenre() != null ? g.getGenre().getName() : null;
        dto.platforms = g.getPlatforms().stream().map(Platform::getName).collect(Collectors.toList());
        dto.multiplayer = g.isMultiplayer();
        dto.minPlayers = g.getMinPlayers();
        dto.maxPlayers = g.getMaxPlayers();
        dto.releaseDate = g.getReleaseDate();
        dto.media = media.stream()
                .map(m -> new MediaDto(m.getMediaType().name(), m.getUrl()))
                .collect(Collectors.toList());
        return dto;
    }
}
