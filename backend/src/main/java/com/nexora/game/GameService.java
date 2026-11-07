package com.nexora.game;

import com.nexora.game.dto.GameDetailDto;
import com.nexora.game.dto.GameSummaryDto;
import com.nexora.game.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class GameService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "releaseDate", "createdAt");

    private final GameRepository gameRepository;
    private final GameMediaRepository mediaRepository;

    public GameService(GameRepository gameRepository, GameMediaRepository mediaRepository) {
        this.gameRepository = gameRepository;
        this.mediaRepository = mediaRepository;
    }

    public PageResponse<GameSummaryDto> search(String search, Long genreId, Long platformId,
                                                boolean multiplayerOnly, String sortBy, String direction,
                                                int page, int size) {
        String sortField = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "title";
        Sort.Direction sortDir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(sortDir, sortField));

        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        Page<Game> results = gameRepository.search(normalizedSearch, genreId, platformId, multiplayerOnly, pageable);

        return PageResponse.of(results, g -> GameSummaryDto.from(g, mediaRepository.findByGameId(g.getId())));
    }

    public GameDetailDto getBySlug(String slug) {
        Game game = gameRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No game: " + slug));
        return GameDetailDto.from(game, mediaRepository.findByGameId(game.getId()));
    }
}
