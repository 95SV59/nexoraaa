package com.nexora.multiplayer.engine;

import com.nexora.library.LibraryService;
import com.nexora.multiplayer.engine.bot.BotPlayerService;
import com.nexora.multiplayer.room.GamePlayer;
import com.nexora.multiplayer.room.GameRoom;
import com.nexora.multiplayer.room.RoomService;
import com.nexora.multiplayer.session.GameSession;
import com.nexora.multiplayer.session.SessionService;
import com.nexora.multiplayer.transport.MultiplayerTransportGateway;
import com.nexora.multiplayer.transport.StompWebSocketTransportGateway;
import com.nexora.multiplayer.transport.TransportChannel;
import com.nexora.multiplayer.websocket.dto.MatchLifecycleMessage;
import com.nexora.network.NetworkControllerOrchestrator;
import com.nexora.network.decision.BroadcastRateRegistry;
import com.nexora.network.experiment.ControllerModeRegistry;
import com.nexora.network.lab.NetworkLabService;
import com.nexora.network.telemetry.TelemetryCollector;
import com.nexora.progression.LeaderboardService;
import com.nexora.progression.ProgressionService;
import jakarta.annotation.PreDestroy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Runs one fixed-rate tick loop per active room and owns the transition from lobby
 * to in-match to finished. This is the piece Phase 7 (\"match lifecycle\") and Phase 8
 * (\"implement NEXORA Arena\") meet: it is written against the MultiplayerGame interface
 * only, so it does not change when Phase 9 adds more games. It also starts and stops
 * the Phase 10-15 network telemetry/controller pipeline alongside each match, and reads
 * BroadcastRateRegistry every tick to apply whatever throttle the controller has decided
 * on — see BroadcastRateRegistry's javadoc for why that indirection exists.
 *
 * High-frequency work (updateState/broadcastState) never touches the database; only
 * startMatch/finishMatch do, via SessionService.
 *
 * Bot integration: after real-player inputs are processed each tick, BotPlayerService
 * computes and submits each bot's AI input. Bots are excluded from telemetry tracking
 * to preserve patent-pipeline data integrity (only real player RTT/jitter matters).
 */
@Component
public class GameLoopManager {

    private record ActiveMatch(MultiplayerGame game, GameSession session, ScheduledFuture<?> tickTask,
                                AtomicLong tickCounter, AtomicLong broadcastCounter,
                                String gameId, List<EnginePlayer> players) { }

    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
    private final Map<String, ActiveMatch> activeMatches = new ConcurrentHashMap<>();

    private final RoomService roomService;
    private final SessionService sessionService;
    private final GameEngineRegistry engineRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final TelemetryCollector telemetryCollector;
    private final NetworkControllerOrchestrator networkControllerOrchestrator;
    private final NetworkLabService networkLabService;
    private final BroadcastRateRegistry broadcastRateRegistry;
    private final ControllerModeRegistry controllerModeRegistry;
    private final BotPlayerService botPlayerService;
    private final ProgressionService progressionService;
    private final LeaderboardService leaderboardService;
    private final LibraryService libraryService;
    private final MultiplayerTransportGateway transportGateway;
    
    @org.springframework.beans.factory.annotation.Autowired
    public GameLoopManager(RoomService roomService, SessionService sessionService,
                            GameEngineRegistry engineRegistry, SimpMessagingTemplate messagingTemplate,
                            TelemetryCollector telemetryCollector,
                            NetworkControllerOrchestrator networkControllerOrchestrator,
                            NetworkLabService networkLabService,
                            BroadcastRateRegistry broadcastRateRegistry,
                            ControllerModeRegistry controllerModeRegistry,
                            BotPlayerService botPlayerService,
                            ProgressionService progressionService,
                            LeaderboardService leaderboardService,
                            LibraryService libraryService,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) MultiplayerTransportGateway transportGateway) {
        this.roomService = roomService;
        this.sessionService = sessionService;
        this.engineRegistry = engineRegistry;
        this.messagingTemplate = messagingTemplate;
        this.telemetryCollector = telemetryCollector;
        this.networkControllerOrchestrator = networkControllerOrchestrator;
        this.networkLabService = networkLabService;
        this.broadcastRateRegistry = broadcastRateRegistry;
        this.controllerModeRegistry = controllerModeRegistry;
        this.botPlayerService = botPlayerService;
        this.progressionService = progressionService;
        this.leaderboardService = leaderboardService;
        this.libraryService = libraryService;
        this.transportGateway = transportGateway != null ? transportGateway
                : new StompWebSocketTransportGateway(messagingTemplate);
    }

    public GameLoopManager(RoomService roomService, SessionService sessionService,
                            GameEngineRegistry engineRegistry, SimpMessagingTemplate messagingTemplate,
                            TelemetryCollector telemetryCollector,
                            NetworkControllerOrchestrator networkControllerOrchestrator,
                            NetworkLabService networkLabService,
                            BroadcastRateRegistry broadcastRateRegistry,
                            ControllerModeRegistry controllerModeRegistry,
                            BotPlayerService botPlayerService,
                            ProgressionService progressionService,
                            LeaderboardService leaderboardService,
                            LibraryService libraryService) {
        this(roomService, sessionService, engineRegistry, messagingTemplate, telemetryCollector,
                networkControllerOrchestrator, networkLabService, broadcastRateRegistry,
                controllerModeRegistry, botPlayerService, progressionService, leaderboardService, libraryService, null);
    }

    public boolean isRunning(String roomCode) {
        return activeMatches.containsKey(roomCode);
    }

    public synchronized void startMatch(String roomCode) {
        if (activeMatches.containsKey(roomCode)) return; // already running

        GameRoom room = roomService.getRoomOrThrow(roomCode);
        List<GamePlayer> roster = roomService.getActivePlayers(room.getId());
        if (roster.size() < 2) return; // nothing to play

        MultiplayerGame game = engineRegistry.create(room.getGameId());

        // Separate real players from bots; bots use their registered botId (e.g. "bot-3")
        // while real players use their userId string as the engine playerId. Spectators are excluded.
        List<EnginePlayer> enginePlayers = roster.stream()
                .filter(p -> !p.isSpectator())
                .map(p -> {
                    if (p.isBot()) {
                        // Resolve the bot's engine playerId from the BotPlayerService registry
                        // using the display name as key (set at bot creation time).
                        String botId = botPlayerService.getBotIdByDisplayName(roomCode, p.getDisplayName());
                        if (botId == null) botId = "bot-" + p.getId(); // fallback
                        return new EnginePlayer(botId, null, p.getDisplayName());
                    }
                    return new EnginePlayer(String.valueOf(p.getUserId()), p.getUserId(), p.getDisplayName());
                })
                .collect(Collectors.toList());

        game.initialize(roomCode, enginePlayers);

        roomService.markInProgress(roomCode);
        GameSession.ControllerMode mode = controllerModeRegistry.getMode(roomCode);
        GameSession session = sessionService.startSession(room.getId(), mode);

        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/lifecycle",
                new MatchLifecycleMessage(roomCode, "GAME_START", null, session.getId()));

        // Only track REAL players in telemetry — bots have no real network round-trip
        telemetryCollector.startTracking(roomCode, session.getId());
        networkControllerOrchestrator.startForSession(roomCode, game.getStateMetadata(), session.getId(), mode);
        broadcastRateRegistry.register(roomCode);

        long periodMs = 1000L / game.getTickRateHz();
        ScheduledFuture<?> task = executor.scheduleAtFixedRate(
                () -> tick(roomCode), periodMs, periodMs, TimeUnit.MILLISECONDS);

        activeMatches.put(roomCode, new ActiveMatch(game, session, task, new AtomicLong(0), new AtomicLong(0), room.getGameId(), enginePlayers));
    }

    public void submitInput(String roomCode, String playerId, PlayerInput input) {
        ActiveMatch match = activeMatches.get(roomCode);
        if (match != null) {
            match.game().processInput(playerId, input);
        }
    }

    private void tick(String roomCode) {
        ActiveMatch match = activeMatches.get(roomCode);
        if (match == null) return;
        try {
            MultiplayerGame game = match.game();

            // --- Bot inputs: computed from last tick's snapshot before advancing state ---
            Object currentSnapshot = game.getStateSnapshot();
            Map<String, PlayerInput> botInputs = botPlayerService.computeBotInputs(
                    roomCode, match.gameId(), currentSnapshot);
            for (Map.Entry<String, PlayerInput> entry : botInputs.entrySet()) {
                game.processInput(entry.getKey(), entry.getValue());
            }

            game.updateState(); // simulation always runs at full rate regardless of broadcast throttling

            if (!game.validateState()) {
                // Academic MVP: log and continue. A production system would reconcile
                // or roll back here; see docs/ARCHITECTURE.md §8 for the authority model.
                System.err.println("[GameLoopManager] validateState() failed for room " + roomCode);
            }

            long tickCount = match.tickCounter().incrementAndGet();
            Object snapshot = game.getStateSnapshot();
            TransportChannel channel = transportGateway.getActiveChannel(roomCode);

            boolean hasIndividualThrottles = broadcastRateRegistry.hasClientThrottles(roomCode);
            if (hasIndividualThrottles) {
                // Per-client adaptive broadcasting (Patent Stage 207 / Claim 1 & 8)
                // Deliver state snapshots individually to each connected player's private topic
                // according to their personalized network throttle factor (N_i).
                for (EnginePlayer ep : match.players()) {
                    if (ep.getUserId() == null) continue; // bots have no client socket
                    String pid = ep.getPlayerId();
                    int clientN = broadcastRateRegistry.getClientEveryNTicks(roomCode, pid);
                    if (tickCount % clientN == 0) {
                        transportGateway.sendToPlayer(roomCode, pid, "state", snapshot, channel);
                        broadcastRateRegistry.incrementClientBroadcast(roomCode, pid);
                    }
                }
                // Deliver to room topic for spectators only when aligned with room-level throttle
                int roomN = broadcastRateRegistry.getEveryNTicks(roomCode);
                if (tickCount % roomN == 0) {
                    transportGateway.broadcastState(roomCode, snapshot, channel);
                    match.broadcastCounter().incrementAndGet();
                }
            } else {
                // Shared room broadcast when no individual throttles are active
                int everyN = broadcastRateRegistry.getEveryNTicks(roomCode);
                if (tickCount % everyN == 0) {
                    transportGateway.broadcastState(roomCode, snapshot, channel);
                    match.broadcastCounter().incrementAndGet();
                }
            }

            if (game.isMatchOver()) {
                finishMatch(roomCode, match);
            }
        } catch (Exception e) {
            System.err.println("[GameLoopManager] tick failed for room " + roomCode + ": " + e.getMessage());
        }
    }

    private void finishMatch(String roomCode, ActiveMatch match) {
        match.tickTask().cancel(false);
        activeMatches.remove(roomCode);
        telemetryCollector.stopTracking(roomCode);
        networkControllerOrchestrator.stopForSession(roomCode);
        networkLabService.clearProfile(roomCode);
        broadcastRateRegistry.clear(roomCode);
        controllerModeRegistry.clear(roomCode);
        botPlayerService.clearRoom(roomCode); // clean up bot registrations

        MatchResultData result = match.game().endMatch();
        Long winnerUserId = parseUserId(result.getWinnerPlayerId());
        sessionService.endSession(match.session(), result, winnerUserId);
        sessionService.recordBroadcastStats(match.session().getId(),
                match.tickCounter().get(), match.broadcastCounter().get());
        roomService.markFinished(roomCode);

        // --- Phase 3: Progression & Leaderboard updates (non-bot real players only) ---
        try {
            List<GamePlayer> players = roomService.getActivePlayers(match.session().getRoomId());
            double matchDurationHours = result != null && result.getDurationSec() > 0
                    ? (result.getDurationSec() / 3600.0) : (2.0 / 60.0);
            for (GamePlayer p : players) {
                if (p.isBot() || p.isSpectator() || p.getUserId() == null) continue;
                boolean won = p.getUserId().equals(winnerUserId);
                progressionService.recordMatchOutcome(p.getUserId(), match.gameId(), won);
                leaderboardService.recordMatchScore(p.getUserId(), match.gameId(), won, 0);
                libraryService.recordPlaytime(p.getUserId(), match.gameId(), matchDurationHours);
            }
        } catch (Exception e) {
            System.err.println("[GameLoopManager] Progression update failed for room " + roomCode + ": " + e.getMessage());
        }

        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/lifecycle",
                new MatchLifecycleMessage(roomCode, "GAME_END", result, match.session().getId()));
    }


    private Long parseUserId(String playerId) {
        if (playerId == null) return null;
        try {
            return Long.valueOf(playerId);
        } catch (NumberFormatException e) {
            return null; // bot winner — no userId
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
