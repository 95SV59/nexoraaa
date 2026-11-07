package com.nexora.multiplayer.room;

import com.nexora.multiplayer.engine.GameLoopManager;
import com.nexora.multiplayer.room.dto.CreateRoomRequest;
import com.nexora.multiplayer.room.dto.JoinRoomRequest;
import com.nexora.multiplayer.room.dto.PlayerSummary;
import com.nexora.multiplayer.room.dto.RoomResponse;
import com.nexora.multiplayer.session.GameSession;
import com.nexora.multiplayer.websocket.dto.LobbyUpdateMessage;
import com.nexora.multiplayer.websocket.dto.MatchLifecycleMessage;
import com.nexora.network.experiment.ControllerModeRegistry;
import com.nexora.security.AuthPrincipal;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST surface for room lifecycle (create/join/ready/leave/lookup). In-match, high-
 * frequency traffic never comes through here — see multiplayer.websocket after a
 * player has joined a room via these endpoints (Phase 7 scope: WebSocket, room
 * creation, room joining, lobby, player state, match lifecycle).
 *
 * Every endpoint requires authentication (see SecurityConfig); identity comes from
 * @AuthenticationPrincipal, never from the request body — the TODO(Phase 5) comments
 * that used to mark client-supplied userId/displayName as temporary are gone because
 * this is what replaced them.
 *
 * Bot endpoints (Phase 7 extension):
 *  POST /{roomCode}/fill-bots  — Host fills empty slots with AI bots.
 *  POST /{roomCode}/start-now  — Host fills bots + starts match immediately.
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final UserRepository userRepository;
    private final ControllerModeRegistry controllerModeRegistry;
    private final GameLoopManager gameLoopManager;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomController(RoomService roomService, UserRepository userRepository,
                           ControllerModeRegistry controllerModeRegistry,
                           GameLoopManager gameLoopManager,
                           SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.userRepository = userRepository;
        this.controllerModeRegistry = controllerModeRegistry;
        this.gameLoopManager = gameLoopManager;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest req,
                                                    @AuthenticationPrincipal AuthPrincipal principal) {
        User user = requireUser(principal);
        GameRoom room = roomService.createRoom(req.getGameId(), req.getMaxPlayers(),
                req.getRegion(), user.getId(), user.getUsername());
        if (req.getControllerMode() != null) {
            controllerModeRegistry.setMode(room.getRoomCode(),
                    GameSession.ControllerMode.valueOf(req.getControllerMode().toUpperCase()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RoomResponse.from(room, roomService.getActivePlayers(room.getId()), user.getId()));
    }

    @PostMapping("/join")
    public ResponseEntity<RoomResponse> joinRoom(@Valid @RequestBody JoinRoomRequest req,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        User user = requireUser(principal);
        GameRoom room = roomService.joinRoom(req.getRoomCode(), user.getId(), user.getUsername());
        return ResponseEntity.ok(RoomResponse.from(room, roomService.getActivePlayers(room.getId()), user.getId()));
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String roomCode,
                                                 @AuthenticationPrincipal AuthPrincipal principal) {
        GameRoom room = roomService.getRoomOrThrow(roomCode);
        Long currentUserId = principal != null ? principal.userId() : null;
        return ResponseEntity.ok(RoomResponse.from(room, roomService.getActivePlayers(room.getId()), currentUserId));
    }

    @PostMapping("/{roomCode}/ready")
    public ResponseEntity<Void> setReady(@PathVariable String roomCode,
                                         @RequestParam(defaultValue = "true") boolean ready,
                                         @AuthenticationPrincipal AuthPrincipal principal) {
        roomService.setReady(roomCode, principal.userId(), ready);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomCode}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable String roomCode,
                                           @AuthenticationPrincipal AuthPrincipal principal) {
        roomService.leaveRoom(roomCode, principal.userId());
        return ResponseEntity.noContent().build();
    }

    public record ReconnectRequest(String reconnectToken) {}

    @PostMapping("/{roomCode}/reconnect")
    public ResponseEntity<RoomResponse> reconnectRoom(
            @PathVariable String roomCode,
            @RequestBody ReconnectRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        GamePlayer player = roomService.reconnectPlayer(roomCode, principal.userId(), req.reconnectToken());
        GameRoom room = roomService.getRoomOrThrow(roomCode);
        broadcastLobby(roomCode, room);
        return ResponseEntity.ok(RoomResponse.from(room, roomService.getActivePlayers(room.getId()), principal.userId()));
    }

    /**
     * Fill remaining slots with AI bots. Only the host may call this.
     * Returns the number of bots added. Can be called multiple times if more humans
     * join after partial fill (excess bots would already be registered).
     */
    @PostMapping("/{roomCode}/fill-bots")
    public ResponseEntity<Map<String, Object>> fillBots(@PathVariable String roomCode,
                                                         @AuthenticationPrincipal AuthPrincipal principal) {
        GameRoom room = roomService.getRoomOrThrow(roomCode);
        requireHost(room, principal.userId());
        int added = roomService.fillWithBots(roomCode);
        // Broadcast updated lobby so all connected clients see the new bot players immediately
        broadcastLobby(roomCode, roomService.getRoomOrThrow(roomCode));
        return ResponseEntity.ok(Map.of("botsAdded", added, "roomCode", roomCode));
    }

    /**
     * "Start Now" — fills remaining slots with bots and then starts the match immediately.
     * Requires ≥ 1 real player (the host) already in the room. No minimum real-player
     * count beyond that (bots fill any gap). Host-only.
     */
    @PostMapping("/{roomCode}/start-now")
    public ResponseEntity<Map<String, Object>> startNow(@PathVariable String roomCode,
                                                         @AuthenticationPrincipal AuthPrincipal principal) {
        GameRoom room = roomService.getRoomOrThrow(roomCode);
        requireHost(room, principal.userId());

        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is not in lobby state.");
        }

        // Fill remaining slots with bots
        int botsAdded = roomService.fillWithBots(roomCode);

        // Ready all bots (they're auto-ready in DB) and start the match
        gameLoopManager.startMatch(roomCode);

        return ResponseEntity.ok(Map.of(
                "roomCode", roomCode,
                "botsAdded", botsAdded,
                "started", true
        ));
    }

    @PostMapping("/{roomCode}/spectate")
    public ResponseEntity<RoomResponse> spectateRoom(
            @PathVariable String roomCode,
            @AuthenticationPrincipal AuthPrincipal principal) {
        User user = requireUser(principal);
        GameRoom room = roomService.spectateRoom(roomCode, user.getId(), user.getUsername());
        broadcastLobby(roomCode, room);
        return ResponseEntity.ok(RoomResponse.from(room, roomService.getActivePlayers(room.getId()), user.getId()));
    }

    @PostMapping("/{roomCode}/rematch")
    public ResponseEntity<RoomResponse> rematch(
            @PathVariable String roomCode,
            @AuthenticationPrincipal AuthPrincipal principal) {
        GameRoom room = roomService.getRoomOrThrow(roomCode);
        requireHost(room, principal.userId());
        GameRoom reset = roomService.resetForRematch(roomCode);
        broadcastLobby(roomCode, reset);
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/lifecycle",
                new MatchLifecycleMessage(roomCode, "REMATCH_RESET", null, null));
        return ResponseEntity.ok(RoomResponse.from(reset, roomService.getActivePlayers(reset.getId()), principal.userId()));
    }

    @ExceptionHandler(RoomException.class)
    public ResponseEntity<String> handleRoomException(RoomException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadControllerMode(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body("controllerMode must be BASELINE or PROPOSED");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    private User requireUser(AuthPrincipal principal) {
        return userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }

    private void requireHost(GameRoom room, Long userId) {
        if (!room.getHostUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can perform this action.");
        }
    }

    private void broadcastLobby(String roomCode, GameRoom room) {
        List<PlayerSummary> players = roomService.getActivePlayers(room.getId()).stream()
                .map(p -> new PlayerSummary(p.getUserId(), p.getDisplayName(), p.isReady(), p.isBot(), p.isSpectator(), p.getPingMs()))
                .collect(Collectors.toList());
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/lobby",
                new LobbyUpdateMessage(roomCode, room.getStatus().name(), players));
    }
}
