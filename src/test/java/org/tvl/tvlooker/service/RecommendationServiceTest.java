package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.exception.InsufficientDataException;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.SavedRecommendationEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.domain.motor.RecommendationEngine;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.repository.SavedRecommendationRepository;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService Unit Tests")
class RecommendationServiceTest {

    @Mock
    private RecommendationEngine recommendationEngine;

    @Mock
    private UserService userService;

    @Mock
    private ItemService itemService;

    @Mock
    private InteractionService interactionService;

    @Mock
    private ReviewService reviewService;

    @Mock
    private SavedRecommendationRepository savedRecommendationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @Captor
    private ArgumentCaptor<RecommendationContext> contextCaptor;

    @Captor
    private ArgumentCaptor<List<SavedRecommendationEntity>> savedRecommendationsCaptor;

    private RecommendationService recommendationService;
    private UUID testUserId;
    private User testUser;
    private List<Item> items;
    private List<Interaction> interactions;
    private List<ScoredItem> scoredRecommendations;
    private Duration cacheTtl;
    private UserEntity testUserEntity;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .password("password123")
                .email("test@test.com")
                .name("Test User")
                .build();

        Item item1 = item(1L, "Movie 1", 8.5);
        Item item2 = item(2L, "Movie 2", 7.5);
        Item item3 = item(3L, "Movie 3", 9.0);
        items = List.of(item1, item2, item3);

        Interaction interaction = Interaction.builder()
                .id(1L)
                .userId(testUserId)
                .itemId(1L)
                .interactionType(InteractionType.RATING)
                .build();
        interactions = List.of(interaction);

        scoredRecommendations = List.of(
                ScoredItem.builder().item(item2).score(0.95).explanation("Highly recommended").build(),
                ScoredItem.builder().item(item3).score(0.85).explanation("Popular choice").build(),
                ScoredItem.builder().item(item1).score(0.75).explanation("Based on similar users").build()
        );

        cacheTtl = Duration.ofHours(24);
        testUserEntity = UserEntity.builder()
                .id(testUserId)
                .username("testuser")
                .password("password123")
                .email("test@test.com")
                .name("Test User")
                .build();

