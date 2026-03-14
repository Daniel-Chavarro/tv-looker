package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.InteractionNotFoundException;
import org.tvl.tvlooker.domain.model.entity.Interaction;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.persistence.repository.InteractionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InteractionService.
 * Tests all CRUD operations and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InteractionService Unit Tests")
class InteractionServiceTest {

    @Mock
    private InteractionRepository interactionRepository;

    @InjectMocks
    private InteractionService interactionService;

    private Interaction testInteraction;
    private Long testInteractionId;
    private User testUser;
    private Item testItem;

    @BeforeEach
    void setUp() {
        testInteractionId = 1L;
        
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .password("password123")
                .build();
        
        testItem = Item.builder()
                .id(1L)
                .title("Test Movie")
                .overview("Test overview")
                .build();
        
        testInteraction = Interaction.builder()
                .id(testInteractionId)
                .interactionType(InteractionType.RATING)
                .user(testUser)
                .item(testItem)
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("create - should save and return interaction")
    void create_shouldSaveAndReturnInteraction() {
        // Arrange
        when(interactionRepository.save(any(Interaction.class))).thenReturn(testInteraction);

        // Act
        Interaction result = interactionService.create(testInteraction);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testInteractionId);
        assertThat(result.getInteractionType()).isEqualTo(InteractionType.RATING);
        verify(interactionRepository, times(1)).save(testInteraction);
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("getById - should return interaction when interaction exists")
    void getById_shouldReturnInteraction_whenInteractionExists() {
        // Arrange
        when(interactionRepository.findById(testInteractionId)).thenReturn(Optional.of(testInteraction));

        // Act
        Interaction result = interactionService.getById(testInteractionId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testInteractionId);
        assertThat(result.getInteractionType()).isEqualTo(InteractionType.RATING);
        verify(interactionRepository, times(1)).findById(testInteractionId);
    }

    @Test
    @DisplayName("getById - should throw InteractionNotFoundException when interaction does not exist")
    void getById_shouldThrowInteractionNotFoundException_whenInteractionDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(interactionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> interactionService.getById(nonExistentId))
                .isInstanceOf(InteractionNotFoundException.class)
                .hasMessageContaining("Interaction not found: " + nonExistentId);
        verify(interactionRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAll - should return all interactions")
    void getAll_shouldReturnAllInteractions() {
        // Arrange
        Interaction interaction2 = Interaction.builder()
                .id(2L)
                .interactionType(InteractionType.VIEW)
                .user(testUser)
                .item(testItem)
                .build();
        List<Interaction> interactions = List.of(testInteraction, interaction2);
        when(interactionRepository.findAll()).thenReturn(interactions);

        // Act
        List<Interaction> result = interactionService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testInteraction, interaction2);
        verify(interactionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no interactions exist")
    void getAll_shouldReturnEmptyList_whenNoInteractionsExist() {
        // Arrange
        when(interactionRepository.findAll()).thenReturn(List.of());

        // Act
        List<Interaction> result = interactionService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(interactionRepository, times(1)).findAll();
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("update - should update and return interaction when interaction exists")
    void update_shouldUpdateAndReturnInteraction_whenInteractionExists() {
        // Arrange
        Interaction updatedInteraction = Interaction.builder()
                .interactionType(InteractionType.VIEW)
                .user(testUser)
                .item(testItem)
                .build();
        Interaction savedInteraction = Interaction.builder()
                .id(testInteractionId)
                .interactionType(InteractionType.VIEW)
                .user(testUser)
                .item(testItem)
                .build();

        when(interactionRepository.existsById(testInteractionId)).thenReturn(true);
        when(interactionRepository.save(any(Interaction.class))).thenReturn(savedInteraction);

        // Act
        Interaction result = interactionService.update(testInteractionId, updatedInteraction);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testInteractionId);
        assertThat(result.getInteractionType()).isEqualTo(InteractionType.VIEW);
        verify(interactionRepository, times(1)).existsById(testInteractionId);
        verify(interactionRepository, times(1)).save(any(Interaction.class));
    }

    @Test
    @DisplayName("update - should throw InteractionNotFoundException when interaction does not exist")
    void update_shouldThrowInteractionNotFoundException_whenInteractionDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        Interaction updatedInteraction = Interaction.builder()
                .interactionType(InteractionType.VIEW)
                .user(testUser)
                .item(testItem)
                .build();

        when(interactionRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> interactionService.update(nonExistentId, updatedInteraction))
                .isInstanceOf(InteractionNotFoundException.class)
                .hasMessageContaining("Interaction not found: " + nonExistentId);
        verify(interactionRepository, times(1)).existsById(nonExistentId);
        verify(interactionRepository, never()).save(any(Interaction.class));
    }

    @Test
    @DisplayName("update - should set ID on interaction entity before saving")
    void update_shouldSetIdOnInteractionEntity_beforeSaving() {
        // Arrange
        Interaction updatedInteraction = Interaction.builder()
                .interactionType(InteractionType.VIEW)
                .user(testUser)
                .item(testItem)
                .build();

        when(interactionRepository.existsById(testInteractionId)).thenReturn(true);
        when(interactionRepository.save(any(Interaction.class))).thenAnswer(invocation -> {
            Interaction interaction = invocation.getArgument(0);
            assertThat(interaction.getId()).isEqualTo(testInteractionId);
            return interaction;
        });

        // Act
        interactionService.update(testInteractionId, updatedInteraction);

        // Assert
        verify(interactionRepository, times(1)).save(any(Interaction.class));
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("deleteById - should delete interaction when interaction exists")
    void deleteById_shouldDeleteInteraction_whenInteractionExists() {
        // Arrange
        when(interactionRepository.existsById(testInteractionId)).thenReturn(true);
        doNothing().when(interactionRepository).deleteById(testInteractionId);

        // Act
        interactionService.deleteById(testInteractionId);

        // Assert
        verify(interactionRepository, times(1)).existsById(testInteractionId);
        verify(interactionRepository, times(1)).deleteById(testInteractionId);
    }

    @Test
    @DisplayName("deleteById - should throw InteractionNotFoundException when interaction does not exist")
    void deleteById_shouldThrowInteractionNotFoundException_whenInteractionDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(interactionRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> interactionService.deleteById(nonExistentId))
                .isInstanceOf(InteractionNotFoundException.class)
                .hasMessageContaining("Interaction not found: " + nonExistentId);
        verify(interactionRepository, times(1)).existsById(nonExistentId);
        verify(interactionRepository, never()).deleteById(any(Long.class));
    }
}
