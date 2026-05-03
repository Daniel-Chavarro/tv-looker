# JWT Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add backend-issued stateless JWT authentication with anonymous guest browsing, authenticated `USER` ownership rules, and `ADMIN` management access.

**Architecture:** Use Spring Security and Spring OAuth2 Resource Server with local HMAC JWT signing through Spring Security JOSE/Nimbus. Keep code in the repo's existing layered packages: config in `config`, controllers in `api/controller`, DTOs in `api/dto`, services in `service`, and authority enum in `domain/model/enums`.

**Tech Stack:** Java 21, Spring Boot 4, Spring Security, Spring OAuth2 Resource Server, Spring Security OAuth2 JOSE/Nimbus, Spring Data JPA, H2 tests, JUnit 5, Mockito, MockMvc.

---

## File Map

Create:
- `src/main/java/org/tvl/tvlooker/domain/model/enums/UserAuthority.java` - enum for persisted user authorities.
- `src/main/java/org/tvl/tvlooker/api/dto/request/LoginRequest.java` - login request DTO.
- `src/main/java/org/tvl/tvlooker/api/dto/request/RegisterRequest.java` - registration request DTO.
- `src/main/java/org/tvl/tvlooker/api/dto/response/AuthResponse.java` - token response DTO.
- `src/main/java/org/tvl/tvlooker/service/JwtService.java` - create JWTs and expose current-token claim conventions.
- `src/main/java/org/tvl/tvlooker/service/AuthService.java` - register/login orchestration.
- `src/main/java/org/tvl/tvlooker/api/controller/AuthController.java` - `/api/v1/auth/**` endpoints.
- `src/main/java/org/tvl/tvlooker/config/SecurityConfig.java` - stateless security, JWT encoder/decoder, authority converter, password encoder.
- `src/test/java/org/tvl/tvlooker/service/AuthServiceTest.java` - auth service tests.
- `src/test/java/org/tvl/tvlooker/service/JwtServiceTest.java` - JWT claim tests.
- `src/test/java/org/tvl/tvlooker/api/controller/AuthControllerTest.java` - auth endpoint tests.
- `src/test/java/org/tvl/tvlooker/api/controller/SecurityIntegrationTest.java` - request authorization tests.

Modify:
- `pom.xml` - add Spring Security dependencies and security test support.
- `src/main/java/org/tvl/tvlooker/domain/model/entity/UserEntity.java` - add `authority` field.
- `src/main/java/org/tvl/tvlooker/domain/model/dto/User.java` - add `authority` and preserve password only for internal use.
- `src/main/java/org/tvl/tvlooker/domain/model/mapper/UserEntityMapper.java` - map authority.
- `src/main/java/org/tvl/tvlooker/api/dto/mapper/UserMapper.java` - stop mapping password to response; map register request if useful.
- `src/main/java/org/tvl/tvlooker/api/dto/response/UserResponse.java` - remove password, add authority.
- `src/main/java/org/tvl/tvlooker/persistence/repository/UserRepository.java` - add login lookup methods.
- `src/main/java/org/tvl/tvlooker/persistence/repository/ReviewRepository.java` - add owner-scoped lookup.
- `src/main/java/org/tvl/tvlooker/service/ReviewService.java` - add owner/admin aware methods.
- `src/main/java/org/tvl/tvlooker/api/controller/ReviewController.java` - derive current user from JWT instead of request `userId` for protected user operations.
- `src/main/resources/application.properties` - add JWT config keys with environment fallbacks.
- `src/test/resources/application-test.properties` - add deterministic test JWT secret.
- Existing user/review tests - update assertions and fixtures for authority/password response changes.

Do not commit during execution unless the user explicitly asks for commits.

---

### Task 1: Add Security Dependencies

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add failing dependency verification**

Run: `mvn test -Dtest=JwtServiceTest`

Expected: FAIL because `JwtServiceTest` does not exist yet. This confirms the auth test target is not already implemented.

