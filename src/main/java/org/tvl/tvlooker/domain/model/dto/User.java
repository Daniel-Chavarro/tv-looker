package org.tvl.tvlooker.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.tvl.tvlooker.domain.model.enums.UserAuthority;

import java.sql.Timestamp;
import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class User {
    private final UUID id;
    private final String username;
    private final String email;
    private final String name;
    private final String password;
    private final UserAuthority authority;
    private final Timestamp createdAt;
}
