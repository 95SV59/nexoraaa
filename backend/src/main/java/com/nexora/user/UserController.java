package com.nexora.user;

import com.nexora.security.AuthPrincipal;
import com.nexora.user.dto.UpdateProfileRequest;
import com.nexora.user.dto.UserProfileDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public UserProfileDto getCurrentProfile(@AuthenticationPrincipal AuthPrincipal principal) {
        return userService.getProfile(principal.userId());
    }

    @GetMapping("/{username}")
    public UserProfileDto getProfileByUsername(@PathVariable String username) {
        return userService.getProfileByUsername(username);
    }

    @PutMapping("/profile")
    public UserProfileDto updateProfile(@AuthenticationPrincipal AuthPrincipal principal,
                                        @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.userId(), request);
    }
}