- [ ] **Step 2: Modify Maven dependencies**

Add these dependencies inside `<dependencies>` in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

If `JwtEncoder` or Nimbus classes are unavailable after this change, add:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>
```

- [ ] **Step 3: Verify Maven can resolve security dependencies**

Run: `mvn test -Dtest=UserServiceTest`

Expected: PASS. If Spring Security starts protecting controller tests later, do not adjust controller tests in this task.

---

### Task 2: Add Authority To User Model And Remove Password From Responses

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/domain/model/enums/UserAuthority.java`
- Modify: `src/main/java/org/tvl/tvlooker/domain/model/entity/UserEntity.java`
- Modify: `src/main/java/org/tvl/tvlooker/domain/model/dto/User.java`
- Modify: `src/main/java/org/tvl/tvlooker/domain/model/mapper/UserEntityMapper.java`
- Modify: `src/main/java/org/tvl/tvlooker/api/dto/response/UserResponse.java`
- Modify: `src/main/java/org/tvl/tvlooker/api/dto/mapper/UserMapper.java`
- Test: `src/test/java/org/tvl/tvlooker/service/UserServiceTest.java`
- Test: `src/test/java/org/tvl/tvlooker/api/controller/UserControllerTest.java`

- [ ] **Step 1: Write failing user response test**

In `UserControllerTest`, add or update a response assertion for `GET /api/v1/users/{id}` so it rejects the password field and includes authority:

```java
mockMvc.perform(get("/api/v1/users/{id}", testUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.authority", is("USER")));
```

- [ ] **Step 2: Run the failing test**

Run: `mvn test -Dtest=UserControllerTest`

Expected: FAIL because `UserResponse` still exposes `password` and has no `authority`.

- [ ] **Step 3: Add authority enum**

Create `src/main/java/org/tvl/tvlooker/domain/model/enums/UserAuthority.java`:

```java
package org.tvl.tvlooker.domain.model.enums;

public enum UserAuthority {
    USER,
    ADMIN
}
```

- [ ] **Step 4: Update user entity/domain model**

In `UserEntity`, add:

```java
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.tvl.tvlooker.domain.model.enums.UserAuthority;
```

and the field:

```java
@Enumerated(EnumType.STRING)
@Column(name = "authority", nullable = false)
@Builder.Default
private UserAuthority authority = UserAuthority.USER;
```

In `User`, add:

```java
import org.tvl.tvlooker.domain.model.enums.UserAuthority;
```

and field:

```java
private final UserAuthority authority;
```

- [ ] **Step 5: Update mappers and response DTO**

In `UserEntityMapper#toDomain`, map:

```java
.authority(entity.getAuthority())
```

In `UserEntityMapper#toEntity`, map defaulted authority:

```java
.authority(domain.getAuthority() == null ? UserAuthority.USER : domain.getAuthority())
```

In `UserResponse`, remove `private String password;` and add:

```java
private UserAuthority authority;
```

In `UserMapper#toResponse`, remove `.password(user.getPassword())` and add:

```java
.authority(user.getAuthority())
```

Remove password reads from `UserMapper#toModel(UserResponse userResponse)` or delete that method if no compile-time references exist.

- [ ] **Step 6: Update existing user test fixtures**

Where tests build `User` or `UserEntity`, add:

```java
.authority(UserAuthority.USER)
```

Import:

```java
import org.tvl.tvlooker.domain.model.enums.UserAuthority;
```

- [ ] **Step 7: Verify model/response behavior**

Run: `mvn test -Dtest=UserServiceTest,UserControllerTest`

Expected: PASS.

---

### Task 3: Add User Repository Login Lookups

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/persistence/repository/UserRepository.java`
- Test: `src/test/java/org/tvl/tvlooker/persistence/repository/UserRepositoryTest.java`

- [ ] **Step 1: Write failing repository tests**

Add tests to `UserRepositoryTest`:

```java
@Test
void givenExistingUsername_whenFindByUsername_thenReturnsUser() {
    UserEntity user = UserEntity.builder()
            .username("loginuser")
            .email("login@example.com")
            .name("Login User")
            .password("{bcrypt}hash")
            .authority(UserAuthority.USER)
            .build();
    UserEntity saved = userRepository.saveAndFlush(user);

    Optional<UserEntity> result = userRepository.findByUsername("loginuser");

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(saved.getId());
}

