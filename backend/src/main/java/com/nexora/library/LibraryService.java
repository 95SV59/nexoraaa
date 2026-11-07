package com.nexora.library;

import com.nexora.exception.ResourceNotFoundException;
import com.nexora.game.Game;
import com.nexora.game.GameMedia;
import com.nexora.game.GameMediaRepository;
import com.nexora.game.GameRepository;
import com.nexora.library.dto.LibraryItemDto;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LibraryService {

    private final LibraryRepository libraryRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final GameMediaRepository mediaRepository;

    public LibraryService(LibraryRepository libraryRepository, GameRepository gameRepository,
                          UserRepository userRepository, GameMediaRepository mediaRepository) {
        this.libraryRepository = libraryRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.mediaRepository = mediaRepository;
    }

    @Transactional(readOnly = true)
    public List<LibraryItemDto> getUserLibrary(Long userId, LibraryStatus status, Boolean favoritesOnly) {
        List<Library> items;
        if (Boolean.TRUE.equals(favoritesOnly)) {
            items = libraryRepository.findByUserIdAndIsFavoriteTrueOrderByUpdatedAtDesc(userId);
        } else if (status != null) {
            items = libraryRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, status);
        } else {
            items = libraryRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        }
        return items.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public LibraryItemDto setLibraryStatus(Long userId, String slug, LibraryStatus status) {
        Optional<Library> existing = libraryRepository.findByUserIdAndGameSlug(userId, slug);
        if (existing.isPresent()) {
            Library lib = existing.get();
            lib.setStatus(status);
            return toDto(libraryRepository.save(lib));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Game game = gameRepository.findBySlug(slug)
                .or(() -> {
                    try {
                        return gameRepository.findById(Long.parseLong(slug));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                })
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + slug));

        Library lib = new Library(user, game, status);
        return toDto(libraryRepository.save(lib));
    }

    @Transactional
    public LibraryItemDto toggleFavorite(Long userId, String slug) {
        Library lib = libraryRepository.findByUserIdAndGameSlug(userId, slug)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    Game game = gameRepository.findBySlug(slug)
                            .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + slug));
                    return libraryRepository.save(new Library(user, game, LibraryStatus.PLAYING));
                });

        lib.setFavorite(!lib.isFavorite());
        return toDto(libraryRepository.save(lib));
    }

    @Transactional
    public LibraryItemDto updateNotes(Long userId, String slug, String notes) {
        Library lib = libraryRepository.findByUserIdAndGameSlug(userId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Game not in library: " + slug));
        lib.setUserNotes(notes);
        return toDto(libraryRepository.save(lib));
    }

    @Transactional
    public void recordPlaytime(Long userId, String gameSlug, double hours) {
        if (userId == null || gameSlug == null) return;
        Library lib = libraryRepository.findByUserIdAndGameSlug(userId, gameSlug.toLowerCase())
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElse(null);
                    Game game = gameRepository.findBySlug(gameSlug.toLowerCase()).orElse(null);
                    if (user == null || game == null) return null;
                    return new Library(user, game, LibraryStatus.PLAYING);
                });

        if (lib != null) {
            lib.addPlaytime(hours);
            lib.setLastPlayed(Instant.now());
            libraryRepository.save(lib);
        }
    }

    @Transactional
    public void removeFromLibrary(Long userId, String slug) {
        libraryRepository.deleteByUserIdAndGameSlug(userId, slug);
    }

    @Transactional(readOnly = true)
    public LibraryStatus getGameStatus(Long userId, String slug) {
        return libraryRepository.findByUserIdAndGameSlug(userId, slug)
                .map(Library::getStatus)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getLibraryStats(Long userId) {
        Map<String, Long> stats = new HashMap<>();
        long total = 0;
        for (LibraryStatus s : LibraryStatus.values()) {
            long count = libraryRepository.countByUserIdAndStatus(userId, s);
            stats.put(s.name(), count);
            total += count;
        }
        stats.put("FAVORITES", libraryRepository.countByUserIdAndIsFavoriteTrue(userId));
        stats.put("TOTAL", total);
        return stats;
    }

    private LibraryItemDto toDto(Library lib) {
        Game g = lib.getGame();
        String thumbnail = mediaRepository.findByGameIdAndMediaType(g.getId(), GameMedia.MediaType.SCREENSHOT)
                .stream().findFirst().map(GameMedia::getUrl)
                .orElse("https://placehold.co/640x360?text=" + g.getSlug());
        String genreName = g.getGenre() != null ? g.getGenre().getName() : "Uncategorized";

        return new LibraryItemDto(
                lib.getId(),
                g.getId(),
                g.getSlug(),
                g.getTitle(),
                g.getDescription(),
                genreName,
                thumbnail,
                g.isMultiplayer(),
                lib.getStatus(),
                lib.getHoursPlayed(),
                lib.getLastPlayed(),
                lib.isFavorite(),
                lib.getUserNotes(),
                lib.getUpdatedAt()
        );
    }
}
