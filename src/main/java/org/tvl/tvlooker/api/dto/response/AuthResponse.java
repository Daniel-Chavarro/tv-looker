package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.tvl.tvlooker.domain.model.enums.UserAuthority;

import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UUID userId;
    private String username;
    private String email;
    private UserAuthority authority;
}
