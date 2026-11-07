package com.nexora.multiplayer.room;

import com.nexora.multiplayer.engine.bot.BotPlayerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * Room lifecycle: create, join, ready-up, fill-with-bots, leave. Persisted via JPA (game_room /
 * game_player) so rooms survive a server restart during development and can be
 * queried by the admin dashboard (Phase 18). High-frequency in-match state does
 * NOT live here — see multiplayer.engine.GameLoopManager for that.
 *
 * Bot support (Phase 7 extension): fillWithBots() adds server-side AI players for any
 * empty slots, enabling "Start Now" from the lobby. Bots are stored as GamePlayer rows
 * with is_bot=true and null user_id. Their engine playerId ("bot-N") is managed by
 * BotPlayerService, which also runs the per-tick AI logic in GameLoopManager.
 */
@Service
public class RoomService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    private final GameRoomRepository roomRepository;
    private final GamePlayerRepository playerRepository;
    private final BotPlayerService botPlayerService;

    public RoomService(GameRoomRepository roomRepository, GamePlayerRepository playerRepository,
                       BotPlayerService botPlayerService) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.botPlayerService = botPlayerService;
    }

    @Transactional
    public GameRoom createRoom(String gameId, int maxPlayers, String region,
                                Long hostUserId, String hostDisplayName) {
        String code = generateUniqueCode();
        GameRoom room = new GameRoom(code, gameId, hostUserId, maxPlayers, region);
        roomRepository.save(room);
        addPlayer(room, hostUserId, hostDisplayName);
        return room;
    }

    @Transactional
    public GameRoom joinRoom(String roomCode, Long userId, String displayName) {
        GameRoom room = getOpenRoomOrThrow(roomCode);
        // Count only non-bot, non-spectator slots against capacity for human joining
        long humanCount = getActivePlayers(room.getId()).stream().filter(p -> !p.isBot() && !p.isSpectator()).count();
        if (humanCount >= room.getMaxPlayers()) {
            throw RoomException.full(roomCode);
        }
        addPlayer(room, userId, displayName);
        return room;
    }

    @Transactional
    public GameRoom spectateRoom(String roomCode, Long userId, String displayName) {
        GameRoom room = getRoomOrThrow(roomCode);
        boolean alreadyActive = playerRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), userId).isPresent();
        if (!alreadyActive) {
            playerRepository.save(new GamePlayer(room, userId, displayName, true));
        }
        return room;
    }

    @Transactional
    public void setReady(String roomCode, Long userId, boolean ready) {
        GameRoom room = getRoomOrThrow(roomCode);
        GamePlayer player = playerRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), userId)
                .orElseThrow(() -> RoomException.notFound(roomCode));
        player.setReady(ready);
        playerRepository.save(player);
    }

    @Transactional
    public void leaveRoom(String roomCode, Long userId) {
        GameRoom room = getRoomOrThrow(roomCode);
        playerRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), userId)
                .ifPresent(p -> {
                    p.markLeft();
                    playerRepository.save(p);
                    room.setCurrentPlayers(Math.max(0, room.getCurrentPlayers() - 1));
                    roomRepository.save(room);
                });
    }

    @Transactional
    public GamePlayer reconnectPlayer(String roomCode, Long userId, String reconnectToken) {
        GameRoom room = getRoomOrThrow(roomCode);
        GamePlayer player = playerRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found in room: " + roomCode));

        if (player.getReconnectToken() != null && !player.getReconnectToken().equals(reconnectToken)) {
            throw new IllegalArgumentException("Invalid reconnect token.");
        }
        player.markReconnected();
        return playerRepository.save(player);
    }

    @Transactional
    public void markDisconnected(String roomCode, Long userId) {
        GameRoom room = getRoomOrThrow(roomCode);
        playerRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), userId)
                .ifPresent(p -> {
                    p.markDisconnected();
                    playerRepository.save(p);
                });
    }

    /**
     * Fill all remaining slots in the room with server-side AI bots so the match
     * can start immediately ("Start Now" button). At least one real human must already
     * be in the room. Returns the number of bots added.
     *
     * Each bot gets:
     *  - A GamePlayer row (is_bot=true, user_id=null, is_ready=true).
     *  - A registration in BotPlayerService with its stable engine playerId ("bot-N").
     */
    @Transactional
    public int fillWithBots(String roomCode) {
        GameRoom room = getOpenRoomOrThrow(roomCode);
        List<GamePlayer> active = getActivePlayers(room.getId());
        long humanCount = active.stream().filter(p -> !p.isBot()).count();

        if (humanCount < 1) {
            throw new IllegalStateException("At least one human player must be in the room before adding bots.");
        }

        int botsNeeded = room.getMaxPlayers() - active.size();
        int botsAdded = 0;

        for (int i = 0; i < botsNeeded; i++) {
            String displayName = BotPlayerService.botDisplayName(active.size() + i);
            // Register in BotPlayerService FIRST to get the stable botId
            String botId = botPlayerService.addBot(roomCode, displayName);
            // Create the GamePlayer row for lobby display and roster queries
            addBotPlayer(room, displayName);
            botsAdded++;
        }

        return botsAdded;
    }

    @Transactional
    public void markInProgress(String roomCode) {
        GameRoom room = getRoomOrThrow(roomCode);
        room.setStatus(RoomStatus.IN_PROGRESS);
        roomRepository.save(room);
    }

    @Transactional
    public void markFinished(String roomCode) {
        GameRoom room = getRoomOrThrow(roomCode);
        room.setStatus(RoomStatus.FINISHED);
        roomRepository.save(room);
    }

    public GameRoom getRoomOrThrow(String roomCode) {
        return roomRepository.findByRoomCode(roomCode).orElseThrow(() -> RoomException.notFound(roomCode));
    }

    public List<GamePlayer> getActivePlayers(Long roomId) {
        return playerRepository.findByRoomIdAndLeftAtIsNull(roomId);
    }

    @Transactional
    public GameRoom resetForRematch(String roomCode) {
        GameRoom room = getRoomOrThrow(roomCode);
        room.setStatus(RoomStatus.LOBBY);
        roomRepository.save(room);
        List<GamePlayer> players = getActivePlayers(room.getId());
        for (GamePlayer p : players) {
            if (!p.isBot() && !p.isSpectator()) {
                p.setReady(false);
                playerRepository.save(p);
            }
        }
        return room;
    }

    /**
     * All players are ready if: there is at least one real human player, all combatants (real + bots)
     * are marked ready, and there are at least 2 combatants total (spectators excluded from count).
     */
    public boolean allReady(Long roomId) {
        List<GamePlayer> players = getActivePlayers(roomId);
        List<GamePlayer> combatants = players.stream().filter(p -> !p.isSpectator()).toList();
        if (combatants.size() < 2) return false;
        boolean anyHuman = combatants.stream().anyMatch(p -> !p.isBot());
        return anyHuman && combatants.stream().allMatch(GamePlayer::isReady);
    }

    private GameRoom getOpenRoomOrThrow(String roomCode) {
        GameRoom room = getRoomOrThrow(roomCode);
        if (room.getStatus() != RoomStatus.LOBBY) {
            throw RoomException.notInLobby(roomCode);
        }
        return room;
    }

    private void addPlayer(GameRoom room, Long userId, String displayName) {
        playerRepository.save(new GamePlayer(room, userId, displayName));
        room.setCurrentPlayers(room.getCurrentPlayers() + 1);
        roomRepository.save(room);
    }

    private void addBotPlayer(GameRoom room, String displayName) {
        playerRepository.save(new GamePlayer(room, displayName, true));
        room.setCurrentPlayers(room.getCurrentPlayers() + 1);
        roomRepository.save(room);
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (roomRepository.existsByRoomCode(code));
        return code;
    }
}
