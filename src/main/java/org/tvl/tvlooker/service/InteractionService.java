package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.model.entity.Interaction;
import org.tvl.tvlooker.persistence.repository.InteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Service for Interaction entity operations.
 */
@Service
@RequiredArgsConstructor
public class InteractionService {

    private final InteractionRepository interactionRepository;

    /**
     * Get all interactions in the system.
     * Used by recommendation engine for collaborative filtering and matrix factorization.
     *
     * @return list of all interactions
     */
    public List<Interaction> getAllInteractions() {
        return interactionRepository.findAll();
    }
}