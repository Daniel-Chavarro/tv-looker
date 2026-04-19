package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.mapper.UserEntityMapper;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for User entity operations.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Create a new user.
     *
     * @param user user to persist
     * @return saved user
     */
    public User create(User user) {
        UserEntity entity = UserEntityMapper.toEntity(user);
        return UserEntityMapper.toDomain(userRepository.save(entity));
    }

    /**
     * Get user by ID.
     *
     * @param id user id
     * @return user
     * @throws UserNotFoundException when the user does not exist
     */
    public User getById(UUID id) {
        return userRepository.findById(id)
                .map(UserEntityMapper::toDomain)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    /**
     * Get all users.
     *
     * @return list of users
     */
    public List<User> getAll() {
        return userRepository.findAll().stream()
                .map(UserEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get all users with pagination.
     *
     * @param pageable pagination information
     * @return page of users
     */
    public Page<User> getAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserEntityMapper::toDomain);
    }

    /**
     * Update a user.
     *
     * @param id user id
     * @param user user data to update
     * @return updated user
     * @throws UserNotFoundException when the user does not exist
     */
    public User update(UUID id, User user) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        UserEntity actual = userRepository.getReferenceById(id);
        UserEntity update = UserEntityMapper.toEntity(user);

        if (update.getEmail() != null) {
            actual.setEmail(update.getEmail());
        }
        if (update.getName() != null) {
            actual.setName(update.getName());
        }
        if (update.getPassword() != null) {
            actual.setPassword(update.getPassword());
        }
        if (update.getUsername() != null) {
            actual.setUsername(update.getUsername());
        }

        return UserEntityMapper.toDomain(userRepository.save(actual));
    }

    /**
     * Delete a user by ID.
     *
     * @param id user id
     * @throws UserNotFoundException when the user does not exist
     */
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
