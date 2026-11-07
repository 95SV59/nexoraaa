package com.nexora.multiplayer.matchmaking;

import com.nexora.multiplayer.engine.GameEngineRegistry;
import com.nexora.multiplayer.matchmaking.dto.MatchFoundMessage;
import com.nexora.multiplayer.matchmaking.dto.MatchmakingStatusResponse;
import com.nexora.multiplayer.room.GameRoom;
import com.nexora.multiplayer.room.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);
    private static final int BOT_FILL_WAIT_SECONDS = 10;

    public record QueueTicket(Long userId, String username, String gameSlug, Instant queuedAt) {}
    public record MatchRecord(String roomCode, String gameSlug, Instant matchedAt) {}

    private final Map<Long, QueueTicket> queue = new ConcurrentHashMap<>();
    private final Map<Long, MatchRecord> recentMatches = new ConcurrentHashMap<>();

    private final RoomService roomService;
    private final GameEngineRegistry engineRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public MatchmakingService(RoomService roomService,
                              GameEngineRegistry engineRegistry,
                              SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.engineRegistry = engineRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    public MatchmakingStatusResponse enqueue(Long userId, String username, String rawGameSlug) {
        String slug = (rawGameSlug == null || rawGameSlug.isBlank()) ? "arena" : rawGameSlug.trim().toLowerCase();
        if (!engineRegistry.supports(slug)) {
            slug = "arena";
        }

        // Clean any old match record for this user
        recentMatches.remove(userId);

        QueueTicket ticket = new QueueTicket(userId, username, slug, Instant.now());
        queue.put(userId, ticket);

        int count = getQueueSize(slug);
        return new MatchmakingStatusResponse("QUEUED", slug, null, 0, count);
    }

    public MatchmakingStatusResponse dequeue(Long userId) {
        queue.remove(userId);
        recentMatches.remove(userId);
        return new MatchmakingStatusResponse("IDLE", null, null, 0, 0);
    }

    public MatchmakingStatusResponse getStatus(Long userId) {
        MatchRecord match = recentMatches.get(userId);
        if (match != null) {
            if (Duration.between(match.matchedAt(), Instant.now()).getSeconds() < 60) {
                return new MatchmakingStatusResponse("MATCHED", match.gameSlug(), match.roomCode(), 0, 0);
            } else {
                recentMatches.remove(userId);
            }
        }

        QueueTicket ticket = queue.get(userId);
        if (ticket != null) {
            long elapsed = Duration.between(ticket.queuedAt(), Instant.now()).getSeconds();
            int count = getQueueSize(ticket.gameSlug());
            return new MatchmakingStatusResponse("QUEUED", ticket.gameSlug(), null, elapsed, count);
        }

        return new MatchmakingStatusResponse("IDLE", null, null, 0, 0);
    }

    public int getQueueSize(String gameSlug) {
        return (int) queue.values().stream()
                .filter(t -> t.gameSlug().equalsIgnoreCase(gameSlug))
                .count();
    }

    @Scheduled(fixedDelay = 2000)
    public synchronized void processQueues() {
        if (queue.isEmpty()) return;

        // Group tickets by game slug
        Map<String, List<QueueTicket>> byGame = queue.values().stream()
                .sorted(Comparator.comparing(QueueTicket::queuedAt))
                .collect(Collectors.groupingBy(QueueTicket::gameSlug));

        for (Map.Entry<String, List<QueueTicket>> entry : byGame.entrySet()) {
            String slug = entry.getKey();
            List<QueueTicket> tickets = entry.getValue();
            int maxCap = getMaxPlayersForGame(slug);

            // Case 1: Multiple players waiting — match them together (up to maxCap)
            if (tickets.size() >= 2) {
                int matchCount = Math.min(tickets.size(), maxCap);
                List<QueueTicket> matchedGroup = tickets.subList(0, matchCount);
                createMatchedRoom(slug, maxCap, matchedGroup, false);
            }
            // Case 2: Solo player waiting past threshold — match with bots!
            else if (!tickets.isEmpty()) {
                QueueTicket solo = tickets.get(0);
                long waitTime = Duration.between(solo.queuedAt(), Instant.now()).getSeconds();
                if (waitTime >= BOT_FILL_WAIT_SECONDS) {
                    createMatchedRoom(slug, maxCap, List.of(solo), true);
                }
            }
        }
    }

    private void createMatchedRoom(String gameSlug, int maxPlayers, List<QueueTicket> matched, boolean fillBots) {
        try {
            QueueTicket host = matched.get(0);
            GameRoom room = roomService.createRoom(gameSlug, maxPlayers, "GLOBAL", host.userId(), host.username());
            String roomCode = room.getRoomCode();

            // Join other players
            for (int i = 1; i < matched.size(); i++) {
                QueueTicket p = matched.get(i);
                try {
                    roomService.joinRoom(roomCode, p.userId(), p.username());
                } catch (Exception ex) {
                    log.warn("Failed to auto-join player {} to matched room {}: {}", p.userId(), roomCode, ex.getMessage());
                }
            }

            // Fill bots if requested (or if solo match)
            if (fillBots || matched.size() < 2) {
                try {
                    roomService.fillWithBots(roomCode);
                } catch (Exception ex) {
                    log.warn("Could not fill bots in matched room {}: {}", roomCode, ex.getMessage());
                }
            }

            MatchFoundMessage message = new MatchFoundMessage("MATCH_FOUND", roomCode, gameSlug);
            Instant now = Instant.now();

            // Notify all matched players
            for (QueueTicket p : matched) {
                queue.remove(p.userId());
                recentMatches.put(p.userId(), new MatchRecord(roomCode, gameSlug, now));
                messagingTemplate.convertAndSend("/topic/user/" + p.userId() + "/matchmaking", message);
            }

            log.info("Casual match created for {} players in room {} ({})", matched.size(), roomCode, gameSlug);
        } catch (Exception e) {
            log.error("Failed to create matched room for game {}: {}", gameSlug, e.getMessage(), e);
        }
    }

    private int getMaxPlayersForGame(String gameSlug) {
        return switch (gameSlug.toLowerCase()) {
            case "racers" -> 8;
            case "football" -> 6;
            case "tag" -> 10;
            default -> 8; // arena
        };
    }
}
