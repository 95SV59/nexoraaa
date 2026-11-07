package com.nexora.multiplayer.room.dto;

import jakarta.validation.constraints.NotBlank;

public class JoinRoomRequest {

    @NotBlank
    private String roomCode;

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
}