@Test
void givenExistingEmail_whenFindByEmail_thenReturnsUser() {
    UserEntity user = UserEntity.builder()
            .username("emailuser")
            .email("email-login@example.com")
            .name("Email User")
            .password("{bcrypt}hash")
            .authority(UserAuthority.USER)
            .build();
    UserEntity saved = userRepository.saveAndFlush(user);

    Optional<UserEntity> result = userRepository.findByEmail("email-login@example.com");

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(saved.getId());
}
```

Add imports:

```java
import org.tvl.tvlooker.domain.model.enums.UserAuthority;
import java.util.Optional;
```

- [ ] **Step 2: Run failing repository tests**

Run: `mvn test -Dtest=UserRepositoryTest`

Expected: FAIL because `findByUsername` and `findByEmail` are not defined.

- [ ] **Step 3: Add repository methods**

Update `UserRepository`:

```java
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);
}
```

- [ ] **Step 4: Verify repository lookups**

Run: `mvn test -Dtest=UserRepositoryTest`

Expected: PASS.

---

### Task 4: Add JWT Service

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/service/JwtService.java`
- Create: `src/test/java/org/tvl/tvlooker/service/JwtServiceTest.java`
- Modify: `src/test/resources/application-test.properties`

- [ ] **Step 1: Add test JWT properties**

Add to `src/test/resources/application-test.properties`:

```properties
app.security.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
app.security.jwt.expiration=PT1H
```

- [ ] **Step 2: Write failing JWT service test**

Create `JwtServiceTest`:

```java
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
    void givenUser_whenGenerateToken_thenTokenContainsSubjectAndAuthority() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("tokenuser")
                .email("token@example.com")
                .name("Token User")
                .password("encoded")
                .authority(UserAuthority.USER)
                .build();

        String token = jwtService.generateToken(user);
        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("username")).isEqualTo("tokenuser");
        assertThat(jwt.getClaimAsStringList("authorities")).isEqualTo(List.of("USER"));
    }
}
```

- [ ] **Step 3: Run failing JWT service test**

Run: `mvn test -Dtest=JwtServiceTest`

Expected: FAIL because `JwtService`, `JwtDecoder`, and security config beans are not implemented.

- [ ] **Step 4: Implement JWT service**

Create `JwtService`:

```java
package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.model.dto.User;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder jwtEncoder;

    @Value("${app.security.jwt.expiration:PT1H}")
    private Duration expiration;

    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .claim("username", user.getUsername())
                .claim("authorities", List.of(user.getAuthority().name()))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
```

- [ ] **Step 5: Implement minimal JWT encoder/decoder beans**

Create `SecurityConfig` in Task 6 before expecting this test to pass. If executing task-by-task strictly, create only JWT encoder/decoder beans now and expand security rules in Task 6.

---

### Task 5: Add Auth Service And DTOs

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/api/dto/request/LoginRequest.java`
- Create: `src/main/java/org/tvl/tvlooker/api/dto/request/RegisterRequest.java`
- Create: `src/main/java/org/tvl/tvlooker/api/dto/response/AuthResponse.java`
- Create: `src/main/java/org/tvl/tvlooker/service/AuthService.java`
- Test: `src/test/java/org/tvl/tvlooker/service/AuthServiceTest.java`

- [ ] **Step 1: Write failing auth service tests**

Create `AuthServiceTest` with these behaviors:

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    @Test
    void givenRegisterRequest_whenRegister_thenHashesPasswordAndReturnsToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setName("New User");
        request.setPassword("plain-password");

        UserEntity saved = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("newuser")
                .email("new@example.com")
                .name("New User")
                .password("encoded-password")
                .authority(UserAuthority.USER)
                .build();

        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenReturn(saved);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getAuthority()).isEqualTo(UserAuthority.USER);
        verify(userRepository).save(argThat(entity ->
                entity.getPassword().equals("encoded-password")
                        && entity.getAuthority() == UserAuthority.USER));
    }

    @Test
    void givenBadPassword_whenLogin_thenThrowsBadCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("loginuser");
        request.setPassword("wrong");
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("loginuser")
                .email("login@example.com")
                .password("encoded")
                .authority(UserAuthority.USER)
                .build();

        when(userRepository.findByUsername("loginuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
```

