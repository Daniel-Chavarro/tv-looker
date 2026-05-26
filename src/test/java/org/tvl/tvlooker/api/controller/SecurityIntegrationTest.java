package org.tvl.tvlooker.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void givenNoToken_whenGetProtectedReview_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenNoToken_whenGetCurrentUserProfile_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenUserJwt_whenGetCurrentUserProfile_thenPassesSecurityAuthorization() throws Exception {
        int status = mockMvc.perform(get("/api/v1/me")
                        .with(jwt()
                                .jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("USER"))))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void givenUserJwt_whenPatchCurrentUserProfile_thenPassesSecurityAuthorization() throws Exception {
        int status = mockMvc.perform(patch("/api/v1/me")
                        .with(jwt()
                                .jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void givenNoToken_whenGetCatalogItems_thenReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk());
    }

    @Test
    void givenUserJwt_whenMutatingCatalogItems_thenReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/items")
                        .with(jwt().authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenUserJwt_whenPostTmdbAdminSync_thenReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/tmdb/sync")
                        .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenUserJwt_whenGetUsers_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAdminJwtAuthoritiesClaim_whenMutatingCatalogItems_thenPassesSecurityAuthorization() throws Exception {
        int status = mockMvc.perform(post("/api/v1/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void givenNoToken_whenPostInvalidLogin_thenPassesSecurityAuthorization() throws Exception {
        int status = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void givenNoToken_whenPostInvalidRegister_thenPassesSecurityAuthorization() throws Exception {
        int status = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    private String adminToken() {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim("authorities", List.of("ADMIN"))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
