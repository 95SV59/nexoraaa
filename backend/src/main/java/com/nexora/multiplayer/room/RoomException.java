package com.nexora.multiplayer.room;

import org.springframework.http.HttpStatus;

/** Lightweight room-layer exception; mapped to an HTTP status by RoomController. */
public class RoomException extends RuntimeException {
    private final HttpStatus status;

    public RoomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static RoomException notFound(String roomCode) {
        return new RoomException("No room with code " + roomCode, HttpStatus.NOT_FOUND);
    }

    public static RoomException full(String roomCode) {
        return new RoomException("Room " + roomCode + " is full", HttpStatus.CONFLICT);
    }

    public static RoomException notInLobby(String roomCode) {
        return new RoomException("Room " + roomCode + " is not accepting players", HttpStatus.CONFLICT);
    }
}
