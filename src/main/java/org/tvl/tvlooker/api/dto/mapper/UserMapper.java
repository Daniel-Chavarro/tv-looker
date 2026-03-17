package org.tvl.tvlooker.api.dto.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.api.dto.request.CreateUserRequest;
import org.tvl.tvlooker.api.dto.request.UpdateUserRequest;
import org.tvl.tvlooker.api.dto.response.UserResponse;
import org.tvl.tvlooker.domain.model.entity.User;

@Component
public class UserMapper {
    // TODO: add name in User entity
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // Possible need to add password in response
    public User toModel(UserResponse userResponse) {
        return User.builder()
                .id(userResponse.getId())
                .username(userResponse.getUsername())
                .createdAt(userResponse.getCreatedAt())
                .build();
    }

    public User fromCreateRequest(CreateUserRequest request) {
        return User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build();
    }

    public User fromUpdateRequest(UpdateUserRequest request) {
        return User.builder()
                .username(request.getUsername())
                .build();
    }
}
