package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.InteractionNotFoundException;
import org.tvl.tvlooker.domain.model.Interaction;
import org.tvl.tvlooker.domain.model.Item;
import org.tvl.tvlooker.domain.model.Review;
import org.tvl.tvlooker.domain.model.User;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.model.entity.InteractionEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.entity.ReviewEntity;
import org.tvl.tvlooker.persistence.repository.InteractionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InteractionService Unit Tests")
class InteractionServiceTest {

    @Mock
    private InteractionRepository interactionRepository;

    @Mock
    private ReviewService reviewService;

    @Mock
    private UserService userService;

    @Mock
    private ItemService itemService;

    @InjectMocks
    private InteractionService interactionService;

    private Interaction testInteraction;
    private InteractionEntity testInteractionEntity;
    private User testUser;
    private Item testItem;
    private Review testReview;
    private Long testInteractionId;
    private UUID testUserId;
    private Long testItemId;

    @BeforeEach
    void setUp() {
        testInteractionId = 1L;
        testUserId = UUID.randomUUID();
        testItemId = 1L;

        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@test.com")
                .name("Test User")
                .build();

        testItem = Item.builder()
                .id(testItemId)
                .title("Test Movie")
                .overview("Test overview")
                .build();

        testReview = Review.builder()
                .id(1L)
                .userId(testUserId)
                .itemId(testItemId)
                .score(8)
                .reviewText("Great!")
                .build();

        testInteraction = Interaction.builder()
                .id(testInteractionId)
                .userId(testUserId)
                .itemId(testItemId)
                .interactionType(InteractionType.RATING)
                .reviewId(1L)
                .build();

        testInteractionEntity = InteractionEntity.builder()
                .id(testInteractionId)
                .user(UserEntity.builder().id(testUserId).username("testuser").email("test@test.com").name("Test User").build())
                .item(ItemEntity.builder().id(testItemId).title("Test Movie").overview("Test overview").build())
                .interactionType(InteractionType.RATING)
                .build();
    }

