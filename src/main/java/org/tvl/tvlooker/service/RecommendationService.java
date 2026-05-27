package org.tvl.tvlooker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.SavedRecommendationEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.mapper.ItemEntityMapper;
import org.tvl.tvlooker.domain.motor.RecommendationEngine;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.repository.SavedRecommendationRepository;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Service for generating personalized recommendations for users.
 */
@Service
public class RecommendationService {
    private final RecommendationEngine recommendationEngine;
    private final UserService userService;
    private final InteractionService interactionService;
    private final ItemService itemService;
    private final ReviewService reviewService;
    private final SavedRecommendationRepository savedRecommendationRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final Duration cacheTtl;
    private final int cacheGeneratedSize;

    public RecommendationService(
            RecommendationEngine recommendationEngine,
            UserService userService,
            InteractionService interactionService,
            ItemService itemService,
            ReviewService reviewService,
            SavedRecommendationRepository savedRecommendationRepository,
            UserRepository userRepository,
            ItemRepository itemRepository,
            @Value("${recommendation.cache.ttl}") Duration cacheTtl,
            @Value("${recommendation.cache.generated-size}") int cacheGeneratedSize) {
        this.recommendationEngine = recommendationEngine;
        this.userService = userService;
        this.interactionService = interactionService;
        this.itemService = itemService;
        this.reviewService = reviewService;
        this.savedRecommendationRepository = savedRecommendationRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.cacheTtl = cacheTtl;
        this.cacheGeneratedSize = cacheGeneratedSize;
    }

    /**
     * Get personalized recommendations for a user.
     *
     * @param userId the ID of the user
     * @param limit limit maximum number of recommendations to return.
     * @return RecommendationResult containing recommendations and status.
     */
    @Transactional
    public List<Item> getUserRecommendations(UUID userId, int limit) {
        validateInput(userId, limit);

        User user = userService.getById(userId);

        List<SavedRecommendationEntity> cachedRecommendations = savedRecommendationRepository
                .findFreshByUserIdOrderByRankPositionAsc(userId, Instant.now());
        if (!cachedRecommendations.isEmpty()) {
            return cachedRecommendations.stream()
                    .map(SavedRecommendationEntity::getItem)
                    .map(ItemEntityMapper::toDomain)
                    .limit(limit)
                    .toList();
        }

        RecommendationContext context = RecommendationContext.builder()
                .users(userService.getAll())
                .items(itemService.getAll())
                .interactions(interactionService.getAll())
                .reviews(reviewService.getAll())
                .build();

        List<ScoredItem> scoredItems = recommendationEngine.recommend(user, context);

        saveRecommendations(userId, scoredItems);

        // Only returned items meanwhile we decide what to do with the scores and explanations
        return scoredItems.stream()
                .map(ScoredItem::getItem)
                .limit(limit)
                .toList();
    }

    private void saveRecommendations(UUID userId, List<ScoredItem> scoredItems) {
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(cacheTtl);
        UserEntity user = userRepository.getReferenceById(userId);

        int savedCount = Math.min(cacheGeneratedSize, scoredItems.size());
        List<SavedRecommendationEntity> savedRecommendations = IntStream.range(0, savedCount)
                .mapToObj(index -> toSavedRecommendation(user, scoredItems.get(index), index, createdAt, expiresAt))
                .toList();

        savedRecommendationRepository.deleteAllByUserId(userId);
        savedRecommendationRepository.saveAll(savedRecommendations);
    }

    private SavedRecommendationEntity toSavedRecommendation(
            UserEntity user,
            ScoredItem scoredItem,
            int rankPosition,
            Instant createdAt,
            Instant expiresAt) {
        ItemEntity item = itemRepository.getReferenceById(scoredItem.getItem().getId());
        return SavedRecommendationEntity.builder()
                .user(user)
                .item(item)
                .score(scoredItem.getScore())
                .explanation(scoredItem.getExplanation())
                .sourceStrategy(scoredItem.getSourceStrategy())
                .rankPosition(rankPosition)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
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
