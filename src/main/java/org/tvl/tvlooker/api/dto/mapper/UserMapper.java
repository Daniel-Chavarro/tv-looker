package org.tvl.tvlooker.api.dto.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.api.dto.request.CreateUserRequest;
import org.tvl.tvlooker.api.dto.request.UpdateUserRequest;
import org.tvl.tvlooker.api.dto.response.UserResponse;
import org.tvl.tvlooker.domain.model.User;

/**
 * Mapper for converting between User DTOs and models.
 */
@Component
public class UserMapper {
    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .createdAt(user.getCreatedAt())
                .password(user.getPassword())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }


    public static User toModel(UserResponse userResponse) {
        return User.builder()
                .id(userResponse.getId())
                .username(userResponse.getUsername())
                .password(userResponse.getPassword())
                .createdAt(userResponse.getCreatedAt())
                .email(userResponse.getEmail())
                .name(userResponse.getName())
                .build();
    }

    public static User fromCreateRequest(CreateUserRequest request) {
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .username(request.getUsername())
                .password(request.getPassword())
                .build();
    }

    public static User fromUpdateRequest(UpdateUserRequest request) {
        return User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .name(request.getName())
                .build();
    }
}
