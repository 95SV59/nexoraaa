package com.nexora.multiplayer.engine;

import com.nexora.multiplayer.engine.arena.NexoraArenaGame;
import com.nexora.multiplayer.engine.football.NexoraFootballGame;
import com.nexora.multiplayer.engine.racers.NexoraRacersGame;
import com.nexora.multiplayer.engine.tag.NexoraTagGame;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Maps a Game.slug (game_id, e.g. "arena", "racers", "football", "tag") to a fresh
 * MultiplayerGame instance for a new match.
 */
@Component
public class GameEngineRegistry {

    private final Map<String, Supplier<MultiplayerGame>> factories = Map.of(
            "arena", NexoraArenaGame::new,
            "racers", NexoraRacersGame::new,
            "football", NexoraFootballGame::new,
            "tag", NexoraTagGame::new
    );

    public MultiplayerGame create(String gameId) {
        Supplier<MultiplayerGame> factory = factories.get(gameId);
        if (factory == null) {
            throw new IllegalArgumentException("No multiplayer engine registered for gameId=" + gameId);
        }
        return factory.get();
    }

    public boolean supports(String gameId) {
        return factories.containsKey(gameId);
    }
}
