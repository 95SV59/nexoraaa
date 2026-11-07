package com.nexora.game.dto;

/** GET /api/genres and GET /api/platforms — just enough to populate filter dropdowns. */
public class LookupDto {
    public Long id;
    public String name;
    public LookupDto(Long id, String name) { this.id = id; this.name = name; }
}
