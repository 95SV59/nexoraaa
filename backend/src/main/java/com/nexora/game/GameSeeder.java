package com.nexora.game;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Seeds the catalogue with Tier 1 multiplayer playable titles (Arena, Racers, Football, Tag)
 * plus upcoming preview titles.
 */
@Component
public class GameSeeder implements ApplicationRunner {

    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;
    private final GameRepository gameRepository;
    private final GameMediaRepository mediaRepository;

    public GameSeeder(GenreRepository genreRepository, PlatformRepository platformRepository,
                       GameRepository gameRepository, GameMediaRepository mediaRepository) {
        this.genreRepository = genreRepository;
        this.platformRepository = platformRepository;
        this.gameRepository = gameRepository;
        this.mediaRepository = mediaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (gameRepository.count() > 0) return; // already seeded

        Genre action = genre("Action");
        Genre strategy = genre("Strategy");
        Genre party = genre("Party");
        Genre sports = genre("Sports");
        Genre racing = genre("Racing");
        Platform web = platform("Web Browser");

        seed("arena", "NEXORA Arena",
                "A 2D top-down multiplayer shooter for 2 to 8 players — the platform's flagship tactical "
                        + "combat arena with high-frequency projectile synchronization.",
                action, web, true, 2, 8, LocalDate.of(2026, 7, 1),
                "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&auto=format&fit=crop&q=80");

        seed("racers", "NEXORA Racers",
                "A high-octane multiplayer kart racer with steering physics, lap checkpoints, "
                        + "dead-reckoning extrapolation and position prediction.",
                racing, web, true, 1, 8, LocalDate.of(2026, 8, 15),
                "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800&auto=format&fit=crop&q=80");

        seed("football", "NEXORA Football",
                "Arcade multiplayer football emphasizing teamwork, precision passing, tackling, "
                        + "and critical ball position consistency control.",
                sports, web, true, 1, 6, LocalDate.of(2026, 9, 1),
                "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=800&auto=format&fit=crop&q=80");

        seed("tag", "NEXORA Tag",
                "Fast-paced multiplayer tag arena designed to benchmark rapid state transitions, "
                        + "tag latency, and jitter compensation.",
                action, web, true, 1, 10, LocalDate.of(2026, 9, 20),
                "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&auto=format&fit=crop&q=80");

        seed("chess-arena", "NEXORA Chess Arena",
                "Turn-based multiplayer chess. Deliberately low-frequency state, to test the controller "
                        + "and consistency engine against a game that barely needs real-time sync at all.",
                strategy, web, true, 2, 2, null,
                "https://images.unsplash.com/photo-1529699211952-734e80c4d42b?w=800&auto=format&fit=crop&q=80");

        seed("party-arena", "NEXORA Party Arena",
                "A rotating set of minigames (including a dodge-arena mode) for larger groups.",
                party, web, true, 4, 16, null,
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80");
    }

    private void seed(String slug, String title, String description, Genre genre, Platform platform,
                       boolean multiplayer, int minPlayers, int maxPlayers, LocalDate releaseDate, String imageUrl) {
        Game game = new Game(slug, title, description, "NEXORA Team", "NEXORA Team",
                multiplayer, minPlayers, maxPlayers, genre, releaseDate);
        game.addPlatform(platform);
        gameRepository.save(game);
        mediaRepository.save(new GameMedia(game, GameMedia.MediaType.SCREENSHOT, imageUrl));
    }

    private Genre genre(String name) {
        return genreRepository.findByName(name).orElseGet(() -> genreRepository.save(new Genre(name)));
    }

    private Platform platform(String name) {
        return platformRepository.findByName(name).orElseGet(() -> platformRepository.save(new Platform(name)));
    }
}
