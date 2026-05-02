package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.tvl.tvlooker.domain.exception.DirectorNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Director;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DirectorService Unit Tests")
class DirectorServiceTest {

    @Mock
    private DirectorRepository directorRepository;

    @InjectMocks
    private DirectorService directorService;

    private Long testDirectorId;
    private Director testDirector;
    private DirectorEntity testDirectorEntity;

    @BeforeEach
    void setUp() {
        testDirectorId = 1L;
        testDirector = Director.builder()
                .id(testDirectorId)
                .tmdbId(525L)
                .name("Christopher Nolan")
                .build();

        testDirectorEntity = DirectorEntity.builder()
                .id(testDirectorId)
                .tmdbId(525L)
                .name("Christopher Nolan")
                .build();
    }

    @Test
    @DisplayName("create - should save and return director")
    void create_shouldSaveAndReturnDirector() {
        when(directorRepository.save(any(DirectorEntity.class))).thenReturn(testDirectorEntity);

        Director result = directorService.create(testDirector);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testDirectorId);
        assertThat(result.getName()).isEqualTo("Christopher Nolan");
        assertThat(result.getTmdbId()).isEqualTo(525L);
        verify(directorRepository, times(1)).save(any(DirectorEntity.class));
    }

    @Test
    @DisplayName("getById - should return director when director exists")
    void getById_shouldReturnDirector_whenDirectorExists() {
        when(directorRepository.findById(testDirectorId)).thenReturn(Optional.of(testDirectorEntity));

        Director result = directorService.getById(testDirectorId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testDirectorId);
        assertThat(result.getName()).isEqualTo("Christopher Nolan");
        verify(directorRepository, times(1)).findById(testDirectorId);
    }

    @Test
    @DisplayName("getById - should throw DirectorNotFoundException when director does not exist")
    void getById_shouldThrowDirectorNotFoundException_whenDirectorDoesNotExist() {
        Long nonExistentId = 999L;
        when(directorRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> directorService.getById(nonExistentId))
                .isInstanceOf(DirectorNotFoundException.class)
                .hasMessageContaining("Director not found: " + nonExistentId);
        verify(directorRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAll - should return all directors")
    void getAll_shouldReturnAllDirectors() {
        DirectorEntity director2 = DirectorEntity.builder()
                .id(2L)
                .tmdbId(488L)
                .name("Steven Spielberg")
                .build();
        List<DirectorEntity> directors = List.of(testDirectorEntity, director2);
        when(directorRepository.findAll()).thenReturn(directors);

        List<Director> result = directorService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(directorRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no directors exist")
    void getAll_shouldReturnEmptyList_whenNoDirectorsExist() {
        when(directorRepository.findAll()).thenReturn(List.of());

        List<Director> result = directorService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(directorRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("update - should update and return director when director exists")
    void update_shouldUpdateAndReturnDirector_whenDirectorExists() {
        Director updatedDirector = Director.builder()
                .tmdbId(138L)
                .name("Quentin Tarantino")
                .build();
        DirectorEntity savedDirector = DirectorEntity.builder()
                .id(testDirectorId)
                .tmdbId(138L)
                .name("Quentin Tarantino")
                .build();

        when(directorRepository.existsById(testDirectorId)).thenReturn(true);
        when(directorRepository.getReferenceById(testDirectorId)).thenReturn(testDirectorEntity);
        when(directorRepository.save(any(DirectorEntity.class))).thenReturn(savedDirector);

        Director result = directorService.update(testDirectorId, updatedDirector);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testDirectorId);
        assertThat(result.getName()).isEqualTo("Quentin Tarantino");
        assertThat(result.getTmdbId()).isEqualTo(138L);
        verify(directorRepository, times(1)).existsById(testDirectorId);
        verify(directorRepository, times(1)).save(any(DirectorEntity.class));
    }

    @Test
    @DisplayName("update - should set ID on director before saving")
    void update_shouldSetIdOnDirector_beforeSaving() {
        Director updatedDirector = Director.builder()
                .tmdbId(138L)
                .name("Quentin Tarantino")
                .build();

        when(directorRepository.existsById(testDirectorId)).thenReturn(true);
        when(directorRepository.getReferenceById(testDirectorId)).thenReturn(testDirectorEntity);
        when(directorRepository.save(any(DirectorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        directorService.update(testDirectorId, updatedDirector);

        verify(directorRepository, times(1)).save(any(DirectorEntity.class));
    }

    @Test
    @DisplayName("update - should throw DirectorNotFoundException when director does not exist")
    void update_shouldThrowDirectorNotFoundException_whenDirectorDoesNotExist() {
        Long nonExistentId = 999L;
        Director updatedDirector = Director.builder()
                .tmdbId(138L)
                .name("Quentin Tarantino")
                .build();

        when(directorRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> directorService.update(nonExistentId, updatedDirector))
                .isInstanceOf(DirectorNotFoundException.class)
                .hasMessageContaining("Director not found: " + nonExistentId);
        verify(directorRepository, times(1)).existsById(nonExistentId);
        verify(directorRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - should delete director when director exists")
    void deleteById_shouldDeleteDirector_whenDirectorExists() {
        when(directorRepository.existsById(testDirectorId)).thenReturn(true);
        doNothing().when(directorRepository).deleteById(testDirectorId);

        directorService.deleteById(testDirectorId);

        verify(directorRepository, times(1)).existsById(testDirectorId);
        verify(directorRepository, times(1)).deleteById(testDirectorId);
    }

    @Test
    @DisplayName("delete - should throw DirectorNotFoundException when director does not exist")
    void deleteById_shouldThrowDirectorNotFoundException_whenDirectorDoesNotExist() {
        Long nonExistentId = 999L;
        when(directorRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> directorService.deleteById(nonExistentId))
                .isInstanceOf(DirectorNotFoundException.class)
                .hasMessageContaining("Director not found: " + nonExistentId);
        verify(directorRepository, times(1)).existsById(nonExistentId);
        verify(directorRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getAll with Pageable - should return page of directors")
    void givenPageable_whenGetAll_thenReturnsPageOfDirectors() {
        Pageable pageable = PageRequest.of(0, 10);
        DirectorEntity directorEntity2 = DirectorEntity.builder()
                .id(2L)
                .tmdbId(488L)
                .name("Steven Spielberg")
                .build();
        List<DirectorEntity> entities = List.of(testDirectorEntity, directorEntity2);
        Page<DirectorEntity> entityPage = new PageImpl<>(entities, pageable, 2);

        when(directorRepository.findAll(pageable)).thenReturn(entityPage);

        Page<Director> result = directorService.getAll(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(directorRepository, times(1)).findAll(pageable);
    }
}
