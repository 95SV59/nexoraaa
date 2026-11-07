package com.nexora.library;

import com.nexora.game.Game;
import com.nexora.game.GameMediaRepository;
import com.nexora.game.GameRepository;
import com.nexora.library.dto.LibraryItemDto;
import com.nexora.user.Role;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LibraryServiceTest {

    private LibraryRepository libraryRepository;
    private GameRepository gameRepository;
    private UserRepository userRepository;
    private GameMediaRepository mediaRepository;
    private LibraryService libraryService;

    @BeforeEach
    void setUp() {
        libraryRepository = mock(LibraryRepository.class);
        gameRepository = mock(GameRepository.class);
        userRepository = mock(UserRepository.class);
        mediaRepository = mock(GameMediaRepository.class);

        libraryService = new LibraryService(libraryRepository, gameRepository, userRepository, mediaRepository);
    }

    @Test
    @DisplayName("Adds new game to library and sets initial status")
    void testSetLibraryStatusNew() {
        Long userId = 1L;
        String slug = "cyber-arena";

        User user = new User("player1", "p1@nexora.gg", "hash", new Role("USER"));
        user.setId(userId);

        Game game = new Game();
        game.setId(10L);
        game.setSlug(slug);
        game.setTitle("Cyber Arena");

        when(libraryRepository.findByUserIdAndGameSlug(userId, slug)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(gameRepository.findBySlug(slug)).thenReturn(Optional.of(game));
        when(mediaRepository.findByGameIdAndMediaType(any(), any())).thenReturn(Collections.emptyList());

        Library savedLib = new Library(user, game, LibraryStatus.PLAYING);
        savedLib.setId(100L);
        when(libraryRepository.save(any(Library.class))).thenReturn(savedLib);

        LibraryItemDto dto = libraryService.setLibraryStatus(userId, slug, LibraryStatus.PLAYING);

        assertNotNull(dto);
        assertEquals(slug, dto.getGameSlug());
        assertEquals(LibraryStatus.PLAYING, dto.getStatus());
        verify(libraryRepository, times(1)).save(any(Library.class));
    }

    @Test
    @DisplayName("Toggles favorite status on existing library entry")
    void testToggleFavorite() {
        Long userId = 1L;
        String slug = "grid-racers";

        User user = new User("player1", "p1@nexora.gg", "hash", new Role("USER"));
        user.setId(userId);
        Game game = new Game();
        game.setId(20L);
        game.setSlug(slug);
        game.setTitle("Grid Racers");

        Library lib = new Library(user, game, LibraryStatus.PLAYING);
        lib.setId(101L);
        lib.setFavorite(false);

        when(libraryRepository.findByUserIdAndGameSlug(userId, slug)).thenReturn(Optional.of(lib));
        when(libraryRepository.save(any(Library.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaRepository.findByGameIdAndMediaType(any(), any())).thenReturn(Collections.emptyList());

        LibraryItemDto dto = libraryService.toggleFavorite(userId, slug);

        assertNotNull(dto);
        assertTrue(dto.isFavorite());
        assertTrue(lib.isFavorite());
    }

    @Test
    @DisplayName("Records accumulated playtime and updates last played timestamp")
    void testRecordPlaytime() {
        Long userId = 1L;
        String slug = "cyber-arena";

        User user = new User("player1", "p1@nexora.gg", "hash", new Role("USER"));
        user.setId(userId);
        Game game = new Game();
        game.setId(10L);
        game.setSlug(slug);

        Library lib = new Library(user, game, LibraryStatus.PLAYING);
        lib.setHoursPlayed(2.5);

        when(libraryRepository.findByUserIdAndGameSlug(userId, slug)).thenReturn(Optional.of(lib));

        libraryService.recordPlaytime(userId, slug, 1.5);

        assertEquals(4.0, lib.getHoursPlayed(), 0.01);
        assertNotNull(lib.getLastPlayed());
        verify(libraryRepository, times(1)).save(lib);
    }

    @Test
    @DisplayName("Aggregates library statistics across statuses")
    void testGetLibraryStats() {
        Long userId = 1L;

        when(libraryRepository.countByUserIdAndStatus(userId, LibraryStatus.PLAYING)).thenReturn(2L);
        when(libraryRepository.countByUserIdAndStatus(userId, LibraryStatus.COMPLETED)).thenReturn(5L);
        when(libraryRepository.countByUserIdAndStatus(userId, LibraryStatus.PLAN_TO_PLAY)).thenReturn(3L);
        when(libraryRepository.countByUserIdAndStatus(userId, LibraryStatus.DROPPED)).thenReturn(1L);
        when(libraryRepository.countByUserIdAndIsFavoriteTrue(userId)).thenReturn(4L);

        Map<String, Long> stats = libraryService.getLibraryStats(userId);

        assertEquals(11L, stats.get("TOTAL"));
        assertEquals(4L, stats.get("FAVORITES"));
        assertEquals(2L, stats.get("PLAYING"));
        assertEquals(5L, stats.get("COMPLETED"));
    }
}
