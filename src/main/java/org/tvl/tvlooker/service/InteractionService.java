package org.tvl.tvlooker.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.exception.InteractionNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.Review;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.entity.InteractionEntity;
import org.tvl.tvlooker.domain.model.mapper.ReviewEntityMapper;
import org.tvl.tvlooker.domain.model.mapper.InteractionEntityMapper;
import org.tvl.tvlooker.persistence.repository.InteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Interaction entity operations.
 */
@Service
@RequiredArgsConstructor
public class InteractionService {

    private static final String INTERACTION_NOT_FOUND_MESSAGE = "Interaction not found: ";

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
        Review review = interaction.getReviewId() == null ? null : reviewService.getById(interaction.getReviewId());

        InteractionEntity entity = InteractionEntityMapper.toEntity(interaction, user, item, review);
        return InteractionEntityMapper.toDomain(interactionRepository.save(entity));
    }

    public Interaction createForUser(Interaction interaction, UUID userId) {
        Interaction ownedInteraction = Interaction.builder()
                .userId(userId)
                .itemId(interaction.getItemId())
                .reviewId(interaction.getReviewId())
                .interactionType(interaction.getInteractionType())
                .createdAt(interaction.getCreatedAt())
                .build();
        return create(ownedInteraction);
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
                .orElseThrow(() -> new InteractionNotFoundException(INTERACTION_NOT_FOUND_MESSAGE + id));
    }

    public Interaction getByIdForUser(Long id, UUID userId) {
        return interactionRepository.findByIdAndUserId(id, userId)
                .map(InteractionEntityMapper::toDomain)
                .orElseThrow(() -> new InteractionNotFoundException(INTERACTION_NOT_FOUND_MESSAGE + id));
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
     * Get all interactions with pagination.
     *
     * @param pageable pagination information
     * @return page of interactions
     */
    public Page<Interaction> getAll(Pageable pageable) {
        return interactionRepository.findAll(pageable)
                .map(InteractionEntityMapper::toDomain);
    }

    public Page<Interaction> getByUserId(UUID userId, Pageable pageable) {
        return interactionRepository.findAllByUserId(userId, pageable)
                .map(InteractionEntityMapper::toDomain);
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
            throw new InteractionNotFoundException(INTERACTION_NOT_FOUND_MESSAGE + id);
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

    public Interaction updateForUser(Long id, Interaction interaction, UUID userId) {
        InteractionEntity actual = interactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new InteractionNotFoundException(INTERACTION_NOT_FOUND_MESSAGE + id));

        if (interaction.getReviewId() != null) {
            Review review = reviewService.getById(interaction.getReviewId());
            actual.setReview(ReviewEntityMapper.toEntity(review, null, null));
        }
        if (interaction.getInteractionType() != null) {
            actual.setInteractionType(interaction.getInteractionType());
        }

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
            throw new InteractionNotFoundException(INTERACTION_NOT_FOUND_MESSAGE + id);
        }
        interactionRepository.deleteById(id);
    }

    public void deleteForUser(Long id, UUID userId) {
        if (interactionRepository.findByIdAndUserId(id, userId).isEmpty()) {
            throw new InteractionNotFoundException(INTERACTION_NOT_FOUND_MESSAGE + id);
        }
        interactionRepository.deleteById(id);
    }
}
