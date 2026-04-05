package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.model.Interaction;
import org.tvl.tvlooker.domain.exception.InteractionNotFoundException;
import org.tvl.tvlooker.domain.model.Item;
import org.tvl.tvlooker.domain.model.Review;
import org.tvl.tvlooker.domain.model.User;
import org.tvl.tvlooker.domain.model.entity.InteractionEntity;
import org.tvl.tvlooker.domain.model.mapper.InteractionEntityMapper;
import org.tvl.tvlooker.persistence.repository.InteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Interaction entity operations.
 */
@Service
@RequiredArgsConstructor
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final ReviewService reviewService;
    private final UserService userService;
    private final ItemService itemService;

    /**
     * Create a new interaction.
     *
     * @param interaction interaction to persist
     * @return saved interaction
     */
    public Interaction create(Interaction interaction) {
        User user = userService.getById(interaction.getUserId());
        Item item = itemService.getById(interaction.getItemId());
        Review review = reviewService.getById(interaction.getReviewId());

        InteractionEntity entity = InteractionEntityMapper.toEntity(interaction, user, item, review);
        return InteractionEntityMapper.toDomain(interactionRepository.save(entity));
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
                .map(InteractionEntityMapper::toDomain)
                .orElseThrow(() -> new InteractionNotFoundException("Interaction not found: " + id));
    }

    /**
     * Get all interactions.
     *
     * @return list of interactions
     */
    public List<Interaction> getAll() {
        return interactionRepository.findAll().stream()
                .map(InteractionEntityMapper::toDomain)
                .collect(Collectors.toList());
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
        InteractionEntity actual = interactionRepository.getReferenceById(id);

        User user = userService.getById(interaction.getUserId());
        Item item = itemService.getById(interaction.getItemId());
        Review review = reviewService.getById(interaction.getReviewId());

        InteractionEntity update = InteractionEntityMapper.toEntity(interaction, user, item, review);

        if (update.getReview() != null) {
            actual.setReview(update.getReview());
        }
        if (update.getInteractionType() != null) {
            actual.setInteractionType(update.getInteractionType());}

        return InteractionEntityMapper.toDomain(interactionRepository.save(actual));
    }

    /**
     * Delete an interaction by id.
     *
     * @param id interaction id
     * @throws InteractionNotFoundException when the interaction does not exist
     */
    public void delete(Long id) {
        if (!interactionRepository.existsById(id)) {
            throw new InteractionNotFoundException("Interaction not found: " + id);
        }
        interactionRepository.deleteById(id);
    }
}
