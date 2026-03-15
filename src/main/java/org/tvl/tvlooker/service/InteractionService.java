package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.model.entity.Interaction;
import org.tvl.tvlooker.domain.exception.InteractionNotFoundException;
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
     * Create a new interaction.
     *
     * @param interaction interaction to persist
     * @return saved interaction
     */
    public Interaction create(Interaction interaction) {
        return interactionRepository.save(interaction);
    }

    /**
     * Get an interaction by id.
     *
     * @param id interaction id
     * @return interaction
     * @throws InteractionNotFoundException when the interaction does not exist
     */
    public Interaction getById(Long id) {
        return interactionRepository.findById(id)
                .orElseThrow(() -> new InteractionNotFoundException("Interaction not found: " + id));
    }

    /**
     * Get all interactions.
     *
     * @return list of interactions
     */
    public List<Interaction> getAll() {
        return interactionRepository.findAll();
    }

    /**
     * Update an interaction.
     *
     * @param id interaction id
     * @param interaction interaction data to update
     * @return updated interaction
     * @throws InteractionNotFoundException when the interaction does not exist
     */
    public Interaction update(Long id, Interaction interaction) {
        if (!interactionRepository.existsById(id)) {
            throw new InteractionNotFoundException("Interaction not found: " + id);
        }
        interaction.setId(id);
        return interactionRepository.save(interaction);
    }

    /**
     * Delete an interaction by id.
     *
     * @param id interaction id
     * @throws InteractionNotFoundException when the interaction does not exist
     */
    public void deleteById(Long id) {
        if (!interactionRepository.existsById(id)) {
            throw new InteractionNotFoundException("Interaction not found: " + id);
        }
        interactionRepository.deleteById(id);
    }
}