        recommendationService = new RecommendationService(
                recommendationEngine,
                userService,
                interactionService,
                itemService,
                reviewService,
                savedRecommendationRepository,
                userRepository,
                itemRepository,
                cacheTtl,
                100);
    }

    @Test
    @DisplayName("getUserRecommendations - should return items when user exists and has recommendations")
    void getUserRecommendations_shouldReturnItems_whenUserExistsAndHasRecommendations() {
        givenCacheMissAndContextData();
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenReturn(scoredRecommendations);
        givenItemReferences();

        List<Item> result = recommendationService.getUserRecommendations(testUserId, 5);

        assertThat(result).extracting(Item::getTitle).containsExactly("Movie 2", "Movie 3", "Movie 1");
        verify(userService).getById(testUserId);
    }

    @Test
    @DisplayName("getUserRecommendations - should build context from domain services")
    void getUserRecommendations_shouldBuildContextFromDomainServices() {
        givenCacheMissAndContextData();
        when(recommendationEngine.recommend(eq(testUser), contextCaptor.capture())).thenReturn(scoredRecommendations);
        givenItemReferences();

        recommendationService.getUserRecommendations(testUserId, 5);

        RecommendationContext context = contextCaptor.getValue();
        assertThat(context.getUsers()).containsExactly(testUser);
        assertThat(context.getItems()).containsExactlyElementsOf(items);
        assertThat(context.getInteractions()).containsExactlyElementsOf(interactions);
        assertThat(context.getReviews()).isEmpty();
        assertThat(context.getDataProviders()).isNotNull().isEmpty();
        assertThat(context.getDataCache()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("getUserRecommendations - should limit results based on limit parameter")
    void getUserRecommendations_shouldLimitResults_basedOnLimitParameter() {
        givenCacheMissAndContextData();
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenReturn(scoredRecommendations);
        givenItemReferences();

        List<Item> result = recommendationService.getUserRecommendations(testUserId, 2);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getUserRecommendations - should throw IllegalArgumentException when limit is zero")
    void getUserRecommendations_shouldThrowIllegalArgumentException_whenLimitIsZero() {
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(testUserId, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be greater than 0");

        verifyNoInteractions(savedRecommendationRepository, recommendationEngine);
    }

    @Test
    @DisplayName("getUserRecommendations - should propagate UserNotFoundException when user not found")
    void getUserRecommendations_shouldPropagateUserNotFoundException_whenUserNotFound() {
        UUID nonExistentUserId = UUID.randomUUID();
        when(userService.getById(nonExistentUserId))
                .thenThrow(new UserNotFoundException("User not found with id: " + nonExistentUserId));

        assertThatThrownBy(() -> recommendationService.getUserRecommendations(nonExistentUserId, 5))
                .isInstanceOf(UserNotFoundException.class);

        verify(userService).getById(nonExistentUserId);
        verifyNoInteractions(savedRecommendationRepository, recommendationEngine);
    }

    @Test
    @DisplayName("getUserRecommendations - should propagate InsufficientDataException from engine")
    void getUserRecommendations_shouldPropagateInsufficientDataException_fromEngine() {
        givenCacheMissAndContextData();
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenThrow(new InsufficientDataException("Insufficient data to generate recommendations"));

        assertThatThrownBy(() -> recommendationService.getUserRecommendations(testUserId, 5))
                .isInstanceOf(InsufficientDataException.class);

        verify(savedRecommendationRepository, never()).deleteAllByUserId(any());
        verify(savedRecommendationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("getUserRecommendations - should save generated recommendations on cache miss")
    void getUserRecommendations_shouldSaveGeneratedRecommendations_onCacheMiss() {
        givenCacheMissAndContextData();
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenReturn(scoredRecommendations);
        givenItemReferences();

        recommendationService.getUserRecommendations(testUserId, 2);

        verify(savedRecommendationRepository).deleteAllByUserId(testUserId);
        verify(savedRecommendationRepository).saveAll(savedRecommendationsCaptor.capture());
        List<SavedRecommendationEntity> savedRows = savedRecommendationsCaptor.getValue();
        assertThat(savedRows).hasSize(3);
        assertThat(savedRows).extracting(SavedRecommendationEntity::getRankPosition).containsExactly(0, 1, 2);
        assertThat(savedRows).extracting(SavedRecommendationEntity::getScore).containsExactly(0.95, 0.85, 0.75);
        assertThat(savedRows).allSatisfy(row -> {
            assertThat(row.getExpiresAt()).isAfter(row.getCreatedAt());
            assertThat(row.getExpiresAt()).isEqualTo(row.getCreatedAt().plus(cacheTtl));
        });
    }

    @Test
    @DisplayName("getUserRecommendations - should return saved recommendations on cache hit")
    void getUserRecommendations_shouldReturnSavedRecommendations_onCacheHit() {
        SavedRecommendationEntity savedRow = SavedRecommendationEntity.builder()
                .user(testUserEntity)
                .item(itemEntity(3L, "Movie 3"))
                .score(0.85)
                .explanation("Popular choice")
                .sourceStrategy("popularity")
                .rankPosition(0)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(cacheTtl))
                .build();
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(savedRecommendationRepository.findFreshByUserIdOrderByRankPositionAsc(eq(testUserId), any(Instant.class)))
                .thenReturn(List.of(savedRow));

        List<Item> result = recommendationService.getUserRecommendations(testUserId, 5);

        assertThat(result).extracting(Item::getTitle).containsExactly("Movie 3");
        verify(recommendationEngine, never()).recommend(any(), any());
        verify(savedRecommendationRepository, never()).deleteAllByUserId(any());
        verify(savedRecommendationRepository, never()).saveAll(any());
        verifyNoInteractions(itemService, interactionService, reviewService, userRepository, itemRepository);
    }

    @Test
    @DisplayName("getUserRecommendations - should limit saved recommendations on cache hit")
    void getUserRecommendations_shouldLimitSavedRecommendations_onCacheHit() {
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(savedRecommendationRepository.findFreshByUserIdOrderByRankPositionAsc(eq(testUserId), any(Instant.class)))
                .thenReturn(List.of(
                        SavedRecommendationEntity.builder().item(itemEntity(2L, "Movie 2")).rankPosition(0).build(),
                        SavedRecommendationEntity.builder().item(itemEntity(3L, "Movie 3")).rankPosition(1).build()
                ));

        List<Item> result = recommendationService.getUserRecommendations(testUserId, 1);

        assertThat(result).extracting(Item::getTitle).containsExactly("Movie 2");
        verify(recommendationEngine, never()).recommend(any(), any());
    }

    private void givenCacheMissAndContextData() {
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(savedRecommendationRepository.findFreshByUserIdOrderByRankPositionAsc(eq(testUserId), any(Instant.class)))
                .thenReturn(List.of());
        when(userService.getAll()).thenReturn(List.of(testUser));
        when(itemService.getAll()).thenReturn(items);
        when(interactionService.getAll()).thenReturn(interactions);
        when(reviewService.getAll()).thenReturn(List.of());
    }

    private void givenItemReferences() {
        when(userRepository.getReferenceById(testUserId)).thenReturn(testUserEntity);
        when(itemRepository.getReferenceById(2L)).thenReturn(itemEntity(2L, "Movie 2"));
        when(itemRepository.getReferenceById(3L)).thenReturn(itemEntity(3L, "Movie 3"));
        when(itemRepository.getReferenceById(1L)).thenReturn(itemEntity(1L, "Movie 1"));
    }

    private Item item(Long id, String title, double popularity) {
        return Item.builder()
                .id(id)
                .title(title)
                .overview("Overview " + id)
                .popularity(BigDecimal.valueOf(popularity))
                .build();
    }

    private ItemEntity itemEntity(Long id, String title) {
        return ItemEntity.builder()
                .id(id)
                .tmdbId(id)
                .tmdbType(TmdbType.MOVIE)
                .title(title)
                .overview("Overview " + id)
                .popularity(BigDecimal.valueOf(8.0))
                .build();
    }
}
