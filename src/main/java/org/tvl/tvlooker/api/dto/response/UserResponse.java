package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * DTO for user response.
 */
@Builder
@Getter
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String name;
    private String password;
    private Timestamp createdAt;
}