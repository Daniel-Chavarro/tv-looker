package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.User;

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

    // ==================== CREATE/SAVE TESTS ====================

    @Test
    @DisplayName("Should save a new user and generate UUID")
    void testSaveUser() {
        // Given
        User user = User.builder()
                .username("testuser")
                .password("password123")
                .build();

        // When
        User savedUser = userRepository.saveAndFlush(user);

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
        User user1 = User.builder().username("user1").password("pass1").build();
        User user2 = User.builder().username("user2").password("pass2").build();
        User user3 = User.builder().username("user3").password("pass3").build();

        // When
        List<User> savedUsers = userRepository.saveAll(List.of(user1, user2, user3));

        // Then
        assertThat(savedUsers).hasSize(3);
        assertThat(savedUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("user1", "user2", "user3");
    }

    @Test
    @DisplayName("Should generate unique UUID for each user")
    void testUniqueUUIDGeneration() {
        // Given
        User user1 = User.builder().username("user1").password("pass1").build();
        User user2 = User.builder().username("user2").password("pass2").build();

        // When
        User savedUser1 = userRepository.save(user1);
        User savedUser2 = userRepository.save(user2);

        // Then
        assertThat(savedUser1.getId()).isNotEqualTo(savedUser2.getId());
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find user by ID")
    void testFindById() {
        // Given
        User user = User.builder().username("findme").password("password").build();
        User savedUser = userRepository.save(user);

        // When
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("findme");
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("Should return empty when user ID not found")
    void testFindByIdNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<User> foundUser = userRepository.findById(nonExistentId);

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should find all users")
    void testFindAll() {
        // Given
        User user1 = User.builder().username("user1").password("pass1").build();
        User user2 = User.builder().username("user2").password("pass2").build();
        User user3 = User.builder().username("user3").password("pass3").build();
        userRepository.saveAll(List.of(user1, user2, user3));

        // When
        List<User> allUsers = userRepository.findAll();

        // Then
        assertThat(allUsers).hasSize(3);
    }

    @Test
    @DisplayName("Should check if user exists by ID")
    void testExistsById() {
        // Given
        User user = User.builder().username("exists").password("password").build();
        User savedUser = userRepository.save(user);

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
        User user1 = User.builder().username("user1").password("pass1").build();
        User user2 = User.builder().username("user2").password("pass2").build();
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
        User user1 = User.builder().username("user1").password("pass1").build();
        User user2 = User.builder().username("user2").password("pass2").build();
        User user3 = User.builder().username("user3").password("pass3").build();
        User savedUser1 = userRepository.save(user1);
        User savedUser2 = userRepository.save(user2);
        userRepository.save(user3);

        // When
        List<User> users = userRepository.findAllById(List.of(savedUser1.getId(), savedUser2.getId()));

        // Then
        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername)
                .containsExactlyInAnyOrder("user1", "user2");
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update existing user")
    void testUpdateUser() {
        // Given
        User user = User.builder().username("oldname").password("oldpass").build();
        User savedUser = userRepository.save(user);

        // When
        savedUser.setUsername("newname");
        savedUser.setPassword("newpass");
        User updatedUser = userRepository.save(savedUser);

        // Then
        assertThat(updatedUser.getId()).isEqualTo(savedUser.getId());
        assertThat(updatedUser.getUsername()).isEqualTo("newname");
        assertThat(updatedUser.getPassword()).isEqualTo("newpass");
    }

    @Test
    @DisplayName("Should update user and persist changes")
    void testUpdateUserPersisted() {
        // Given
        User user = User.builder().username("original").password("password").build();
        User savedUser = userRepository.save(user);
        UUID userId = savedUser.getId();

        // When
        savedUser.setUsername("updated");
        userRepository.save(savedUser);
        userRepository.flush();

        // Then
        Optional<User> reloadedUser = userRepository.findById(userId);
        assertThat(reloadedUser).isPresent();
        assertThat(reloadedUser.get().getUsername()).isEqualTo("updated");
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete user by ID")
    void testDeleteById() {
        // Given
        User user = User.builder().username("deleteme").password("password").build();
        User savedUser = userRepository.save(user);

        // When
        userRepository.deleteById(savedUser.getId());

        // Then
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("Should delete user entity")
    void testDelete() {
        // Given
        User user = User.builder().username("deleteme").password("password").build();
        User savedUser = userRepository.save(user);

        // When
        userRepository.delete(savedUser);

        // Then
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("Should delete all users")
    void testDeleteAll() {
        // Given
        User user1 = User.builder().username("user1").password("pass1").build();
        User user2 = User.builder().username("user2").password("pass2").build();
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
        User user1 = User.builder().username("user1").password("pass1").build();
        User user2 = User.builder().username("user2").password("pass2").build();
        User user3 = User.builder().username("user3").password("pass3").build();
        User savedUser1 = userRepository.save(user1);
        User savedUser2 = userRepository.save(user2);
        User savedUser3 = userRepository.save(user3);

        // When
        userRepository.deleteAllById(List.of(savedUser1.getId(), savedUser2.getId()));

        // Then
        assertThat(userRepository.count()).isEqualTo(1);
        Optional<User> remainingUser = userRepository.findById(savedUser3.getId());
        assertThat(remainingUser).isPresent();
    }

    // ==================== CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should enforce username uniqueness constraint")
    void testUsernameUniqueConstraint() {
        // Given
        User user1 = User.builder().username("duplicate").password("pass1").build();
        userRepository.save(user1);
        userRepository.flush();

        User user2 = User.builder().username("duplicate").password("pass2").build();

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
        User user = User.builder().username(null).password("password").build();

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
        User user = User.builder().username("validuser").password(null).build();

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

    // ==================== TIMESTAMP TESTS ====================

    @Test
    @DisplayName("Should automatically set createdAt timestamp on save")
    void testCreatedAtTimestamp() {
        // Given
        User user = User.builder().username("timestamptest").password("password").build();

        // When
        User savedUser = userRepository.saveAndFlush(user);

        // Then
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not modify createdAt on update")
    void testCreatedAtImmutable() {
        // Given
        User user = User.builder().username("immutable").password("password").build();
        User savedUser = userRepository.save(user);
        userRepository.flush();

        // When
        savedUser.setPassword("newpassword");
        User updatedUser = userRepository.save(savedUser);
        userRepository.flush();

        // Then
        assertThat(updatedUser.getCreatedAt()).isEqualTo(savedUser.getCreatedAt());
    }
}
