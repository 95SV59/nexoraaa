package com.nexora.multiplayer.engine.bot;

import com.nexora.multiplayer.engine.PlayerInput;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages bot registrations per room and dispatches BotBrain.computeInput() each tick.
 *
 * Bot player IDs follow the pattern "bot-{n}" (e.g. "bot-1", "bot-2").
 * These are NEVER real user IDs, so TelemetryCollector ignores them for RTT/jitter.
 * BotBrains are game-specific; the registry maps gameId -> BotBrain factory.
 *
 * Thread-safety: getBotIds/computeBotInputs are called from GameLoopManager's
 * per-room scheduled thread. addBot/clearRoom are called from request threads.
 * ConcurrentHashMap + AtomicInteger cover all races here.
 */
@Service
public class BotPlayerService {

    /** Map from roomCode -> ordered list of BotEntry (botId + displayName). */
    private final Map<String, List<BotEntry>> roomBots = new ConcurrentHashMap<>();

    /** Map from gameId -> BotBrain instance (stateless per-game strategy). */
    private static final Map<String, BotBrain> BRAINS = Map.of(
            "arena",    new ArenaBotBrain(),
            "racers",   new RacersBotBrain(),
            "football", new FootballBotBrain(),
            "tag",      new TagBotBrain()
    );

    private final AtomicInteger botSequence = new AtomicInteger(0);

    public record BotEntry(String botId, String displayName) {}

    /**
     * Register a new bot for this room. Returns the bot's stable playerId ("bot-N").
     * The caller (RoomService) creates the corresponding GamePlayer row with is_bot=true.
     *
     * @param roomCode    The room this bot belongs to.
     * @param displayName Human-readable bot name (e.g. "BOT-Nexus").
     * @return            The stable bot playerId used in the game engine ("bot-N").
     */
    public String addBot(String roomCode, String displayName) {
        String botId = "bot-" + botSequence.incrementAndGet();
        roomBots.computeIfAbsent(roomCode, k -> new ArrayList<>()).add(new BotEntry(botId, displayName));
        return botId;
    }

    /** All bot playerIds currently registered for this room (in insertion order). */
    public List<String> getBotIds(String roomCode) {
        List<BotEntry> entries = roomBots.getOrDefault(roomCode, Collections.emptyList());
        return entries.stream().map(BotEntry::botId).toList();
    }

    /** Look up the bot playerId registered with the given display name for this room. */
    public String getBotIdByDisplayName(String roomCode, String displayName) {
        List<BotEntry> entries = roomBots.getOrDefault(roomCode, Collections.emptyList());
        return entries.stream()
                .filter(e -> e.displayName().equals(displayName))
                .map(BotEntry::botId)
                .findFirst()
                .orElse(null);
    }

    /** True if this playerId is a bot (i.e. was registered via addBot). */
    public boolean isBot(String playerId) {
        return playerId != null && playerId.startsWith("bot-");
    }

    /**
     * Compute and return all bots' inputs for this tick.
     * Called from GameLoopManager.tick() after real inputs have been submitted.
     *
     * @param roomCode  The room being ticked.
     * @param gameId    Used to select the correct BotBrain implementation.
     * @param gameState The current state snapshot (game.getStateSnapshot()).
     * @return          Map from botPlayerId -> PlayerInput for this tick.
     */
    public Map<String, PlayerInput> computeBotInputs(String roomCode, String gameId, Object gameState) {
        List<BotEntry> bots = roomBots.getOrDefault(roomCode, Collections.emptyList());
        if (bots.isEmpty()) return Collections.emptyMap();

        BotBrain brain = BRAINS.get(gameId);
        if (brain == null) return Collections.emptyMap();

        Map<String, PlayerInput> inputs = new HashMap<>(bots.size());
        for (BotEntry bot : bots) {
            inputs.put(bot.botId(), brain.computeInput(bot.botId(), gameState));
        }
        return inputs;
    }

    /** Clean up when the match ends or room is abandoned. */
    public void clearRoom(String roomCode) {
        roomBots.remove(roomCode);
    }

    /** Generate a display name for a bot (1-indexed). */
    public static String botDisplayName(int index) {
        String[] names = {"Nexus", "Cipher", "Vega", "Orion", "Lyra", "Zeta", "Atlas", "Nova", "Echo", "Apex"};
        return "BOT-" + names[index % names.length];
    }
}
