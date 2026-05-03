package org.tvl.tvlooker.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void jwtEncoderShouldRejectBlankSecret() {
        assertThatThrownBy(() -> securityConfig.jwtEncoder(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT secret must be at least 32 bytes");
    }

    @Test
    void jwtDecoderShouldRejectShortSecret() {
        assertThatThrownBy(() -> securityConfig.jwtDecoder("too-short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT secret must be at least 32 bytes");
    }
}
