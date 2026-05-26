package org.tvl.tvlooker.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.enums.UserAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void generateTokenShouldCreateDecodableJwtWithUserClaims() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .password("password")
                .authority(UserAuthority.USER)
                .build();

        String token = jwtService.generateToken(user);

        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("username")).isEqualTo("testuser");
        assertThat(jwt.getClaimAsStringList("authorities")).isEqualTo(List.of("USER"));
    }
}