- [ ] **Step 2: Run failing auth service tests**

Run: `mvn test -Dtest=AuthServiceTest`

Expected: FAIL because DTOs and `AuthService` do not exist.

- [ ] **Step 3: Add auth DTOs**

`LoginRequest`:

```java
package org.tvl.tvlooker.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;
}
```

`RegisterRequest`:

```java
package org.tvl.tvlooker.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 100)
    private String name;

    @NotBlank(message = "Password is required")
    @Size(max = 100)
    private String password;
}
```

`AuthResponse`:

```java
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
```

- [ ] **Step 4: Implement AuthService**

Create `AuthService`:

```java
package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.api.dto.request.LoginRequest;
import org.tvl.tvlooker.api.dto.request.RegisterRequest;
import org.tvl.tvlooker.api.dto.response.AuthResponse;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.enums.UserAuthority;
import org.tvl.tvlooker.domain.model.mapper.UserEntityMapper;
import org.tvl.tvlooker.persistence.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        UserEntity entity = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .authority(UserAuthority.USER)
                .build();
        User user = UserEntityMapper.toDomain(userRepository.save(entity));
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity entity = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), entity.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return toAuthResponse(UserEntityMapper.toDomain(entity));
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .authority(user.getAuthority())
                .build();
    }
}
```

- [ ] **Step 5: Verify auth service**

Run: `mvn test -Dtest=AuthServiceTest`

Expected: PASS.

---

### Task 6: Add Security Configuration

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/config/SecurityConfig.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/test/resources/application-test.properties`
- Test: `src/test/java/org/tvl/tvlooker/service/JwtServiceTest.java`

- [ ] **Step 1: Add application JWT properties**

Add to `src/main/resources/application.properties`:

```properties
app.security.jwt.secret=${JWT_SECRET:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef}
app.security.jwt.expiration=${JWT_EXPIRATION:PT1H}
```

- [ ] **Step 2: Implement SecurityConfig**

Create `SecurityConfig` with:

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/items/**", "/api/v1/genres/**",
                                "/api/v1/actors/**", "/api/v1/directors/**",
                                "/api/v1/reviews/item/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/items/**", "/api/v1/genres/**",
                                "/api/v1/actors/**", "/api/v1/directors/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/items/**", "/api/v1/genres/**",
                                "/api/v1/actors/**", "/api/v1/directors/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/items/**", "/api/v1/genres/**",
                                "/api/v1/actors/**", "/api/v1/directors/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Also add `JwtEncoder`, `JwtDecoder`, and `JwtAuthenticationConverter` beans using `SecretKeySpec` and Nimbus. The complete bean code is:

```java
@Bean
JwtEncoder jwtEncoder() {
    SecretKeySpec key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
}

@Bean
JwtDecoder jwtDecoder() {
    SecretKeySpec key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
}

@Bean
JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("authorities");
    authoritiesConverter.setAuthorityPrefix("");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
}
```

- [ ] **Step 3: Run JWT service test**

Run: `mvn test -Dtest=JwtServiceTest`

Expected: PASS.

---

### Task 7: Add Auth Controller

**Files:**
- Create: `src/main/java/org/tvl/tvlooker/api/controller/AuthController.java`
- Create: `src/test/java/org/tvl/tvlooker/api/controller/AuthControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Create `AuthControllerTest` with standalone MockMvc and `GlobalExceptionHandler`:

