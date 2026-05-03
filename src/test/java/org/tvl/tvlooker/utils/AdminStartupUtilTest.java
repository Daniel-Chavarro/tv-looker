package org.tvl.tvlooker.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.enums.UserAuthority;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStartupUtil Unit Tests")
class AdminStartupUtilTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments applicationArguments;

    private AdminStartupUtil adminStartupUtil;

    @BeforeEach
    void setUp() {
        adminStartupUtil = new AdminStartupUtil(
                userRepository,
                passwordEncoder,
                "admin",
                "admin-password",
                "admin@test.com",
                "Admin User");
    }

    @Test
    void runShouldCreateAdminUserWhenNoExistingUserMatches() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin-password")).thenReturn("hashed-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminStartupUtil.run(applicationArguments);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("admin");
        assertThat(savedUser.getEmail()).isEqualTo("admin@test.com");
        assertThat(savedUser.getName()).isEqualTo("Admin User");
        assertThat(savedUser.getPassword()).isEqualTo("hashed-password");
        assertThat(savedUser.getAuthority()).isEqualTo(UserAuthority.ADMIN);
        verify(passwordEncoder).encode("admin-password");
    }

    @Test
    void runShouldSkipWhenUsernameAlreadyExists() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(UserEntity.builder().build()));

        adminStartupUtil.run(applicationArguments);

        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void runShouldSkipWhenEmailAlreadyExists() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(UserEntity.builder().build()));

        adminStartupUtil.run(applicationArguments);

        verify(userRepository).findByUsername("admin");
        verify(userRepository).findByEmail("admin@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}

