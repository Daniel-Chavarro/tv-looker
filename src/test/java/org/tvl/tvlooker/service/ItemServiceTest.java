package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.ItemNotFoundException;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.persistence.repository.ItemRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ItemService.
 * Tests all CRUD operations and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService Unit Tests")
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item testItem;
    private Long testItemId;

    @BeforeEach
    void setUp() {
        testItemId = 1L;
        testItem = Item.builder()
                .id(testItemId)
                .title("Test Movie")
                .overview("Test overview")
                .popularity(BigDecimal.valueOf(8.5))
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("create - should save and return item")
    void create_shouldSaveAndReturnItem() {
        // Arrange
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        Item result = itemService.create(testItem);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testItemId);
        assertThat(result.getTitle()).isEqualTo("Test Movie");
        verify(itemRepository, times(1)).save(testItem);
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("getById - should return item when item exists")
    void getById_shouldReturnItem_whenItemExists() {
        // Arrange
        when(itemRepository.findById(testItemId)).thenReturn(Optional.of(testItem));

        // Act
        Item result = itemService.getById(testItemId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testItemId);
        assertThat(result.getTitle()).isEqualTo("Test Movie");
        verify(itemRepository, times(1)).findById(testItemId);
    }

    @Test
    @DisplayName("getById - should throw ItemNotFoundException when item does not exist")
    void getById_shouldThrowItemNotFoundException_whenItemDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(itemRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> itemService.getById(nonExistentId))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("Item not found: " + nonExistentId);
        verify(itemRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAll - should return all items")
    void getAll_shouldReturnAllItems() {
        // Arrange
        Item item2 = Item.builder()
                .id(2L)
                .title("Test Movie 2")
                .overview("Test overview 2")
                .popularity(BigDecimal.valueOf(7.5))
                .build();
        List<Item> items = List.of(testItem, item2);
        when(itemRepository.findAll()).thenReturn(items);

        // Act
        List<Item> result = itemService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testItem, item2);
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no items exist")
    void getAll_shouldReturnEmptyList_whenNoItemsExist() {
        // Arrange
        when(itemRepository.findAll()).thenReturn(List.of());

        // Act
        List<Item> result = itemService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(itemRepository, times(1)).findAll();
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("update - should update and return item when item exists")
    void update_shouldUpdateAndReturnItem_whenItemExists() {
        // Arrange
        Item updatedItem = Item.builder()
                .title("Updated Movie")
                .overview("Updated overview")
                .popularity(BigDecimal.valueOf(9.0))
                .build();
        Item savedItem = Item.builder()
                .id(testItemId)
                .title("Updated Movie")
                .overview("Updated overview")
                .popularity(BigDecimal.valueOf(9.0))
                .build();

        when(itemRepository.existsById(testItemId)).thenReturn(true);
        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);

        // Act
        Item result = itemService.update(testItemId, updatedItem);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testItemId);
        assertThat(result.getTitle()).isEqualTo("Updated Movie");
        verify(itemRepository, times(1)).existsById(testItemId);
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    @DisplayName("update - should throw ItemNotFoundException when item does not exist")
    void update_shouldThrowItemNotFoundException_whenItemDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        Item updatedItem = Item.builder()
                .title("Updated Movie")
                .overview("Updated overview")
                .popularity(BigDecimal.valueOf(9.0))
                .build();

        when(itemRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> itemService.update(nonExistentId, updatedItem))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("Item not found: " + nonExistentId);
        verify(itemRepository, times(1)).existsById(nonExistentId);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    @DisplayName("update - should set ID on item entity before saving")
    void update_shouldSetIdOnItemEntity_beforeSaving() {
        // Arrange
        Item updatedItem = Item.builder()
                .title("Updated Movie")
                .overview("Updated overview")
                .build();

        when(itemRepository.existsById(testItemId)).thenReturn(true);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            assertThat(item.getId()).isEqualTo(testItemId);
            return item;
        });

        // Act
        itemService.update(testItemId, updatedItem);

        // Assert
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("deleteById - should delete item when item exists")
    void deleteById_shouldDeleteItem_whenItemExists() {
        // Arrange
        when(itemRepository.existsById(testItemId)).thenReturn(true);
        doNothing().when(itemRepository).deleteById(testItemId);

        // Act
        itemService.deleteById(testItemId);

        // Assert
        verify(itemRepository, times(1)).existsById(testItemId);
        verify(itemRepository, times(1)).deleteById(testItemId);
    }

    @Test
    @DisplayName("deleteById - should throw ItemNotFoundException when item does not exist")
    void deleteById_shouldThrowItemNotFoundException_whenItemDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(itemRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> itemService.deleteById(nonExistentId))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("Item not found: " + nonExistentId);
        verify(itemRepository, times(1)).existsById(nonExistentId);
        verify(itemRepository, never()).deleteById(any(Long.class));
    }
}