    @Test
    @DisplayName("create - should save and return interaction")
    void create_shouldSaveAndReturnInteraction() {
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(itemService.getById(testItemId)).thenReturn(testItem);
        when(reviewService.getById(1L)).thenReturn(testReview);
        when(interactionRepository.save(any(InteractionEntity.class))).thenReturn(testInteractionEntity);

        Interaction result = interactionService.create(testInteraction);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testInteractionId);
        assertThat(result.getInteractionType()).isEqualTo(InteractionType.RATING);
        verify(interactionRepository, times(1)).save(any(InteractionEntity.class));
    }

    @Test
    @DisplayName("getById - should return interaction when interaction exists")
    void getById_shouldReturnInteraction_whenInteractionExists() {
        when(interactionRepository.findById(testInteractionId)).thenReturn(Optional.of(testInteractionEntity));

        Interaction result = interactionService.getById(testInteractionId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testInteractionId);
        assertThat(result.getInteractionType()).isEqualTo(InteractionType.RATING);
        verify(interactionRepository, times(1)).findById(testInteractionId);
    }

    @Test
    @DisplayName("getById - should throw InteractionNotFoundException when interaction does not exist")
    void getById_shouldThrowInteractionNotFoundException_whenInteractionDoesNotExist() {
        Long nonExistentId = 999L;
        when(interactionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interactionService.getById(nonExistentId))
                .isInstanceOf(InteractionNotFoundException.class)
                .hasMessageContaining("Interaction not found: " + nonExistentId);
        verify(interactionRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAll - should return all interactions")
    void getAll_shouldReturnAllInteractions() {
        InteractionEntity interaction2 = InteractionEntity.builder()
                .id(2L)
                .user(UserEntity.builder().id(UUID.randomUUID()).username("user2").email("user2@test.com").name("User 2").build())
                .item(ItemEntity.builder().id(2L).title("Movie 2").overview("Overview 2").build())
                .interactionType(InteractionType.VIEW)
                .build();
        List<InteractionEntity> interactions = List.of(testInteractionEntity, interaction2);
        when(interactionRepository.findAll()).thenReturn(interactions);

        List<Interaction> result = interactionService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(interactionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no interactions exist")
    void getAll_shouldReturnEmptyList_whenNoInteractionsExist() {
        when(interactionRepository.findAll()).thenReturn(List.of());

        List<Interaction> result = interactionService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(interactionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("update - should update and return interaction when interaction exists")
    void update_shouldUpdateAndReturnInteraction_whenInteractionExists() {
        Interaction updatedInteraction = Interaction.builder()
                .userId(testUserId)
                .itemId(testItemId)
                .interactionType(InteractionType.VIEW)
                .reviewId(1L)
                .build();
        InteractionEntity savedInteraction = InteractionEntity.builder()
                .id(testInteractionId)
                .user(UserEntity.builder().id(testUserId).username("testuser").email("test@test.com").name("Test User").build())
                .item(ItemEntity.builder().id(testItemId).title("Test Movie").overview("Test overview").build())
                .interactionType(InteractionType.VIEW)
                .build();

        when(interactionRepository.existsById(testInteractionId)).thenReturn(true);
        when(interactionRepository.getReferenceById(testInteractionId)).thenReturn(testInteractionEntity);
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(itemService.getById(testItemId)).thenReturn(testItem);
        when(reviewService.getById(1L)).thenReturn(testReview);
        when(interactionRepository.save(any(InteractionEntity.class))).thenReturn(savedInteraction);

        Interaction result = interactionService.update(testInteractionId, updatedInteraction);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testInteractionId);
        assertThat(result.getInteractionType()).isEqualTo(InteractionType.VIEW);
        verify(interactionRepository, times(1)).existsById(testInteractionId);
        verify(interactionRepository, times(1)).save(any(InteractionEntity.class));
    }

    @Test
    @DisplayName("update - should throw InteractionNotFoundException when interaction does not exist")
    void update_shouldThrowInteractionNotFoundException_whenInteractionDoesNotExist() {
        Long nonExistentId = 999L;
        Interaction updatedInteraction = Interaction.builder()
                .userId(testUserId)
                .itemId(testItemId)
                .interactionType(InteractionType.VIEW)
                .reviewId(1L)
                .build();

        when(interactionRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> interactionService.update(nonExistentId, updatedInteraction))
                .isInstanceOf(InteractionNotFoundException.class)
                .hasMessageContaining("Interaction not found: " + nonExistentId);
        verify(interactionRepository, times(1)).existsById(nonExistentId);
        verify(interactionRepository, never()).save(any(InteractionEntity.class));
    }

    @Test
    @DisplayName("update - should set ID on interaction entity before saving")
    void update_shouldSetIdOnInteractionEntity_beforeSaving() {
        Interaction updatedInteraction = Interaction.builder()
                .userId(testUserId)
                .itemId(testItemId)
                .interactionType(InteractionType.VIEW)
                .reviewId(1L)
                .build();

        when(interactionRepository.existsById(testInteractionId)).thenReturn(true);
        when(interactionRepository.getReferenceById(testInteractionId)).thenReturn(testInteractionEntity);
        when(userService.getById(testUserId)).thenReturn(testUser);
        when(itemService.getById(testItemId)).thenReturn(testItem);
        when(reviewService.getById(1L)).thenReturn(testReview);
        when(interactionRepository.save(any(InteractionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        interactionService.update(testInteractionId, updatedInteraction);

        verify(interactionRepository, times(1)).save(any(InteractionEntity.class));
    }

    @Test
    @DisplayName("delete - should delete interaction when interaction exists")
    void deleteById_shouldDeleteInteraction_whenInteractionExists() {
        when(interactionRepository.existsById(testInteractionId)).thenReturn(true);
        doNothing().when(interactionRepository).deleteById(testInteractionId);

        interactionService.delete(testInteractionId);

        verify(interactionRepository, times(1)).existsById(testInteractionId);
        verify(interactionRepository, times(1)).deleteById(testInteractionId);
    }

    @Test
    @DisplayName("delete - should throw InteractionNotFoundException when interaction does not exist")
    void deleteById_shouldThrowInteractionNotFoundException_whenInteractionDoesNotExist() {
        Long nonExistentId = 999L;
        when(interactionRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> interactionService.delete(nonExistentId))
                .isInstanceOf(InteractionNotFoundException.class)
                .hasMessageContaining("Interaction not found: " + nonExistentId);
        verify(interactionRepository, times(1)).existsById(nonExistentId);
        verify(interactionRepository, never()).deleteById(any(Long.class));
    }
}
