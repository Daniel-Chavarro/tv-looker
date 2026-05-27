package org.tvl.tvlooker.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.entity.InteractionEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.domain.model.entity.SavedRecommendationEntity;
import org.tvl.tvlooker.persistence.repository.InteractionRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.repository.SavedRecommendationRepository;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("RecommendationService Integration Tests")
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class RecommendationServiceIntegrationTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private SavedRecommendationRepository savedRecommendationRepository;

    @Autowired
    private EntityManager entityManager;

    private UserEntity testUser1Entity;
    private UserEntity testUser2Entity;
    private UserEntity newUserEntity;
    private ItemEntity popularItem1Entity;
    private ItemEntity popularItem2Entity;
    private ItemEntity popularItem3Entity;
    private ItemEntity lessPopularItemEntity;

    @BeforeEach
    void setUp() {
        savedRecommendationRepository.deleteAll();
        interactionRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();

        testUser1Entity = UserEntity.builder()
                .username("testuser1")
                .password("password123")
                .email("user1@test.com")
                .name("User 1")
                .build();
        testUser1Entity = userRepository.save(testUser1Entity);

        testUser2Entity = UserEntity.builder()
                .username("testuser2")
                .password("password123")
                .email("user2@test.com")
                .name("User 2")
                .build();
        testUser2Entity = userRepository.save(testUser2Entity);

        newUserEntity = UserEntity.builder()
                .username("newuser")
                .password("password123")
                .email("new@test.com")
                .name("New User")
                .build();
        newUserEntity = userRepository.save(newUserEntity);

        popularItem1Entity = ItemEntity.builder()
                .tmdbId(1L)
                .tmdbType(TmdbType.MOVIE)
                .title("The Matrix")
                .overview("A computer hacker learns about the true nature of reality.")
                .releaseDate(LocalDate.of(1999, 3, 31))
                .popularity(BigDecimal.valueOf(950.5678))
                .voteAverage(BigDecimal.valueOf(8.7))
                .build();
        popularItem1Entity = itemRepository.save(popularItem1Entity);

        popularItem2Entity = ItemEntity.builder()
                .tmdbId(2L)
                .tmdbType(TmdbType.MOVIE)
                .title("Inception")
                .overview("A thief who steals corporate secrets through dream-sharing technology.")
                .releaseDate(LocalDate.of(2010, 7, 16))
                .popularity(BigDecimal.valueOf(920.1234))
                .voteAverage(BigDecimal.valueOf(8.8))
                .build();
        popularItem2Entity = itemRepository.save(popularItem2Entity);

        popularItem3Entity = ItemEntity.builder()
                .tmdbId(3L)
                .tmdbType(TmdbType.TV)
                .title("Breaking Bad")
                .overview("A chemistry teacher turned methamphetamine manufacturer.")
                .releaseDate(LocalDate.of(2008, 1, 20))
                .popularity(BigDecimal.valueOf(900.0))
                .voteAverage(BigDecimal.valueOf(9.5))
                .build();
        popularItem3Entity = itemRepository.save(popularItem3Entity);

        lessPopularItemEntity = ItemEntity.builder()
                .tmdbId(4L)
                .tmdbType(TmdbType.MOVIE)
                .title("Indie Film")
                .overview("A lesser-known independent film.")
                .releaseDate(LocalDate.of(2020, 5, 1))
                .popularity(BigDecimal.valueOf(50.0))
                .voteAverage(BigDecimal.valueOf(7.0))
                .build();
        lessPopularItemEntity = itemRepository.save(lessPopularItemEntity);

        InteractionEntity interaction1 = InteractionEntity.builder()
                .id(1L)
                .user(testUser1Entity)
                .item(popularItem1Entity)
                .interactionType(InteractionType.VIEW)
                .build();
        interactionRepository.save(interaction1);

        InteractionEntity interaction2 = InteractionEntity.builder()
                .id(2L)
                .user(testUser1Entity)
                .item(popularItem2Entity)
                .interactionType(InteractionType.RATING)
                .build();
        interactionRepository.save(interaction2);

        InteractionEntity interaction3 = InteractionEntity.builder()
                .id(3L)
                .user(testUser2Entity)
                .item(popularItem1Entity)
                .interactionType(InteractionType.VIEW)
                .build();
        interactionRepository.save(interaction3);

        InteractionEntity interaction4 = InteractionEntity.builder()
                .id(4L)
                .user(testUser2Entity)
                .item(lessPopularItemEntity)
                .interactionType(InteractionType.VIEW)
                .build();
        interactionRepository.save(interaction4);
    }

    @AfterEach
    void tearDown() {
        savedRecommendationRepository.deleteAll();
        interactionRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return personalized recommendations for user with interaction history")
    void testGetRecommendations_UserWithInteractions_ReturnsRecommendations() {
        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), 5);

        assertThat(recommendations).isNotNull();
        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).hasSizeLessThanOrEqualTo(5);

        for (Item item : recommendations) {
            assertThat(itemRepository.findById(item.getId())).isPresent();
        }
    }

    @Test
    @DisplayName("Should return popularity-based recommendations for new user without interactions")
    void testGetRecommendations_NewUserNoInteractions_ReturnsPopularityBasedRecommendations() {
        List<Item> recommendations = recommendationService.getUserRecommendations(newUserEntity.getId(), 3);

        assertThat(recommendations).isNotNull();
        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).hasSizeLessThanOrEqualTo(3);

        for (Item item : recommendations) {
            assertThat(itemRepository.findById(item.getId())).isPresent();
        }
    }

    @Test
    @DisplayName("Should respect limit parameter and return correct number of recommendations")
    void testGetRecommendations_LimitParameter_ReturnsCorrectNumberOfItems() {
        int limit = 2;

        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), limit);

        assertThat(recommendations).hasSizeLessThanOrEqualTo(limit);
    }

    @Test
    @DisplayName("Should return items that exist in database")
    void testGetRecommendations_VerifyItemsExistInDatabase() {
        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), 5);

        assertThat(recommendations).isNotNull();
        for (Item item : recommendations) {
            ItemEntity dbItem = itemRepository.findById(item.getId()).orElse(null);
            assertThat(dbItem).isNotNull();
            assertThat(dbItem.getId()).isEqualTo(item.getId());
            assertThat(dbItem.getTitle()).isEqualTo(item.getTitle());
        }
    }

    @Test
    @DisplayName("Should handle multiple users with different interaction patterns")
    void testGetRecommendations_MultipleUsers_ReturnsPersonalizedResults() {
        List<Item> recommendationsUser1 = recommendationService.getUserRecommendations(testUser1Entity.getId(), 3);
        List<Item> recommendationsUser2 = recommendationService.getUserRecommendations(testUser2Entity.getId(), 3);

        assertThat(recommendationsUser1).isNotNull();
        assertThat(recommendationsUser2).isNotNull();
        assertThat(recommendationsUser1).isNotEmpty();
        assertThat(recommendationsUser2).isNotEmpty();
    }

    @Test
    @DisplayName("Should throw UserNotFoundException for non-existent user")
    void testGetRecommendations_NonExistentUser_ThrowsUserNotFoundException() {
        UUID nonExistentUserId = UUID.randomUUID();

        assertThatThrownBy(() -> recommendationService.getUserRecommendations(nonExistentUserId, 5))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: " + nonExistentUserId);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null user ID")
    void testGetRecommendations_NullUserId_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(null, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for zero limit")
    void testGetRecommendations_ZeroLimit_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(testUser1Entity.getId(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be greater than 0");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative limit")
    void testGetRecommendations_NegativeLimit_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(testUser1Entity.getId(), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be greater than 0");
    }

    @Test
    @DisplayName("Should handle limit larger than available items")
    void testGetRecommendations_LimitLargerThanAvailableItems_ReturnsAllAvailableItems() {
        int largeLimit = 1000;

        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), largeLimit);

        assertThat(recommendations).isNotNull();
        assertThat(recommendations.size()).isLessThanOrEqualTo(4);
    }

    @Test
    @DisplayName("Should work correctly with limit of 1")
    void testGetRecommendations_LimitOne_ReturnsSingleItem() {
        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), 1);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0)).isNotNull();
    }

    @Test
    @DisplayName("Should persist saved recommendation rows on first request")
    void testGetRecommendations_FirstRequest_PersistsSavedRows() {
        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), 3);

        List<SavedRecommendationEntity> savedRows = savedRecommendationRepository
                .findFreshByUserIdOrderByRankPositionAsc(testUser1Entity.getId(), Instant.now());
        assertThat(savedRows).isNotEmpty();
        assertThat(savedRows).hasSizeGreaterThanOrEqualTo(recommendations.size());
        assertThat(savedRows).extracting(SavedRecommendationEntity::getRankPosition)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, savedRows.size()).boxed().toList());
    }

    @Test
    @DisplayName("Should reuse saved recommendation rows before TTL expiry")
    void testGetRecommendations_SecondRequest_ReusesSavedRows() {
        List<Item> firstRecommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), 3);
        List<Long> savedIds = savedRecommendationRepository
                .findFreshByUserIdOrderByRankPositionAsc(testUser1Entity.getId(), Instant.now())
                .stream()
                .map(SavedRecommendationEntity::getId)
                .toList();

        assertThat(firstRecommendations).isNotEmpty();
        assertThat(savedIds).isNotEmpty();

        List<Item> secondRecommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), 3);
        List<Long> savedIdsAfterSecondRequest = savedRecommendationRepository
                .findFreshByUserIdOrderByRankPositionAsc(testUser1Entity.getId(), Instant.now())
                .stream()
                .map(SavedRecommendationEntity::getId)
                .toList();

        assertThat(secondRecommendations).extracting(Item::getId)
                .containsExactlyElementsOf(firstRecommendations.stream().map(Item::getId).toList());
        assertThat(savedIdsAfterSecondRequest).containsExactlyElementsOf(savedIds);
    }

    @Test
    @DisplayName("Should replace expired saved recommendation rows")
    void testGetRecommendations_ExpiredRows_ReplacesSavedRows() {
        recommendationService.getUserRecommendations(testUser1Entity.getId(), 3);
        List<SavedRecommendationEntity> savedRows = savedRecommendationRepository
                .findFreshByUserIdOrderByRankPositionAsc(testUser1Entity.getId(), Instant.now());
        assertThat(savedRows).isNotEmpty();
        savedRows.forEach(row -> row.setExpiresAt(Instant.now().minusSeconds(1)));
        savedRecommendationRepository.saveAll(savedRows);
        List<Long> expiredIds = savedRows.stream().map(SavedRecommendationEntity::getId).toList();
        entityManager.flush();
        entityManager.clear();

        recommendationService.getUserRecommendations(testUser1Entity.getId(), 3);

        List<Long> freshIds = savedRecommendationRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(testUser1Entity.getId()))
                .map(SavedRecommendationEntity::getId)
                .toList();
        assertThat(freshIds).isNotEmpty();
        assertThat(freshIds).doesNotContainAnyElementsOf(expiredIds);
    }

    @Test
    @DisplayName("Should return cached items ordered by rank position")
    void testGetRecommendations_CacheHit_ReturnsRankOrder() {
        savedRecommendationRepository.deleteAllByUserId(testUser1Entity.getId());
        Instant now = Instant.now();
        savedRecommendationRepository.saveAll(List.of(
                SavedRecommendationEntity.builder()
                        .user(testUser1Entity)
                        .item(popularItem2Entity)
                        .score(0.9)
                        .rankPosition(1)
                        .createdAt(now)
                        .expiresAt(now.plusSeconds(3600))
                        .build(),
                SavedRecommendationEntity.builder()
                        .user(testUser1Entity)
                        .item(popularItem3Entity)
                        .score(0.6)
                        .rankPosition(0)
                        .createdAt(now)
                        .expiresAt(now.plusSeconds(3600))
                        .build()
        ));
        entityManager.flush();
        entityManager.clear();

        List<Item> recommendations = recommendationService.getUserRecommendations(testUser1Entity.getId(), 2);

        assertThat(recommendations).extracting(Item::getId)
                .containsExactly(popularItem3Entity.getId(), popularItem2Entity.getId());
    }
}
