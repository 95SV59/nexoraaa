package com.nexora.game;

import com.nexora.game.dto.GameDetailDto;
import com.nexora.game.dto.GameSummaryDto;
import com.nexora.game.dto.LookupDto;
import com.nexora.game.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Phase 6 — Catalogue. Requires authentication like everything else under /api (see
 * SecurityConfig) — there's no public/guest browsing yet, which is a reasonable scope cut
 * for now rather than an oversight: this project has no guest session concept at all.
 */
@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;
    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;

    public GameController(GameService gameService, GenreRepository genreRepository,
                           PlatformRepository platformRepository) {
        this.gameService = gameService;
        this.genreRepository = genreRepository;
        this.platformRepository = platformRepository;
    }

    @GetMapping("/games")
    public ResponseEntity<PageResponse<GameSummaryDto>> listGames(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Long platformId,
            @RequestParam(defaultValue = "false") boolean multiplayerOnly,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(gameService.search(search, genreId, platformId, multiplayerOnly,
                sortBy, direction, page, size));
    }

    @GetMapping("/games/{slug}")
    public ResponseEntity<GameDetailDto> getGame(@PathVariable String slug) {
        return ResponseEntity.ok(gameService.getBySlug(slug));
    }

    @GetMapping("/genres")
    public ResponseEntity<List<LookupDto>> listGenres() {
        return ResponseEntity.ok(genreRepository.findAll().stream()
                .map(g -> new LookupDto(g.getId(), g.getName())).toList());
    }

    @GetMapping("/platforms")
    public ResponseEntity<List<LookupDto>> listPlatforms() {
        return ResponseEntity.ok(platformRepository.findAll().stream()
                .map(p -> new LookupDto(p.getId(), p.getName())).toList());
    }
}
