package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;
import org.tvl.tvlooker.domain.exception.InsufficientDataException;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.model.entity.Interaction;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.motor.RecommendationEngine;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecommendationService.
 * Tests orchestration logic, service composition, and exception handling.
 */
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

    @InjectMocks
    private RecommendationService recommendationService;

    @Captor
    private ArgumentCaptor<RecommendationContext> contextCaptor;

    private UUID testUserId;
    private User testUser;
    private List<User> allUsers;
    private List<Item> allItems;
    private List<Interaction> allInteractions;
    private List<ScoredItem> scoredRecommendations;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Create test user
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .password("password123")
                .build();

        // Create test data - all users
        User user2 = User.builder()
                .id(UUID.randomUUID())
                .username("user2")
                .password("password456")
                .build();
        allUsers = List.of(testUser, user2);

        // Create test data - all items
        Item item1 = Item.builder()
                .id(1L)
                .title("Movie 1")
                .overview("Overview 1")
                .popularity(BigDecimal.valueOf(8.5))
                .build();
        Item item2 = Item.builder()
                .id(2L)
                .title("Movie 2")
                .overview("Overview 2")
                .popularity(BigDecimal.valueOf(7.5))
                .build();
        Item item3 = Item.builder()
                .id(3L)
                .title("Movie 3")
                .overview("Overview 3")
                .popularity(BigDecimal.valueOf(9.0))
                .build();
        allItems = List.of(item1, item2, item3);

        // Create test data - all interactions
        Interaction interaction1 = Interaction.builder()
                .id(1L)
                .user(testUser)
                .item(item1)
                .interactionType(InteractionType.RATING)
                .build();
        Interaction interaction2 = Interaction.builder()
                .id(2L)
                .user(user2)
                .item(item2)
                .interactionType(InteractionType.VIEW)
                .build();
        allInteractions = List.of(interaction1, interaction2);

        // Create scored recommendations
        scoredRecommendations = List.of(
                ScoredItem.builder()
                        .item(item2)
                        .score(0.95)
                        .explanation("Highly recommended")
                        .build(),
                ScoredItem.builder()
                        .item(item3)
                        .score(0.85)
                        .explanation("Popular choice")
                        .build(),
                ScoredItem.builder()
                        .item(item1)
                        .score(0.75)
                        .explanation("Based on similar users")
                        .build()
        );
    }

    // ========== HAPPY PATH TESTS ==========

    @Test
    @DisplayName("getUserRecommendations - should return items when user exists and has interactions")
    void getUserRecommendations_shouldReturnItems_whenUserExistsAndHasInteractions() {
        // Arrange
        int limit = 5;
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(userService.getAll()).thenReturn(allUsers);
        when(itemService.getAll()).thenReturn(allItems);
        when(interactionService.getAll()).thenReturn(allInteractions);
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenReturn(scoredRecommendations);

        // Act
        List<Item> result = recommendationService.getUserRecommendations(testUserId, limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTitle()).isEqualTo("Movie 2");
        assertThat(result.get(1).getTitle()).isEqualTo("Movie 3");
        assertThat(result.get(2).getTitle()).isEqualTo("Movie 1");
        
        verify(userService, times(1)).getById(testUserId);
        verify(userService, times(1)).getAll();
        verify(itemService, times(1)).getAll();
        verify(interactionService, times(1)).getAll();
        verify(recommendationEngine, times(1)).recommend(eq(testUser), any(RecommendationContext.class));
    }

    @Test
    @DisplayName("getUserRecommendations - should limit results based on limit parameter")
    void getUserRecommendations_shouldLimitResults_basedOnLimitParameter() {
        // Arrange
        int limit = 2;
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(userService.getAll()).thenReturn(allUsers);
        when(itemService.getAll()).thenReturn(allItems);
        when(interactionService.getAll()).thenReturn(allInteractions);
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenReturn(scoredRecommendations);

        // Act
        List<Item> result = recommendationService.getUserRecommendations(testUserId, limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2); // Limited to 2 even though engine returned 3
        assertThat(result.get(0).getTitle()).isEqualTo("Movie 2");
        assertThat(result.get(1).getTitle()).isEqualTo("Movie 3");
    }

    @Test
    @DisplayName("getUserRecommendations - should return empty list when engine returns empty")
    void getUserRecommendations_shouldReturnEmptyList_whenEngineReturnsEmpty() {
        // Arrange
        int limit = 5;
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(userService.getAll()).thenReturn(allUsers);
        when(itemService.getAll()).thenReturn(allItems);
        when(interactionService.getAll()).thenReturn(allInteractions);
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenReturn(List.of());

        // Act
        List<Item> result = recommendationService.getUserRecommendations(testUserId, limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserRecommendations - should load all system data for context")
    void getUserRecommendations_shouldLoadAllSystemData_forContext() {
        // Arrange
        int limit = 5;
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(userService.getAll()).thenReturn(allUsers);
        when(itemService.getAll()).thenReturn(allItems);
        when(interactionService.getAll()).thenReturn(allInteractions);
        when(recommendationEngine.recommend(eq(testUser), contextCaptor.capture()))
                .thenReturn(scoredRecommendations);

        // Act
        recommendationService.getUserRecommendations(testUserId, limit);

        // Assert
        RecommendationContext capturedContext = contextCaptor.getValue();
        assertThat(capturedContext).isNotNull();
        assertThat(capturedContext.getUsers()).hasSize(2);
        assertThat(capturedContext.getItems()).hasSize(3);
        assertThat(capturedContext.getInteractions()).hasSize(2);
    }

    @Test
    @DisplayName("getUserRecommendations - should call all entity services")
    void getUserRecommendations_shouldCallAllEntityServices() {
        // Arrange
        int limit = 5;
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(userService.getAll()).thenReturn(allUsers);
        when(itemService.getAll()).thenReturn(allItems);
        when(interactionService.getAll()).thenReturn(allInteractions);
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenReturn(scoredRecommendations);

        // Act
        recommendationService.getUserRecommendations(testUserId, limit);

        // Assert
        verify(userService, times(1)).getById(testUserId);
        verify(userService, times(1)).getAll();
        verify(itemService, times(1)).getAll();
        verify(interactionService, times(1)).getAll();
    }

    // ========== VALIDATION TESTS ==========

    @Test
    @DisplayName("getUserRecommendations - should throw IllegalArgumentException when userId is null")
    void getUserRecommendations_shouldThrowIllegalArgumentException_whenUserIdIsNull() {
        // Arrange
        int limit = 5;

        // Act & Assert
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(null, limit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null");
        
        verify(userService, never()).getById(any());
        verify(recommendationEngine, never()).recommend(any(), any());
    }

    @Test
    @DisplayName("getUserRecommendations - should throw IllegalArgumentException when limit is zero")
    void getUserRecommendations_shouldThrowIllegalArgumentException_whenLimitIsZero() {
        // Arrange
        int limit = 0;

        // Act & Assert
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(testUserId, limit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be greater than 0");
        
        verify(userService, never()).getById(any());
        verify(recommendationEngine, never()).recommend(any(), any());
    }

    @Test
    @DisplayName("getUserRecommendations - should throw IllegalArgumentException when limit is negative")
    void getUserRecommendations_shouldThrowIllegalArgumentException_whenLimitIsNegative() {
        // Arrange
        int limit = -5;

        // Act & Assert
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(testUserId, limit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be greater than 0");
        
        verify(userService, never()).getById(any());
        verify(recommendationEngine, never()).recommend(any(), any());
    }

    // ========== EXCEPTION HANDLING TESTS ==========

    @Test
    @DisplayName("getUserRecommendations - should propagate UserNotFoundException when user not found")
    void getUserRecommendations_shouldPropagateUserNotFoundException_whenUserNotFound() {
        // Arrange
        int limit = 5;
        UUID nonExistentUserId = UUID.randomUUID();
        when(userService.getById(nonExistentUserId))
                .thenThrow(new UserNotFoundException("User not found with id: " + nonExistentUserId));

        // Act & Assert
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(nonExistentUserId, limit))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: " + nonExistentUserId);
        
        verify(userService, times(1)).getById(nonExistentUserId);
        verify(userService, never()).getAll();
        verify(itemService, never()).getAll();
        verify(interactionService, never()).getAll();
        verify(recommendationEngine, never()).recommend(any(), any());
    }

    @Test
    @DisplayName("getUserRecommendations - should propagate InsufficientDataException from engine")
    void getUserRecommendations_shouldPropagateInsufficientDataException_fromEngine() {
        // Arrange
        int limit = 5;
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(userService.getAll()).thenReturn(allUsers);
        when(itemService.getAll()).thenReturn(allItems);
        when(interactionService.getAll()).thenReturn(allInteractions);
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenThrow(new InsufficientDataException("Insufficient data to generate recommendations"));

        // Act & Assert
        assertThatThrownBy(() -> recommendationService.getUserRecommendations(testUserId, limit))
                .isInstanceOf(InsufficientDataException.class)
                .hasMessageContaining("Insufficient data to generate recommendations");
        
        verify(recommendationEngine, times(1)).recommend(eq(testUser), any(RecommendationContext.class));
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("getUserRecommendations - should work with new user (no interactions)")
    void getUserRecommendations_shouldWork_withNewUserNoInteractions() {
        // Arrange
        int limit = 5;
        UUID newUserId = UUID.randomUUID();
        User newUser = User.builder()
                .id(newUserId)
                .username("newuser")
                .password("password")
                .build();
        
        when(userService.getById(newUserId)).thenReturn(newUser);
        when(userService.getAll()).thenReturn(List.of(newUser));
        when(itemService.getAll()).thenReturn(allItems);
        when(interactionService.getAll()).thenReturn(List.of()); // No interactions
        when(recommendationEngine.recommend(eq(newUser), any(RecommendationContext.class)))
                .thenReturn(scoredRecommendations); // Popularity-based recommendations

        // Act
        List<Item> result = recommendationService.getUserRecommendations(newUserId, limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        verify(recommendationEngine, times(1)).recommend(eq(newUser), any(RecommendationContext.class));
    }

    @Test
    @DisplayName("getUserRecommendations - should work when no items in system")
    void getUserRecommendations_shouldWork_whenNoItemsInSystem() {
        // Arrange
        int limit = 5;
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(userService.getAll()).thenReturn(allUsers);
        when(itemService.getAll()).thenReturn(List.of()); // No items
        when(interactionService.getAll()).thenReturn(allInteractions);
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenReturn(List.of()); // No recommendations possible

        // Act
        List<Item> result = recommendationService.getUserRecommendations(testUserId, limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserRecommendations - should handle limit larger than available recommendations")
    void getUserRecommendations_shouldHandleLimitLargerThanAvailable() {
        // Arrange
        int limit = 100; // Much larger than available recommendations
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(userService.getAll()).thenReturn(allUsers);
        when(itemService.getAll()).thenReturn(allItems);
        when(interactionService.getAll()).thenReturn(allInteractions);
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenReturn(scoredRecommendations); // Only 3 items

        // Act
        List<Item> result = recommendationService.getUserRecommendations(testUserId, limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3); // Returns all available, not more
    }

    @Test
    @DisplayName("getUserRecommendations - should extract items correctly from ScoredItems")
    void getUserRecommendations_shouldExtractItemsCorrectly_fromScoredItems() {
        // Arrange
        int limit = 5;
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(userService.getAll()).thenReturn(allUsers);
        when(itemService.getAll()).thenReturn(allItems);
        when(interactionService.getAll()).thenReturn(allInteractions);
        when(recommendationEngine.recommend(eq(testUser), any(RecommendationContext.class)))
                .thenReturn(scoredRecommendations);

        // Act
        List<Item> result = recommendationService.getUserRecommendations(testUserId, limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        // Verify items are extracted in order (scores not exposed)
        assertThat(result.get(0)).isEqualTo(scoredRecommendations.get(0).getItem());
        assertThat(result.get(1)).isEqualTo(scoredRecommendations.get(1).getItem());
        assertThat(result.get(2)).isEqualTo(scoredRecommendations.get(2).getItem());
    }
}
