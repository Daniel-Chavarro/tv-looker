package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests all CRUD operations and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .password("password123")
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("createUser - should save and return user")
    void createUser_shouldSaveAndReturnUser() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.createUser(testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository, times(1)).save(testUser);
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("getById - should return user when user exists")
    void getById_shouldReturnUser_whenUserExists() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getById(testUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    @DisplayName("getById - should throw UserNotFoundException when user does not exist")
    void getById_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getById(nonExistentId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: " + nonExistentId);
        verify(userRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAll - should return all users")
    void getAll_shouldReturnAllUsers() {
        // Arrange
        User user2 = User.builder()
                .id(UUID.randomUUID())
                .username("testuser2")
                .password("password456")
                .build();
        List<User> users = List.of(testUser, user2);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testUser, user2);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no users exist")
    void getAll_shouldReturnEmptyList_whenNoUsersExist() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of());

        // Act
        List<User> result = userService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(userRepository, times(1)).findAll();
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("updateUser - should update and return user when user exists")
    void updateUser_shouldUpdateAndReturnUser_whenUserExists() {
        // Arrange
        User updatedUser = User.builder()
                .username("updateduser")
                .password("newpassword")
                .build();
        User savedUser = User.builder()
                .id(testUserId)
                .username("updateduser")
                .password("newpassword")
                .build();

        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.updateUser(testUserId, updatedUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        assertThat(result.getUsername()).isEqualTo("updateduser");
        verify(userRepository, times(1)).existsById(testUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - should throw UserNotFoundException when user does not exist")
    void updateUser_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        User updatedUser = User.builder()
                .username("updateduser")
                .password("newpassword")
                .build();

        when(userRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(nonExistentId, updatedUser))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: " + nonExistentId);
        verify(userRepository, times(1)).existsById(nonExistentId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - should set ID on user entity before saving")
    void updateUser_shouldSetIdOnUserEntity_beforeSaving() {
        // Arrange
        User updatedUser = User.builder()
                .username("updateduser")
                .password("newpassword")
                .build();

        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertThat(user.getId()).isEqualTo(testUserId);
            return user;
        });

        // Act
        userService.updateUser(testUserId, updatedUser);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("deleteUser - should delete user when user exists")
    void deleteUser_shouldDeleteUser_whenUserExists() {
        // Arrange
        when(userRepository.existsById(testUserId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(testUserId);

        // Act
        userService.deleteUser(testUserId);

        // Assert
        verify(userRepository, times(1)).existsById(testUserId);
        verify(userRepository, times(1)).deleteById(testUserId);
    }

    @Test
    @DisplayName("deleteUser - should throw UserNotFoundException when user does not exist")
    void deleteUser_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(nonExistentId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: " + nonExistentId);
        verify(userRepository, times(1)).existsById(nonExistentId);
        verify(userRepository, never()).deleteById(any(UUID.class));
    }
}
