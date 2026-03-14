package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for User entity operations.
 */
@Service
@RequiredArgsConstructor
class UserService {

    private final UserRepository userRepository;

    /**
     * Create a new user.
     *
     * @param user user to persist
     * @return saved user
     */
    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Get user by ID.
     *
     * @param id user id
     * @return user
     */
    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() ->
                new UserNotFoundException("User not found with id: " + id));
    }

    /**
     * Get all users.
     *
     * @return list of users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Update a user.
     *
     * @param id user id
     * @param user user data to update
     * @return updated user
     * @throws UserNotFoundException when the user does not exist
     */
    public User updateUser(UUID id, User user) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        user.setId(id);
        return userRepository.save(user);
    }

    /**
     * Delete a user by ID.
     *
     * @param id user id
     * @throws UserNotFoundException when the user does not exist
     */
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
