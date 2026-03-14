package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.persistence.repository.UserRepository;

@Service
@RequiredArgsConstructor
class UserService {
    private final UserRepository userRepository;
}
