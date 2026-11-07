package com.nexora.admin;

import com.nexora.admin.dto.AdminStatsDto;
import com.nexora.admin.dto.CreateGameRequest;
import com.nexora.admin.dto.UserManagementDto;
import com.nexora.game.dto.GameDetailDto;
import com.nexora.review.ReviewService;
import com.nexora.review.dto.ReviewDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ReviewService reviewService;

    public AdminController(AdminService adminService, ReviewService reviewService) {
        this.adminService = adminService;
        this.reviewService = reviewService;
    }

    @GetMapping("/stats")
    public AdminStatsDto getStats() {
        return adminService.getStats();
    }

    @GetMapping("/users")
    public List<UserManagementDto> listUsers() {
        return adminService.listUsers();
    }

    @PutMapping("/users/{id}/status")
    public UserManagementDto toggleUserStatus(@PathVariable Long id) {
        return adminService.toggleUserStatus(id);
    }

    @PutMapping("/users/{id}/role")
    public UserManagementDto changeUserRole(@PathVariable Long id, @RequestParam String role) {
        return adminService.changeUserRole(id, role);
    }

    @PostMapping("/games")
    public ResponseEntity<GameDetailDto> createGame(@Valid @RequestBody CreateGameRequest request) {
        GameDetailDto created = adminService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/games/{id}")
    public GameDetailDto updateGame(@PathVariable Long id, @Valid @RequestBody CreateGameRequest request) {
        return adminService.updateGame(id, request);
    }

    @DeleteMapping("/games/{id}")
    public Map<String, Object> deleteGame(@PathVariable Long id) {
        adminService.deleteGame(id);
        return Map.of("deleted", true, "id", id);
    }

    @GetMapping("/reviews/reported")
    public List<ReviewDto> getReportedReviews() {
        return reviewService.getReportedReviews();
    }
}