```java
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock private AuthService authService;
    @InjectMocks private AuthController authController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void givenValidLogin_whenLogin_thenReturnsToken() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .userId(UUID.randomUUID())
                .username("loginuser")
                .email("login@example.com")
                .authority(UserAuthority.USER)
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"loginuser","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt-token")))
                .andExpect(jsonPath("$.authority", is("USER")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
```

- [ ] **Step 2: Run failing controller test**

Run: `mvn test -Dtest=AuthControllerTest`

Expected: FAIL because `AuthController` does not exist.

- [ ] **Step 3: Implement AuthController**

Create controller:

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
```

- [ ] **Step 4: Verify auth controller**

Run: `mvn test -Dtest=AuthControllerTest`

Expected: PASS.

---

### Task 8: Secure Review Ownership

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/persistence/repository/ReviewRepository.java`
- Modify: `src/main/java/org/tvl/tvlooker/service/ReviewService.java`
- Modify: `src/main/java/org/tvl/tvlooker/api/controller/ReviewController.java`
- Test: `src/test/java/org/tvl/tvlooker/persistence/repository/ReviewRepositoryTest.java`
- Test: `src/test/java/org/tvl/tvlooker/service/ReviewServiceTest.java`
- Test: `src/test/java/org/tvl/tvlooker/api/controller/ReviewControllerTest.java`

- [ ] **Step 1: Write failing repository owner lookup test**

Add this `ReviewRepositoryTest` case after the existing user-scoped query tests. Reuse the repository test's existing user/item fixture helpers if present; otherwise create two users, one item, and one review in the test setup:

```java
@Test
void givenReviewOwnedByUser_whenFindByIdAndUserId_thenOnlyOwnerCanFetchIt() {
    UserEntity owner = userRepository.saveAndFlush(UserEntity.builder()
            .username("review-owner")
            .email("review-owner@example.com")
            .password("encoded")
            .authority(UserAuthority.USER)
            .build());
    UserEntity otherUser = userRepository.saveAndFlush(UserEntity.builder()
            .username("review-other")
            .email("review-other@example.com")
            .password("encoded")
            .authority(UserAuthority.USER)
            .build());
    ItemEntity item = itemRepository.saveAndFlush(ItemEntity.builder()
            .title("Owner Test Item")
            .build());
    ReviewEntity review = reviewRepository.saveAndFlush(ReviewEntity.builder()
            .user(owner)
            .item(item)
            .score(5)
            .reviewText("Owned review")
            .build());

    Optional<ReviewEntity> ownerResult = reviewRepository.findByIdAndUserId(review.getId(), owner.getId());
    Optional<ReviewEntity> otherResult = reviewRepository.findByIdAndUserId(review.getId(), otherUser.getId());

    assertThat(ownerResult).isPresent();
    assertThat(otherResult).isEmpty();
}
```

- [ ] **Step 2: Add repository method**

Update `ReviewRepository`:

```java
Optional<ReviewEntity> findByIdAndUserId(Long id, UUID userId);
```

- [ ] **Step 3: Add service tests for owner and admin access**

Add tests for these methods:

```java
Review getByIdForUser(Long id, UUID userId)
Review getByIdForAdmin(Long id)
Review createForUser(Review review, UUID userId)
Review updateForUser(Long id, Review review, UUID userId)
void deleteByIdForUser(Long id, UUID userId)
```

Expected behavior: user-scoped methods use `findByIdAndUserId`; admin method delegates to existing `getById`; create ignores request user ID and uses the JWT user ID.

- [ ] **Step 4: Implement service owner methods**

In `ReviewService`, implement:

