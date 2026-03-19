package org.tvl.tvlooker.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.mapper.UserMapper;
import org.tvl.tvlooker.api.dto.request.CreateUserRequest;
import org.tvl.tvlooker.api.dto.request.UpdateUserRequest;
import org.tvl.tvlooker.api.dto.response.UserResponse;
import org.tvl.tvlooker.domain.model.User;
import org.tvl.tvlooker.service.UserService;

import java.util.List;
import java.util.UUID;

/**
 *
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * Retrieves all users.
     * @return A list of user responses.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.getAll();
        List<UserResponse> response = users.stream()
                .map(UserMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        User user = UserMapper.fromCreateRequest(request);
        User created = userService.create(user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/v1/users/" + created.getId())
                .body(UserMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        User user = UserMapper.fromUpdateRequest(request);
        User updated = userService.update(id, user);
        return ResponseEntity.ok(UserMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}