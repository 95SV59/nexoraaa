package com.nexora.multiplayer.matchmaking;

import com.nexora.multiplayer.engine.GameEngineRegistry;
import com.nexora.multiplayer.matchmaking.dto.MatchmakingStatusResponse;
import com.nexora.multiplayer.room.GameRoom;
import com.nexora.multiplayer.room.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchmakingServiceTest {

    private RoomService roomService;
    private GameEngineRegistry engineRegistry;
    private SimpMessagingTemplate messagingTemplate;
    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        roomService = mock(RoomService.class);
        engineRegistry = mock(GameEngineRegistry.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);

        when(engineRegistry.supports(anyString())).thenReturn(true);

        matchmakingService = new MatchmakingService(roomService, engineRegistry, messagingTemplate);
    }

    @Test
    void testEnqueueAndStatusAndDequeue() {
        MatchmakingStatusResponse res = matchmakingService.enqueue(101L, "PlayerOne", "arena");
        assertEquals("QUEUED", res.status());
        assertEquals("arena", res.gameSlug());
        assertEquals(1, matchmakingService.getQueueSize("arena"));

        MatchmakingStatusResponse status = matchmakingService.getStatus(101L);
        assertEquals("QUEUED", status.status());
        assertEquals("arena", status.gameSlug());

        MatchmakingStatusResponse leaveRes = matchmakingService.dequeue(101L);
        assertEquals("IDLE", leaveRes.status());
        assertEquals(0, matchmakingService.getQueueSize("arena"));
    }

    @Test
    void testMatchmakingPairsTwoPlayers() {
        GameRoom mockRoom = new GameRoom("ABCD12", "arena", 101L, 8, "GLOBAL");
        when(roomService.createRoom(eq("arena"), eq(8), eq("GLOBAL"), eq(101L), eq("PlayerOne")))
                .thenReturn(mockRoom);

        matchmakingService.enqueue(101L, "PlayerOne", "arena");
        matchmakingService.enqueue(102L, "PlayerTwo", "arena");
        assertEquals(2, matchmakingService.getQueueSize("arena"));

        // Trigger scheduler
        matchmakingService.processQueues();

        // Verify room created and player 2 joined
        verify(roomService, times(1)).createRoom("arena", 8, "GLOBAL", 101L, "PlayerOne");
        verify(roomService, times(1)).joinRoom("ABCD12", 102L, "PlayerTwo");

        // Verify notifications sent to both users
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/101/matchmaking"), any(Object.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/102/matchmaking"), any(Object.class));

        // Queue should now be empty
        assertEquals(0, matchmakingService.getQueueSize("arena"));

        // Status should be MATCHED
        MatchmakingStatusResponse status1 = matchmakingService.getStatus(101L);
        assertEquals("MATCHED", status1.status());
        assertEquals("ABCD12", status1.roomCode());

        MatchmakingStatusResponse status2 = matchmakingService.getStatus(102L);
        assertEquals("MATCHED", status2.status());
        assertEquals("ABCD12", status2.roomCode());
    }
}
