package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.enums.UserAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * TDD Test class for UserRepository.
 * Tests cover CRUD operations and JpaRepository default methods.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-11
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserRepository TDD Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private UserEntity user(String username, String password, String email) {
        return UserEntity.builder()
                .username(username)
                .password(password)
                .email(email)
                .authority(UserAuthority.USER)
                .build();
    }

    // ==================== CREATE/SAVE TESTS ====================

    @Test
    @DisplayName("Should save a new user and generate UUID")
    void testSaveUser() {
        // Given
        UserEntity user = user("testuser", "password123", "test@test.com");

        // When
        UserEntity savedUser = userRepository.saveAndFlush(user);

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getPassword()).isEqualTo("password123");
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should save multiple users")
    void testSaveMultipleUsers() {
        // Given
        UserEntity user1 = user("user1", "pass1", "user1@example.com");
        UserEntity user2 = user("user2", "pass2", "user2@example.com");
        UserEntity user3 = user("user3", "pass3", "user3@example.com");

        // When
        List<UserEntity> savedUsers = userRepository.saveAll(List.of(user1, user2, user3));

        // Then
        assertThat(savedUsers).hasSize(3);
        assertThat(savedUsers).extracting(UserEntity::getUsername)
                .containsExactlyInAnyOrder("user1", "user2", "user3");
    }

    @Test
    @DisplayName("Should generate unique UUID for each user")
    void testUniqueUUIDGeneration() {
        // Given
        UserEntity user1 = user("user1", "pass1", "user1@example.com");
        UserEntity user2 = user("user2", "pass2", "user2@example.com");

        // When
        UserEntity savedUser1 = userRepository.save(user1);
        UserEntity savedUser2 = userRepository.save(user2);

        // Then
        assertThat(savedUser1.getId()).isNotEqualTo(savedUser2.getId());
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find user by ID")
    void testFindById() {
        // Given
        UserEntity user = user("findme", "password", "test@test.com");
        UserEntity savedUser = userRepository.save(user);

        // When
        Optional<UserEntity> foundUser = userRepository.findById(savedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("findme");
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("Should find user by username")
    void testFindByUsername() {
        // Given
        UserEntity user = user("username-search", "password", "username-search@example.com");
        userRepository.save(user);

        // When
        Optional<UserEntity> foundUser = userRepository.findByUsername("username-search");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("username-search@example.com");
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        // Given
        UserEntity user = user("email-search", "password", "email-search@example.com");
        userRepository.save(user);

        // When
        Optional<UserEntity> foundUser = userRepository.findByEmail("email-search@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("email-search");
    }

    @Test
    @DisplayName("Should return empty when user ID not found")
    void testFindByIdNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<UserEntity> foundUser = userRepository.findById(nonExistentId);

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should find all users")
    void testFindAll() {
        // Given
        UserEntity user1 = user("user1", "pass1", "user1@example.com");
        UserEntity user2 = user("user2", "pass2", "user2@example.com");
        UserEntity user3 = user("user3", "pass3", "user3@example.com");
        userRepository.saveAll(List.of(user1, user2, user3));

        // When
        List<UserEntity> allUsers = userRepository.findAll();

        // Then
        assertThat(allUsers).hasSize(3);
    }

    @Test
    @DisplayName("Should check if user exists by ID")
    void testExistsById() {
        // Given
        UserEntity user = user("exists", "password", "exists@example.com");
        UserEntity savedUser = userRepository.save(user);

        // When
        boolean exists = userRepository.existsById(savedUser.getId());
        boolean notExists = userRepository.existsById(UUID.randomUUID());

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should count all users")
    void testCount() {
        // Given
        UserEntity user1 = user("user1", "pass1", "user1@example.com");
        UserEntity user2 = user("user2", "pass2", "user2@example.com");
        userRepository.saveAll(List.of(user1, user2));

        // When
        long count = userRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find all users by IDs")
    void testFindAllById() {
        // Given
        UserEntity user1 = user("user1", "pass1", "user1@example.com");
        UserEntity user2 = user("user2", "pass2", "user2@example.com");
        UserEntity user3 = user("user3", "pass3", "user3@example.com");
        UserEntity savedUser1 = userRepository.save(user1);
        UserEntity savedUser2 = userRepository.save(user2);
        userRepository.save(user3);

        // When
        List<UserEntity> users = userRepository.findAllById(List.of(savedUser1.getId(), savedUser2.getId()));

        // Then
        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserEntity::getUsername)
                .containsExactlyInAnyOrder("user1", "user2");
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update existing user")
    void testUpdateUser() {
        // Given
        UserEntity user = user("oldname", "oldpass", "oldname@example.com");
        UserEntity savedUser = userRepository.save(user);

        // When
        savedUser.setUsername("newname");
        savedUser.setPassword("newpass");
        savedUser.setEmail("newemail@example.com");
        UserEntity updatedUser = userRepository.save(savedUser);

        // Then
        assertThat(updatedUser.getId()).isEqualTo(savedUser.getId());
        assertThat(updatedUser.getUsername()).isEqualTo("newname");
        assertThat(updatedUser.getPassword()).isEqualTo("newpass");
        assertThat(updatedUser.getEmail()).isEqualTo("newemail@example.com");
    }

    @Test
    @DisplayName("Should update user and persist changes")
    void testUpdateUserPersisted() {
        // Given
        UserEntity user = user("original", "password", "original@example.com");
        UserEntity savedUser = userRepository.save(user);
        UUID userId = savedUser.getId();

        // When
        savedUser.setUsername("updated");
        userRepository.save(savedUser);
        userRepository.flush();

        // Then
        Optional<UserEntity> reloadedUser = userRepository.findById(userId);
        assertThat(reloadedUser).isPresent();
        assertThat(reloadedUser.get().getUsername()).isEqualTo("updated");
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete user by ID")
    void testDeleteById() {
        // Given
        UserEntity user = user("deleteme", "password", "deleteme@example.com");
        UserEntity savedUser = userRepository.save(user);

        // When
        userRepository.deleteById(savedUser.getId());

        // Then
        Optional<UserEntity> deletedUser = userRepository.findById(savedUser.getId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("Should delete user entity")
    void testDelete() {
        // Given
        UserEntity user = user("deleteme", "password", "deleteme@example.com");
        UserEntity savedUser = userRepository.save(user);

        // When
        userRepository.delete(savedUser);

        // Then
        Optional<UserEntity> deletedUser = userRepository.findById(savedUser.getId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("Should delete all users")
    void testDeleteAll() {
        // Given
        UserEntity user1 = user("user1", "pass1", "user1@example.com");
        UserEntity user2 = user("user2", "pass2", "user2@example.com");
        userRepository.saveAll(List.of(user1, user2));

        // When
        userRepository.deleteAll();

        // Then
        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("Should delete all users by IDs")
    void testDeleteAllById() {
        // Given
        UserEntity user1 = user("user1", "pass1", "user1@example.com");
        UserEntity user2 = user("user2", "pass2", "user2@example.com");
        UserEntity user3 = user("user3", "pass3", "user3@example.com");
        UserEntity savedUser1 = userRepository.save(user1);
        UserEntity savedUser2 = userRepository.save(user2);
        UserEntity savedUser3 = userRepository.save(user3);

        // When
        userRepository.deleteAllById(List.of(savedUser1.getId(), savedUser2.getId()));

        // Then
        assertThat(userRepository.count()).isEqualTo(1);
        Optional<UserEntity> remainingUser = userRepository.findById(savedUser3.getId());
        assertThat(remainingUser).isPresent();
    }

    // ==================== CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should enforce username uniqueness constraint")
    void testUsernameUniqueConstraint() {
        // Given
        UserEntity user1 = user("duplicate", "pass1", "duplicate@example.com");
        userRepository.save(user1);
        userRepository.flush();

        UserEntity user2 = user("duplicate", "pass2", "duplicate2@example.com");
        // When & Then
        try {
            userRepository.save(user2);
            userRepository.flush();
            fail("Should have thrown exception for duplicate username");
        } catch (Exception e) {
            // Expected exception due to unique constraint violation
            assertThat(e.getMessage()).containsAnyOf("unique", "constraint", "duplicate", "Unique");
        }
    }

    @Test
    @DisplayName("Should not allow null username")
    void testNullUsername() {
        // Given
        UserEntity user = UserEntity.builder()
                .username(null)
                .password("password")
                .email("nullusername@example.com")
                .authority(UserAuthority.USER)
                .build();

        // When & Then
        try {
            userRepository.save(user);
            userRepository.flush();
            fail("Should have thrown exception for null username");
        } catch (Exception e) {
            // Expected exception due to null constraint violation
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should not allow null password")
    void testNullPassword() {
        // Given
        UserEntity user = UserEntity.builder()
                .username("validuser")
                .password(null)
                .email("validuser@example.com")
                .authority(UserAuthority.USER)
                .build();

        // When & Then
        try {
            userRepository.save(user);
            userRepository.flush();
            fail("Should have thrown exception for null password");
        } catch (Exception e) {
            // Expected exception due to null constraint violation
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should not allow null email")
    void testNullEmail() {
        // Given
        UserEntity user = UserEntity.builder()
                .username("validuser")
                .password("password")
                .authority(UserAuthority.USER)
                .build();
        try {
            userRepository.save(user);
            userRepository.flush();
            fail("Should have thrown exception for null email");
        } catch (Exception e) {
            // Expected exception due to null constraint violation
            assertThat(e).isNotNull();
        }
    }

    // ==================== TIMESTAMP TESTS ====================

    @Test
    @DisplayName("Should automatically set createdAt timestamp on save")
    void testCreatedAtTimestamp() {
        // Given
        UserEntity user = user("timestamptest", "password", "timestamptest@example.com");

        // When
        UserEntity savedUser = userRepository.saveAndFlush(user);

        // Then
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not modify createdAt on update")
    void testCreatedAtImmutable() {
        // Given
        UserEntity user = user("immutable", "password", "immutable@example.com");
        UserEntity savedUser = userRepository.save(user);
        userRepository.flush();

        // When
        savedUser.setPassword("newpassword");
        UserEntity updatedUser = userRepository.save(savedUser);
        userRepository.flush();

        // Then
        assertThat(updatedUser.getCreatedAt()).isEqualTo(savedUser.getCreatedAt());
    }
}
