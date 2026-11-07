package com.nexora.admin.dto;

import java.time.Instant;

public class UserManagementDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;
    private Instant createdAt;

    public UserManagementDto() { }

    public UserManagementDto(Long id, String username, String email, String role, String status, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
