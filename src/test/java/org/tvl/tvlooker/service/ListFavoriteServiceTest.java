package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.ListFavoriteNotFoundException;
import org.tvl.tvlooker.domain.model.entity.ListFavorite;
import org.tvl.tvlooker.persistence.repository.ListFavoriteRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ListFavoriteService.
 * Tests all CRUD operations and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListFavoriteService Unit Tests")
class ListFavoriteServiceTest {

    @Mock
    private ListFavoriteRepository listFavoriteRepository;

    @InjectMocks
    private ListFavoriteService listFavoriteService;

    private Long testListId;
    private ListFavorite testList;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testListId = 1L;
        testUserId = UUID.randomUUID();
        testList = ListFavorite.builder()
                .id(testListId)
                .name("My Favorites")
                .description("My favorite movies")
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("create - should save and return list favorite")
    void create_shouldSaveAndReturnListFavorite() {
        // Arrange
        when(listFavoriteRepository.save(any(ListFavorite.class))).thenReturn(testList);

        // Act
        ListFavorite result = listFavoriteService.create(testList);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testListId);
        assertThat(result.getName()).isEqualTo("My Favorites");
        verify(listFavoriteRepository, times(1)).save(testList);
    }

    // ========== GET BY ID TESTS ==========

    @Test
    @DisplayName("getById - should return list favorite when list exists")
    void getById_shouldReturnListFavorite_whenListExists() {
        // Arrange
        when(listFavoriteRepository.findById(testListId)).thenReturn(Optional.of(testList));

        // Act
        ListFavorite result = listFavoriteService.getById(testListId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testListId);
        assertThat(result.getName()).isEqualTo("My Favorites");
        verify(listFavoriteRepository, times(1)).findById(testListId);
    }

    @Test
    @DisplayName("getById - should throw ListFavoriteNotFoundException when list does not exist")
    void getById_shouldThrowListFavoriteNotFoundException_whenListDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(listFavoriteRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listFavoriteService.getById(nonExistentId))
                .isInstanceOf(ListFavoriteNotFoundException.class)
                .hasMessageContaining("ListFavorite not found: " + nonExistentId);
        verify(listFavoriteRepository, times(1)).findById(nonExistentId);
    }

    // ========== GET ALL TESTS ==========

    @Test
    @DisplayName("getAll - should return all list favorites")
    void getAll_shouldReturnAllListFavorites() {
        // Arrange
        ListFavorite list2 = ListFavorite.builder()
                .id(2L)
                .name("Watchlist")
                .description("Movies to watch")
                .build();
        List<ListFavorite> lists = List.of(testList, list2);
        when(listFavoriteRepository.findAll()).thenReturn(lists);

        // Act
        List<ListFavorite> result = listFavoriteService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testList, list2);
        verify(listFavoriteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no lists exist")
    void getAll_shouldReturnEmptyList_whenNoListsExist() {
        // Arrange
        when(listFavoriteRepository.findAll()).thenReturn(List.of());

        // Act
        List<ListFavorite> result = listFavoriteService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(listFavoriteRepository, times(1)).findAll();
    }

    // ========== GET LIST FAVORITES BY USER ID TESTS ==========

    @Test
    @DisplayName("getListFavorites - should return user's list favorites")
    void getListFavorites_shouldReturnUserListFavorites() {
        // Arrange
        ListFavorite list2 = ListFavorite.builder()
                .id(2L)
                .name("Watchlist")
                .description("Movies to watch")
                .build();
        List<ListFavorite> userLists = List.of(testList, list2);
        when(listFavoriteRepository.findByUserId(testUserId)).thenReturn(userLists);

        // Act
        List<ListFavorite> result = listFavoriteService.getListFavorites(testUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testList, list2);
        verify(listFavoriteRepository, times(1)).findByUserId(testUserId);
    }

    @Test
    @DisplayName("getListFavorites - should return empty list when user has no lists")
    void getListFavorites_shouldReturnEmptyList_whenUserHasNoLists() {
        // Arrange
        when(listFavoriteRepository.findByUserId(testUserId)).thenReturn(List.of());

        // Act
        List<ListFavorite> result = listFavoriteService.getListFavorites(testUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(listFavoriteRepository, times(1)).findByUserId(testUserId);
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("update - should update and return list favorite when list exists")
    void update_shouldUpdateAndReturnListFavorite_whenListExists() {
        // Arrange
        ListFavorite updatedList = ListFavorite.builder()
                .name("Updated List")
                .description("Updated description")
                .build();
        ListFavorite savedList = ListFavorite.builder()
                .id(testListId)
                .name("Updated List")
                .description("Updated description")
                .build();

        when(listFavoriteRepository.existsById(testListId)).thenReturn(true);
        when(listFavoriteRepository.save(any(ListFavorite.class))).thenReturn(savedList);

        // Act
        ListFavorite result = listFavoriteService.update(testListId, updatedList);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testListId);
        assertThat(result.getName()).isEqualTo("Updated List");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(listFavoriteRepository, times(1)).existsById(testListId);
        verify(listFavoriteRepository, times(1)).save(updatedList);
    }

    @Test
    @DisplayName("update - should set ID on list favorite before saving")
    void update_shouldSetIdOnListFavorite_beforeSaving() {
        // Arrange
        ListFavorite updatedList = ListFavorite.builder()
                .name("Updated List")
                .description("Updated description")
                .build();

        when(listFavoriteRepository.existsById(testListId)).thenReturn(true);
        when(listFavoriteRepository.save(any(ListFavorite.class))).thenReturn(updatedList);

        // Act
        listFavoriteService.update(testListId, updatedList);

        // Assert
        assertThat(updatedList.getId()).isEqualTo(testListId);
        verify(listFavoriteRepository, times(1)).save(updatedList);
    }

    @Test
    @DisplayName("update - should throw ListFavoriteNotFoundException when list does not exist")
    void update_shouldThrowListFavoriteNotFoundException_whenListDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        ListFavorite updatedList = ListFavorite.builder()
                .name("Updated List")
                .description("Updated description")
                .build();

        when(listFavoriteRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> listFavoriteService.update(nonExistentId, updatedList))
                .isInstanceOf(ListFavoriteNotFoundException.class)
                .hasMessageContaining("ListFavorite not found: " + nonExistentId);
        verify(listFavoriteRepository, times(1)).existsById(nonExistentId);
        verify(listFavoriteRepository, never()).save(any());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("deleteById - should delete list favorite when list exists")
    void deleteById_shouldDeleteListFavorite_whenListExists() {
        // Arrange
        when(listFavoriteRepository.existsById(testListId)).thenReturn(true);
        doNothing().when(listFavoriteRepository).deleteById(testListId);

        // Act
        listFavoriteService.deleteById(testListId);

        // Assert
        verify(listFavoriteRepository, times(1)).existsById(testListId);
        verify(listFavoriteRepository, times(1)).deleteById(testListId);
    }

    @Test
    @DisplayName("deleteById - should throw ListFavoriteNotFoundException when list does not exist")
    void deleteById_shouldThrowListFavoriteNotFoundException_whenListDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(listFavoriteRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> listFavoriteService.deleteById(nonExistentId))
                .isInstanceOf(ListFavoriteNotFoundException.class)
                .hasMessageContaining("ListFavorite not found: " + nonExistentId);
        verify(listFavoriteRepository, times(1)).existsById(nonExistentId);
        verify(listFavoriteRepository, never()).deleteById(any());
    }
}
