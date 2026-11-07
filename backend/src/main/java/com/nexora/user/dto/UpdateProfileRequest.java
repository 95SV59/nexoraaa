package com.nexora.user.dto;

import jakarta.validation.constraints.Email;

public class UpdateProfileRequest {

    @Email(message = "Invalid email format")
    private String email;

    private String avatarUrl;

    public UpdateProfileRequest() { }

    public UpdateProfileRequest(String email, String avatarUrl) {
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