```java
public Review getByIdForUser(Long id, UUID userId) {
    return reviewRepository.findByIdAndUserId(id, userId)
            .map(ReviewEntityMapper::toDomain)
            .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + id));
}

public Review getByIdForAdmin(Long id) {
    return getById(id);
}

public Review createForUser(Review review, UUID userId) {
    Review ownedReview = Review.builder()
            .userId(userId)
            .itemId(review.getItemId())
            .score(review.getScore())
            .reviewText(review.getReviewText())
            .reviewDate(review.getReviewDate())
            .build();
    return create(ownedReview);
}

public Review updateForUser(Long id, Review review, UUID userId) {
    Review existing = getByIdForUser(id, userId);
    Review ownedReview = Review.builder()
            .id(existing.getId())
            .userId(userId)
            .itemId(existing.getItemId())
            .score(review.getScore())
            .reviewText(review.getReviewText())
            .reviewDate(review.getReviewDate())
            .build();
    return update(id, ownedReview);
}

public void deleteByIdForUser(Long id, UUID userId) {
    getByIdForUser(id, userId);
    reviewRepository.deleteById(id);
}
```

- [ ] **Step 5: Update controller to use Authentication principal**

In `ReviewController`, add a helper:

```java
private UUID currentUserId(Authentication authentication) {
    return UUID.fromString(authentication.getName());
}

private boolean isAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ADMIN"));
}
```

Use it in `GET /{id}`:

```java
@GetMapping("/{id}")
public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id, Authentication authentication) {
    Review review = isAdmin(authentication)
            ? reviewService.getByIdForAdmin(id)
            : reviewService.getByIdForUser(id, currentUserId(authentication));
    return ResponseEntity.ok(ReviewMapper.toResponse(review));
}
```

Add current-user listing:

```java
@GetMapping("/me")
public ResponseEntity<PageResponse<ReviewResponse>> getMyReviews(
        Authentication authentication,
        @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<Review> reviewsPage = reviewService.getByUserId(currentUserId(authentication), pageable);
    List<ReviewResponse> content = reviewsPage.getContent().stream()
            .map(ReviewMapper::toResponse)
            .toList();
    return ResponseEntity.ok(new PageResponse<>(
            content,
            reviewsPage.getTotalElements(),
            reviewsPage.getNumber(),
            reviewsPage.getTotalPages(),
            reviewsPage.isLast()));
}
```

- [ ] **Step 6: Verify review ownership**

Run: `mvn test -Dtest=ReviewRepositoryTest,ReviewServiceTest,ReviewControllerTest`

Expected: PASS.

---

### Task 9: Add Security Integration Tests

**Files:**
- Create: `src/test/java/org/tvl/tvlooker/api/controller/SecurityIntegrationTest.java`

- [ ] **Step 1: Write failing authorization tests**

