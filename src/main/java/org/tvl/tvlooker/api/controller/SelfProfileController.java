package org.tvl.tvlooker.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.mapper.UserMapper;
import org.tvl.tvlooker.api.dto.request.UpdateUserRequest;
import org.tvl.tvlooker.api.dto.response.UserResponse;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.service.UserService;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
public class SelfProfileController {

    private final UserService userService;

    public SelfProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        User user = userService.getById(currentUserId(principal));
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    @PatchMapping
    public ResponseEntity<UserResponse> updateCurrentUser(
            Principal principal,
            @Valid @RequestBody UpdateUserRequest request) {
        User user = UserMapper.fromUpdateRequest(request);
        User updated = userService.update(currentUserId(principal), user);
        return ResponseEntity.ok(UserMapper.toResponse(updated));
    }

    private UUID currentUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
