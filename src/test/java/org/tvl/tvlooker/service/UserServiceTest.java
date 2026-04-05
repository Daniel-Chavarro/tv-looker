package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserEntity testUserEntity;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@test.com")
                .name("Test User")
                .password("password123")
                .build();

        testUserEntity = UserEntity.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@test.com")
                .name("Test User")
                .password("password123")
                .build();
    }

    @Test
    @DisplayName("create - should save and return user")
    void createUser_shouldSaveAndReturn() {
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUserEntity);

        User result = userService.create(testUser);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("getById - should return user when user exists")
    void getById_shouldReturnUser_whenUserExists() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUserEntity));

        User result = userService.getById(testUserId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    @DisplayName("getById - should throw UserNotFoundException when user does not exist")
    void getById_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(nonExistentId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: " + nonExistentId);
        verify(userRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAll - should return all users")
    void getAll_shouldReturnAllUsers() {
        UserEntity user2 = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("testuser2")
                .email("test2@test.com")
                .name("Test User 2")
                .password("password456")
                .build();
        List<UserEntity> users = List.of(testUserEntity, user2);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no users exist")
    void getAll_shouldReturnEmptyList_whenNoUsersExist() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = userService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("update - should update and return user when user exists")
    void updateUser_shouldUpdateAndReturnUser_whenExists() {
        User updatedUser = User.builder()
                .username("updateduser")
                .email("updated@test.com")
                .name("Updated User")
                .password("newpassword")
                .build();
        UserEntity savedUser = UserEntity.builder()
                .id(testUserId)
                .username("updateduser")
                .email("updated@test.com")
                .name("Updated User")
                .password("newpassword")
                .build();

        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(userRepository.getReferenceById(testUserId)).thenReturn(testUserEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        User result = userService.update(testUserId, updatedUser);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        assertThat(result.getUsername()).isEqualTo("updateduser");
        verify(userRepository, times(1)).existsById(testUserId);
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("update - should throw UserNotFoundException when user does not exist")
    void updateUser_shouldThrowUserNotFoundException_whenDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        User updatedUser = User.builder()
                .username("updateduser")
                .email("updated@test.com")
                .name("Updated User")
                .password("newpassword")
                .build();

        when(userRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> userService.update(nonExistentId, updatedUser))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: " + nonExistentId);
        verify(userRepository, times(1)).existsById(nonExistentId);
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("update - should set ID on user entity before saving")
    void updateUser_shouldSetIdOnEntity_beforeSaving() {
        User updatedUser = User.builder()
                .username("updateduser")
                .email("updated@test.com")
                .name("Updated User")
                .password("newpassword")
                .build();

        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(userRepository.getReferenceById(testUserId)).thenReturn(testUserEntity);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.update(testUserId, updatedUser);

        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("delete - should delete user when user exists")
    void deleteUser_shouldDeleteUser_whenExists() {
        when(userRepository.existsById(testUserId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(testUserId);

        userService.delete(testUserId);

        verify(userRepository, times(1)).existsById(testUserId);
        verify(userRepository, times(1)).deleteById(testUserId);
    }

    @Test
    @DisplayName("delete - should throw UserNotFoundException when user does not exist")
    void deleteUser_shouldThrowUserNotFoundException_whenDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(nonExistentId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: " + nonExistentId);
        verify(userRepository, times(1)).existsById(nonExistentId);
        verify(userRepository, never()).deleteById(any(UUID.class));
    }
}
