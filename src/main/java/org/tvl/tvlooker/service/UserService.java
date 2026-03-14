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
     * Get user by ID
     *
     * @param id the ID of the user to retrieve
     * @return the user
     * @throws UserNotFoundException if no user is found with the given ID
     */
    public User getUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(() ->
                new UserNotFoundException("User not found with id: " + id));
    }

    /**
     * Get all users in the system
     *
     * @return list of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
