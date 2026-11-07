package com.nexora.social.group;

import com.nexora.security.AuthPrincipal;
import com.nexora.social.group.dto.CreateGroupRequest;
import com.nexora.social.group.dto.GroupDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupDto> createGroup(
            @Valid @RequestBody CreateGroupRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        GroupDto created = groupService.createGroup(req, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<GroupDto>> listAllGroups(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(groupService.listAllGroups(principal.userId()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<GroupDto>> listMyGroups(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(groupService.listMyGroups(principal.userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupDto> getGroup(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(groupService.getGroupDetails(id, principal.userId()));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<GroupDto> joinGroup(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(groupService.joinGroup(id, principal.userId()));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        groupService.leaveGroup(id, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
