package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.model.User;
import org.tvl.tvlooker.persistence.mapper.UserEntityMapper;
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
    private final UserEntityMapper userMapper;

    /**
     * Create a new user.
     *
     * @param user user to persist
     * @return saved user
     */
    public User create(User user) {
        var entity = userMapper.toEntity(user);
        return userMapper.toDomain(userRepository.save(entity));
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
                .map(userMapper::toDomain)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    /**
     * Get all users.
     *
     * @return list of users
     */
    public List<User> getAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
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
        var entity = userMapper.toEntity(user);
        entity.setId(id);
        return userMapper.toDomain(userRepository.save(entity));
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
