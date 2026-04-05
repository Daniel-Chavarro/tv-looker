package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.ListFavoriteNotFoundException;
import org.tvl.tvlooker.domain.model.dto.ListFavorite;
import org.tvl.tvlooker.domain.model.entity.ListFavoriteEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.persistence.repository.ListFavoriteRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListFavoriteService Unit Tests")
class ListFavoriteServiceTest {

    @Mock
    private ListFavoriteRepository listFavoriteRepository;

    @Mock
    private ItemService itemService;

    @InjectMocks
    private ListFavoriteService listFavoriteService;

    private Long testListId;
    private ListFavorite testList;
    private ListFavoriteEntity testListEntity;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testListId = 1L;
        testUserId = UUID.randomUUID();
        
        testList = ListFavorite.builder()
                .id(testListId)
                .userId(testUserId)
                .name("My Favorites")
                .description("My favorite movies")
                .build();

        testListEntity = ListFavoriteEntity.builder()
                .id(testListId)
                .user(UserEntity.builder().id(testUserId).username("testuser").email("test@test.com").name("Test User").build())
                .name("My Favorites")
                .description("My favorite movies")
                .build();
    }

    @Test
    @DisplayName("create - should save and return list favorite")
    void create_shouldSaveAndReturnListFavorite() {
        when(listFavoriteRepository.save(any(ListFavoriteEntity.class))).thenReturn(testListEntity);

        ListFavorite result = listFavoriteService.create(testList);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testListId);
        assertThat(result.getName()).isEqualTo("My Favorites");
        verify(listFavoriteRepository, times(1)).save(any(ListFavoriteEntity.class));
    }

    @Test
    @DisplayName("getById - should return list favorite when list exists")
    void getById_shouldReturnListFavorite_whenListExists() {
        when(listFavoriteRepository.findById(testListId)).thenReturn(Optional.of(testListEntity));

        ListFavorite result = listFavoriteService.getById(testListId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testListId);
        assertThat(result.getName()).isEqualTo("My Favorites");
        verify(listFavoriteRepository, times(1)).findById(testListId);
    }

    @Test
    @DisplayName("getById - should throw ListFavoriteNotFoundException when list does not exist")
    void getById_shouldThrowListFavoriteNotFoundException_whenListDoesNotExist() {
        Long nonExistentId = 999L;
        when(listFavoriteRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listFavoriteService.getById(nonExistentId))
                .isInstanceOf(ListFavoriteNotFoundException.class)
                .hasMessageContaining("ListFavorite not found: " + nonExistentId);
        verify(listFavoriteRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAll - should return all list favorites")
    void getAll_shouldReturnAllListFavorites() {
        ListFavoriteEntity list2 = ListFavoriteEntity.builder()
                .id(2L)
                .user(UserEntity.builder().id(UUID.randomUUID()).username("user2").email("user2@test.com").name("User 2").build())
                .name("Watchlist")
                .description("Movies to watch")
                .build();
        List<ListFavoriteEntity> lists = List.of(testListEntity, list2);
        when(listFavoriteRepository.findAll()).thenReturn(lists);

        List<ListFavorite> result = listFavoriteService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(listFavoriteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no lists exist")
    void getAll_shouldReturnEmptyList_whenNoListsExist() {
        when(listFavoriteRepository.findAll()).thenReturn(List.of());

        List<ListFavorite> result = listFavoriteService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(listFavoriteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getListFavorites - should return user's list favorites")
    void getListFavorites_shouldReturnUserListFavorites() {
        ListFavoriteEntity list2 = ListFavoriteEntity.builder()
                .id(2L)
                .user(UserEntity.builder().id(testUserId).username("testuser").email("test@test.com").name("Test User").build())
                .name("Watchlist")
                .description("Movies to watch")
                .build();
        List<ListFavoriteEntity> userLists = List.of(testListEntity, list2);
        when(listFavoriteRepository.findByUserId(testUserId)).thenReturn(userLists);

        List<ListFavorite> result = listFavoriteService.getListFavorites(testUserId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(listFavoriteRepository, times(1)).findByUserId(testUserId);
    }

    @Test
    @DisplayName("getListFavorites - should return empty list when user has no lists")
    void getListFavorites_shouldReturnEmptyList_whenUserHasNoLists() {
        when(listFavoriteRepository.findByUserId(testUserId)).thenReturn(List.of());

        List<ListFavorite> result = listFavoriteService.getListFavorites(testUserId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(listFavoriteRepository, times(1)).findByUserId(testUserId);
    }

    @Test
    @DisplayName("update - should update and return list favorite when list exists")
    void update_shouldUpdateAndReturnListFavorite_whenListExists() {
        ListFavorite updatedList = ListFavorite.builder()
                .userId(testUserId)
                .name("Updated List")
                .description("Updated description")
                .build();
        ListFavoriteEntity savedList = ListFavoriteEntity.builder()
                .id(testListId)
                .user(UserEntity.builder().id(testUserId).username("testuser").email("test@test.com").name("Test User").build())
                .name("Updated List")
                .description("Updated description")
                .build();

        when(listFavoriteRepository.existsById(testListId)).thenReturn(true);
        when(listFavoriteRepository.getReferenceById(testListId)).thenReturn(testListEntity);
        when(listFavoriteRepository.save(any(ListFavoriteEntity.class))).thenReturn(savedList);

        ListFavorite result = listFavoriteService.update(testListId, updatedList);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testListId);
        assertThat(result.getName()).isEqualTo("Updated List");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(listFavoriteRepository, times(1)).existsById(testListId);
        verify(listFavoriteRepository, times(1)).save(any(ListFavoriteEntity.class));
    }

    @Test
    @DisplayName("update - should set ID on list favorite before saving")
    void update_shouldSetIdOnListFavorite_beforeSaving() {
        ListFavorite updatedList = ListFavorite.builder()
                .userId(testUserId)
                .name("Updated List")
                .description("Updated description")
                .build();

        when(listFavoriteRepository.existsById(testListId)).thenReturn(true);
        when(listFavoriteRepository.getReferenceById(testListId)).thenReturn(testListEntity);
        when(listFavoriteRepository.save(any(ListFavoriteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listFavoriteService.update(testListId, updatedList);

        verify(listFavoriteRepository, times(1)).save(any(ListFavoriteEntity.class));
    }

    @Test
    @DisplayName("update - should throw ListFavoriteNotFoundException when list does not exist")
    void update_shouldThrowListFavoriteNotFoundException_whenListDoesNotExist() {
        Long nonExistentId = 999L;
        ListFavorite updatedList = ListFavorite.builder()
                .userId(testUserId)
                .name("Updated List")
                .description("Updated description")
                .build();

        when(listFavoriteRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> listFavoriteService.update(nonExistentId, updatedList))
                .isInstanceOf(ListFavoriteNotFoundException.class)
                .hasMessageContaining("ListFavorite not found: " + nonExistentId);
        verify(listFavoriteRepository, times(1)).existsById(nonExistentId);
        verify(listFavoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - should delete list favorite when list exists")
    void deleteById_shouldDeleteListFavorite_whenListExists() {
        when(listFavoriteRepository.existsById(testListId)).thenReturn(true);
        doNothing().when(listFavoriteRepository).deleteById(testListId);

        listFavoriteService.deleteById(testListId);

        verify(listFavoriteRepository, times(1)).existsById(testListId);
        verify(listFavoriteRepository, times(1)).deleteById(testListId);
    }

    @Test
    @DisplayName("delete - should throw ListFavoriteNotFoundException when list does not exist")
    void deleteById_shouldThrowListFavoriteNotFoundException_whenListDoesNotExist() {
        Long nonExistentId = 999L;
        when(listFavoriteRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> listFavoriteService.deleteById(nonExistentId))
                .isInstanceOf(ListFavoriteNotFoundException.class)
                .hasMessageContaining("ListFavorite not found: " + nonExistentId);
        verify(listFavoriteRepository, times(1)).existsById(nonExistentId);
        verify(listFavoriteRepository, never()).deleteById(any());
    }
}
