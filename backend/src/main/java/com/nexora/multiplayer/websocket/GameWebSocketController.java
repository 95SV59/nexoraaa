package com.nexora.multiplayer.websocket;

import com.nexora.multiplayer.engine.GameLoopManager;
import com.nexora.multiplayer.engine.PlayerInput;
import com.nexora.multiplayer.room.GameRoom;
import com.nexora.multiplayer.room.RoomService;
import com.nexora.multiplayer.websocket.dto.InputRequest;
import com.nexora.multiplayer.websocket.dto.LobbyUpdateMessage;
import com.nexora.multiplayer.websocket.dto.PongRequest;
import com.nexora.multiplayer.websocket.dto.ReadyRequest;
import com.nexora.multiplayer.room.dto.PlayerSummary;
import com.nexora.network.lab.NetworkLabService;
import com.nexora.network.telemetry.TelemetryCollector;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * STOMP inbound handlers, the real-time half of Phase 7 (REST — RoomController —
 * handles room creation/join; this handles ready-up, per-tick input, and Phase 10
 * ping/pong telemetry once a match is running). See docs/WEBSOCKET_PROTOCOL.md.
 *
 * Every handler takes a Principal — populated by StompAuthChannelInterceptor from the
 * JWT presented on STOMP CONNECT — and derives playerId from it via Long.valueOf(
 * principal.getName()). None of the request DTOs carry an identity field anymore: a
 * client that could put any userId in an InputRequest could move or fire as anyone,
 * which is exactly the hole Phase 5 closes.
 *
 * Input and pong handling are routed through NetworkLabService (Phase 16): with no
 * active lab profile this is a plain pass-through, but with one active it can delay or
 * silently drop either message to simulate a degraded connection — see NetworkLabService.
 */
@Controller
public class GameWebSocketController {

    private final RoomService roomService;
    private final GameLoopManager gameLoopManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final TelemetryCollector telemetryCollector;
    private final NetworkLabService networkLabService;

    public GameWebSocketController(RoomService roomService, GameLoopManager gameLoopManager,
                                    SimpMessagingTemplate messagingTemplate,
                                    TelemetryCollector telemetryCollector,
                                    NetworkLabService networkLabService) {
        this.roomService = roomService;
        this.gameLoopManager = gameLoopManager;
        this.messagingTemplate = messagingTemplate;
        this.telemetryCollector = telemetryCollector;
        this.networkLabService = networkLabService;
    }

    /** Client sends to /app/room/{roomCode}/ready */
    @MessageMapping("/room/{roomCode}/ready")
    public void handleReady(@DestinationVariable String roomCode, ReadyRequest req, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        roomService.setReady(roomCode, userId, req.isReady());
        GameRoom room = roomService.getRoomOrThrow(roomCode);

        broadcastLobby(roomCode, room);

        if (room.getStatus() == com.nexora.multiplayer.room.RoomStatus.LOBBY
                && roomService.allReady(room.getId())) {
            gameLoopManager.startMatch(roomCode);
        }
    }

    /** Client sends to /app/room/{roomCode}/input, continuously while playing. */
    @MessageMapping("/room/{roomCode}/input")
    public void handleInput(@DestinationVariable String roomCode, InputRequest req, Principal principal) {
        String playerId = principal.getName();
        if (networkLabService.shouldDrop(roomCode)) {
            return; // simulated packet loss: this tick's input never "arrives"
        }
        networkLabService.runWithSimulatedDelay(roomCode, () -> {
            PlayerInput input = new PlayerInput();
            input.setPlayerId(playerId);
            input.setSequence(req.getSequence());
            input.setClientTimestamp(req.getClientTimestamp());
            input.setMoveX(req.getMoveX());
            input.setMoveY(req.getMoveY());
            input.setAimAngleRad(req.getAimAngleRad());
            input.setFiring(req.isFiring());
            input.setActionPressed(req.isActionPressed());

            gameLoopManager.submitInput(roomCode, playerId, input);
            telemetryCollector.onInputReceived(roomCode, playerId);
        });
    }

    /** Client echoes back /topic/room/{roomCode}/ping via /app/room/{roomCode}/pong;
     *  RTT is computed here, in server time only (docs/ARCHITECTURE.md §8 spirit).
     *  A lab profile's latency/jitter is applied before this runs, so it shows up as
     *  genuinely measured RTT — see NetworkLabService and TelemetryCollector. */
    @MessageMapping("/room/{roomCode}/pong")
    public void handlePong(@DestinationVariable String roomCode, PongRequest req, Principal principal) {
        String playerId = principal.getName();
        if (networkLabService.shouldDrop(roomCode)) {
            return; // simulated packet loss: this ping round counts as missed
        }
        networkLabService.runWithSimulatedDelay(roomCode, () ->
                telemetryCollector.onPongReceived(roomCode, playerId, req.getPingId(), req.getServerSendTime()));
    }

    private void broadcastLobby(String roomCode, GameRoom room) {
        List<PlayerSummary> players = roomService.getActivePlayers(room.getId()).stream()
                .map(p -> new PlayerSummary(p.getUserId(), p.getDisplayName(), p.isReady(), p.isBot(), p.getPingMs()))
                .collect(Collectors.toList());
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/lobby",
                new LobbyUpdateMessage(roomCode, room.getStatus().name(), players));
    }
}