Create Spring Boot MockMvc tests with `spring-security-test`:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void givenNoToken_whenGetProtectedReview_thenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenGuest_whenGetPublicItems_thenReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk());
    }

    @Test
    void givenUserJwt_whenPostCatalogItem_thenReturns403() throws Exception {
        mockMvc.perform(post("/api/v1/items")
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run failing integration tests**

Run: `mvn test -Dtest=SecurityIntegrationTest`

Expected: FAIL until security config and endpoint matchers are complete.

- [ ] **Step 3: Adjust security matchers**

Set the final matcher order in `SecurityConfig` to keep auth endpoints public, keep safe catalog reads public, require `ADMIN` for catalog writes, and require authentication for all remaining endpoints:

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/v1/items/**", "/api/v1/genres/**",
                "/api/v1/actors/**", "/api/v1/directors/**", "/api/v1/reviews/item/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/v1/items/**", "/api/v1/genres/**",
                "/api/v1/actors/**", "/api/v1/directors/**").hasAuthority("ADMIN")
        .requestMatchers(HttpMethod.PATCH, "/api/v1/items/**", "/api/v1/genres/**",
                "/api/v1/actors/**", "/api/v1/directors/**").hasAuthority("ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/api/v1/items/**", "/api/v1/genres/**",
                "/api/v1/actors/**", "/api/v1/directors/**").hasAuthority("ADMIN")
        .anyRequest().authenticated())
```

- [ ] **Step 4: Verify security integration tests**

Run: `mvn test -Dtest=SecurityIntegrationTest`

Expected: PASS.

---

### Task 10: Extend Ownership Pattern To Lists And Interactions

**Files:**
- Modify: `src/main/java/org/tvl/tvlooker/service/ListFavoriteService.java`
- Modify: `src/main/java/org/tvl/tvlooker/api/controller/ListFavoriteController.java`
- Modify: `src/main/java/org/tvl/tvlooker/persistence/repository/ListFavoriteRepository.java`
- Modify: `src/main/java/org/tvl/tvlooker/service/InteractionService.java`
- Modify: `src/main/java/org/tvl/tvlooker/api/controller/InteractionController.java`
- Modify: `src/main/java/org/tvl/tvlooker/persistence/repository/InteractionRepository.java`

- [ ] **Step 1: Add owner-scoped repository methods**

Add methods following the review pattern:

```java
Optional<ListFavoriteEntity> findByIdAndUserId(Long id, UUID userId);
Optional<InteractionEntity> findByIdAndUserId(Long id, UUID userId);
Page<InteractionEntity> findAllByUserId(UUID userId, Pageable pageable);
```

- [ ] **Step 2: Add `/me` controller tests**

For lists and interactions, add controller tests using mocked `Authentication` that prove the controller passes the current JWT subject to the service:

```java
Authentication authentication = mock(Authentication.class);
when(authentication.getName()).thenReturn(testUserId.toString());
when(listFavoriteService.getByUserId(eq(testUserId), any(Pageable.class))).thenReturn(favoritesPage);

ResponseEntity<PageResponse<ListFavoriteResponse>> response =
        listFavoriteController.getMyLists(authentication, PageRequest.of(0, 50));

assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
verify(listFavoriteService).getByUserId(eq(testUserId), any(Pageable.class));
```

Use the same pattern for `interactionController.getMyInteractions(authentication, pageable)`.

- [ ] **Step 3: Implement owner-scoped services and controllers**

Add owner-scoped service methods with the same semantics as reviews:

```java
public ListFavorite getByIdForUser(Long id, UUID userId) {
    return listFavoriteRepository.findByIdAndUserId(id, userId)
            .map(ListFavoriteEntityMapper::toDomain)
            .orElseThrow(() -> new ListFavoriteNotFoundException("Favorite list not found: " + id));
}

public Interaction getByIdForUser(Long id, UUID userId) {
    return interactionRepository.findByIdAndUserId(id, userId)
            .map(InteractionEntityMapper::toDomain)
            .orElseThrow(() -> new InteractionNotFoundException("Interaction not found: " + id));
}
```

Add `GET /api/v1/lists/me` and `GET /api/v1/interactions/me` controller methods that parse `UUID.fromString(authentication.getName())` and call the corresponding `getByUserId` service method.

- [ ] **Step 4: Verify user-owned resources**

Run: `mvn test -Dtest=ListFavorite*Test,Interaction*Test`

Expected: PASS for existing and new tests.

---

### Task 11: Full Backend Verification

**Files:**
- All changed backend files.

- [ ] **Step 1: Run focused auth/security tests**

Run: `mvn test -Dtest=AuthServiceTest,JwtServiceTest,AuthControllerTest,SecurityIntegrationTest`

Expected: PASS.

- [ ] **Step 2: Run focused user/review tests**

Run: `mvn test -Dtest=UserServiceTest,UserControllerTest,UserRepositoryTest,ReviewServiceTest,ReviewControllerTest,ReviewRepositoryTest`

Expected: PASS.

- [ ] **Step 3: Run backend merge gate**

Run: `mvn clean verify`

Expected: PASS. If Checkstyle or PMD flags formatting/naming, fix only files touched by this implementation.

- [ ] **Step 4: Check working tree**

Run: `git status --short`

Expected: only intended auth/security/spec/plan files are changed or untracked.
