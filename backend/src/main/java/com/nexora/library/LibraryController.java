package com.nexora.library;

import com.nexora.library.dto.LibraryItemDto;
import com.nexora.library.dto.UpdateLibraryRequest;
import com.nexora.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping
    public List<LibraryItemDto> getMyLibrary(@RequestParam(required = false) LibraryStatus status,
                                             @RequestParam(required = false) Boolean favoritesOnly,
                                             @AuthenticationPrincipal AuthPrincipal principal) {
        return libraryService.getUserLibrary(principal.userId(), status, favoritesOnly);
    }

    public record AddToLibraryRequest(String gameSlug, String gameId, LibraryStatus status) {}

    @PostMapping
    public ResponseEntity<LibraryItemDto> addToLibrary(@RequestBody AddToLibraryRequest request,
                                                       @AuthenticationPrincipal AuthPrincipal principal) {
        String target = request.gameSlug() != null ? request.gameSlug() : request.gameId();
        LibraryStatus st = request.status() != null ? request.status() : LibraryStatus.PLAYING;
        LibraryItemDto item = libraryService.setLibraryStatus(principal.userId(), target, st);
        return ResponseEntity.ok(item);
    }

    @PostMapping("/{slug}")
    public ResponseEntity<LibraryItemDto> setStatus(@PathVariable String slug,
                                                    @Valid @RequestBody UpdateLibraryRequest request,
                                                    @AuthenticationPrincipal AuthPrincipal principal) {
        LibraryItemDto item = libraryService.setLibraryStatus(principal.userId(), slug, request.getStatus());
        return ResponseEntity.ok(item);
    }

    @PostMapping("/{slug}/favorite")
    public ResponseEntity<LibraryItemDto> toggleFavorite(@PathVariable String slug,
                                                         @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(libraryService.toggleFavorite(principal.userId(), slug));
    }

    public record UpdateNotesRequest(String notes) {}

    @PostMapping("/{slug}/notes")
    public ResponseEntity<LibraryItemDto> updateNotes(@PathVariable String slug,
                                                      @RequestBody UpdateNotesRequest request,
                                                      @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(libraryService.updateNotes(principal.userId(), slug, request.notes()));
    }

    @DeleteMapping("/{slug}")
    public Map<String, Object> removeFromLibrary(@PathVariable String slug,
                                                 @AuthenticationPrincipal AuthPrincipal principal) {
        libraryService.removeFromLibrary(principal.userId(), slug);
        return Map.of("removed", true, "slug", slug);
    }

    @GetMapping("/{slug}/status")
    public Map<String, Object> getGameStatus(@PathVariable String slug,
                                             @AuthenticationPrincipal AuthPrincipal principal) {
        LibraryStatus status = libraryService.getGameStatus(principal.userId(), slug);
        return Map.of("status", status != null ? status.name() : "", "inLibrary", status != null);
    }

    @GetMapping("/stats")
    public Map<String, Long> getStats(@AuthenticationPrincipal AuthPrincipal principal) {
        return libraryService.getLibraryStats(principal.userId());
    }
}
