package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.ItemNotFoundException;
import org.tvl.tvlooker.domain.model.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.persistence.repository.ItemRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService Unit Tests")
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item testItem;
    private ItemEntity testItemEntity;
    private Long testItemId;

    @BeforeEach
    void setUp() {
        testItemId = 1L;
        testItem = Item.builder()
                .id(testItemId)
                .title("Test Movie")
                .overview("Test overview")
                .popularity(BigDecimal.valueOf(8.5))
                .tmdbType(TmdbType.MOVIE)
                .tmdbId(550L)
                .build();

        testItemEntity = ItemEntity.builder()
                .id(testItemId)
                .title("Test Movie")
                .overview("Test overview")
                .popularity(BigDecimal.valueOf(8.5))
                .tmdbType(TmdbType.MOVIE)
                .tmdbId(550L)
                .build();
    }

    @Test
    @DisplayName("create - should save and return item")
    void create_shouldSaveAndReturnItem() {
        when(itemRepository.save(any(ItemEntity.class))).thenReturn(testItemEntity);

        Item result = itemService.create(testItem);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testItemId);
        assertThat(result.getTitle()).isEqualTo("Test Movie");
        verify(itemRepository, times(1)).save(any(ItemEntity.class));
    }

    @Test
    @DisplayName("getById - should return item when item exists")
    void getById_shouldReturnItem_whenItemExists() {
        when(itemRepository.findById(testItemId)).thenReturn(Optional.of(testItemEntity));

        Item result = itemService.getById(testItemId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testItemId);
        assertThat(result.getTitle()).isEqualTo("Test Movie");
        verify(itemRepository, times(1)).findById(testItemId);
    }

    @Test
    @DisplayName("getById - should throw ItemNotFoundException when item does not exist")
    void getById_shouldThrowItemNotFoundException_whenItemDoesNotExist() {
        Long nonExistentId = 999L;
        when(itemRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getById(nonExistentId))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("Item not found: " + nonExistentId);
        verify(itemRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAll - should return all items")
    void getAll_shouldReturnAllItems() {
        ItemEntity item2 = ItemEntity.builder()
                .id(2L)
                .title("Test Movie 2")
                .overview("Test overview 2")
                .popularity(BigDecimal.valueOf(7.5))
                .tmdbType(TmdbType.MOVIE)
                .tmdbId(551L)
                .build();
        List<ItemEntity> items = List.of(testItemEntity, item2);
        when(itemRepository.findAll()).thenReturn(items);

        List<Item> result = itemService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no items exist")
    void getAll_shouldReturnEmptyList_whenNoItemsExist() {
        when(itemRepository.findAll()).thenReturn(List.of());

        List<Item> result = itemService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("update - should update and return item when item exists")
    void update_shouldUpdateAndReturnItem_whenItemExists() {
        Item updatedItem = Item.builder()
                .title("Updated Movie")
                .overview("Updated overview")
                .popularity(BigDecimal.valueOf(9.0))
                .tmdbType(TmdbType.MOVIE)
                .tmdbId(550L)
                .build();
        ItemEntity savedItem = ItemEntity.builder()
                .id(testItemId)
                .title("Updated Movie")
                .overview("Updated overview")
                .popularity(BigDecimal.valueOf(9.0))
                .tmdbType(TmdbType.MOVIE)
                .tmdbId(550L)
                .build();

        when(itemRepository.existsById(testItemId)).thenReturn(true);
        when(itemRepository.getReferenceById(testItemId)).thenReturn(testItemEntity);
        when(itemRepository.save(any(ItemEntity.class))).thenReturn(savedItem);

        Item result = itemService.update(testItemId, updatedItem);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testItemId);
        assertThat(result.getTitle()).isEqualTo("Updated Movie");
        verify(itemRepository, times(1)).existsById(testItemId);
        verify(itemRepository, times(1)).save(any(ItemEntity.class));
    }

    @Test
    @DisplayName("update - should throw ItemNotFoundException when item does not exist")
    void update_shouldThrowItemNotFoundException_whenItemDoesNotExist() {
        Long nonExistentId = 999L;
        Item updatedItem = Item.builder()
                .title("Updated Movie")
                .overview("Updated overview")
                .popularity(BigDecimal.valueOf(9.0))
                .tmdbType(TmdbType.MOVIE)
                .tmdbId(550L)
                .build();

        when(itemRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> itemService.update(nonExistentId, updatedItem))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("Item not found: " + nonExistentId);
        verify(itemRepository, times(1)).existsById(nonExistentId);
        verify(itemRepository, never()).save(any(ItemEntity.class));
    }

    @Test
    @DisplayName("update - should set ID on item entity before saving")
    void update_shouldSetIdOnItemEntity_beforeSaving() {
        Item updatedItem = Item.builder()
                .title("Updated Movie")
                .overview("Updated overview")
                .popularity(BigDecimal.valueOf(9.0))
                .tmdbType(TmdbType.MOVIE)
                .tmdbId(550L)
                .build();

        when(itemRepository.existsById(testItemId)).thenReturn(true);
        when(itemRepository.getReferenceById(testItemId)).thenReturn(testItemEntity);
        when(itemRepository.save(any(ItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        itemService.update(testItemId, updatedItem);

        verify(itemRepository, times(1)).save(any(ItemEntity.class));
    }

    @Test
    @DisplayName("delete - should delete item when item exists")
    void deleteById_shouldDeleteItem_whenItemExists() {
        when(itemRepository.existsById(testItemId)).thenReturn(true);
        doNothing().when(itemRepository).deleteById(testItemId);

        itemService.deleteById(testItemId);

        verify(itemRepository, times(1)).existsById(testItemId);
        verify(itemRepository, times(1)).deleteById(testItemId);
    }

    @Test
    @DisplayName("delete - should throw ItemNotFoundException when item does not exist")
    void deleteById_shouldThrowItemNotFoundException_whenItemDoesNotExist() {
        Long nonExistentId = 999L;
        when(itemRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> itemService.deleteById(nonExistentId))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("Item not found: " + nonExistentId);
        verify(itemRepository, times(1)).existsById(nonExistentId);
        verify(itemRepository, never()).deleteById(any(Long.class));
    }
}
