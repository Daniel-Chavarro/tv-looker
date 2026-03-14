package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.motor.RecommendationEngine;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.List;
import java.util.UUID;

/**
 * Service for generating personalized recommendations for users.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final RecommendationEngine recommendationEngine;
    private final UserService userService;
    private final InteractionService interactionService;
    private final ItemService itemService;

    /**
     * Get personalized recommendations for a user.
     *
     * @param userId the ID of the user
     * @param limit limit maximum number of recommendations to return.
     * @return RecommendationResult containing recommendations and status.
     */
    @Transactional(readOnly=true)
    public List<Item> getUserRecommendations(UUID userId, int limit) {
        validateInput(userId, limit);

        User user = userService.getById(userId);

        RecommendationContext context = RecommendationContext.builder()
                .users(userService.getAll())
                .items(itemService.getAll())
                .interactions(interactionService.getAll())
                .build();

        List<ScoredItem> scoredItems = recommendationEngine.recommend(user, context);

        // Only returned items meanwhile we decide what to do with the scores and explanations
        return scoredItems.stream()
                .map(ScoredItem::getItem)
                .limit(limit)
                .toList();
    }

    /**
     * Helper method to validate input parameters for getUserRecommendations.
     *
     * @param userId the user ID
     * @param limit limit maximum number of recommendations to return.
     */
    private void validateInput(UUID userId, int limit) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
    }
}
