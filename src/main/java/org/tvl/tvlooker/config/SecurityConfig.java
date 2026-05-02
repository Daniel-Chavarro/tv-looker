package org.tvl.tvlooker.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ITEMS_PATTERN = "/api/v1/items/**";
    private static final String GENRES_PATTERN = "/api/v1/genres/**";
    private static final String ACTORS_PATTERN = "/api/v1/actors/**";
    private static final String DIRECTORS_PATTERN = "/api/v1/directors/**";
    private static final String ADMIN_TMDB_PATTERN = "/api/v1/admin/tmdb/**";
    private static final String USERS_PATTERN = "/api/v1/users/**";
    private static final String ADMIN_AUTHORITY = "ADMIN";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                ITEMS_PATTERN,
                                GENRES_PATTERN,
                                ACTORS_PATTERN,
                                DIRECTORS_PATTERN,
                                "/api/v1/reviews/item/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                ITEMS_PATTERN,
                                GENRES_PATTERN,
                                ACTORS_PATTERN,
                                DIRECTORS_PATTERN,
                                ADMIN_TMDB_PATTERN).hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.PATCH,
                                ITEMS_PATTERN,
                                GENRES_PATTERN,
                                ACTORS_PATTERN,
                                DIRECTORS_PATTERN,
                                USERS_PATTERN).hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.DELETE,
                                ITEMS_PATTERN,
                                GENRES_PATTERN,
                                ACTORS_PATTERN,
                                DIRECTORS_PATTERN,
                                USERS_PATTERN).hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.GET, USERS_PATTERN).hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.POST, USERS_PATTERN).hasAuthority(ADMIN_AUTHORITY)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public JwtEncoder jwtEncoder(@Value("${app.security.jwt.secret}") String jwtSecret) {
        SecretKeySpec key = jwtSecretKey(jwtSecret);
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.security.jwt.secret}") String jwtSecret) {
        SecretKeySpec key = jwtSecretKey(jwtSecret);
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private SecretKeySpec jwtSecretKey(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.trim().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
