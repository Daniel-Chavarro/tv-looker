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
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is already registered");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .authority(UserAuthority.USER)
                .build();

        User savedUser = UserEntityMapper.toDomain(userRepository.save(user));
        return toAuthResponse(savedUser, jwtService.generateToken(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        User authenticatedUser = UserEntityMapper.toDomain(user);
        return toAuthResponse(authenticatedUser, jwtService.generateToken(authenticatedUser));
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .authority(user.getAuthority())
                .build();
    }
}
