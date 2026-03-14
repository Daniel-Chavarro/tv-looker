package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.DirectorNotFoundException;
import org.tvl.tvlooker.domain.model.entity.Director;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DirectorService.
 * Tests all CRUD operations and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DirectorService Unit Tests")
class DirectorServiceTest {

    @Mock
    private DirectorRepository directorRepository;

    @InjectMocks
    private DirectorService directorService;

    private Long testDirectorId;
    private Director testDirector;

    @BeforeEach
    void setUp() {
        testDirectorId = 1L;
        testDirector = new Director();
        testDirector.setId(testDirectorId);
        testDirector.setName("Christopher Nolan");
        testDirector.setTmdbId(525L);
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("create - should save and return director")
    void create_shouldSaveAndReturnDirector() {
        // Arrange
        when(directorRepository.save(any(Director.class))).thenReturn(testDirector);

        // Act
        Director result = directorService.create(testDirector);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testDirectorId);
        assertThat(result.getName()).isEqualTo("Christopher Nolan");
        assertThat(result.getTmdbId()).isEqualTo(525L);
        verify(directorRepository, times(1)).save(testDirector);
    }

    // ========== GET BY ID TESTS ==========

    @Test
    @DisplayName("getById - should return director when director exists")
    void getById_shouldReturnDirector_whenDirectorExists() {
        // Arrange
        when(directorRepository.findById(testDirectorId)).thenReturn(Optional.of(testDirector));

        // Act
        Director result = directorService.getById(testDirectorId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testDirectorId);
        assertThat(result.getName()).isEqualTo("Christopher Nolan");
        verify(directorRepository, times(1)).findById(testDirectorId);
    }

    @Test
    @DisplayName("getById - should throw DirectorNotFoundException when director does not exist")
    void getById_shouldThrowDirectorNotFoundException_whenDirectorDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(directorRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> directorService.getById(nonExistentId))
                .isInstanceOf(DirectorNotFoundException.class)
                .hasMessageContaining("Director not found: " + nonExistentId);
        verify(directorRepository, times(1)).findById(nonExistentId);
    }

    // ========== GET ALL TESTS ==========

    @Test
    @DisplayName("getAll - should return all directors")
    void getAll_shouldReturnAllDirectors() {
        // Arrange
        Director director2 = new Director();
        director2.setId(2L);
        director2.setName("Steven Spielberg");
        director2.setTmdbId(488L);
        List<Director> directors = List.of(testDirector, director2);
        when(directorRepository.findAll()).thenReturn(directors);

        // Act
        List<Director> result = directorService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testDirector, director2);
        verify(directorRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no directors exist")
    void getAll_shouldReturnEmptyList_whenNoDirectorsExist() {
        // Arrange
        when(directorRepository.findAll()).thenReturn(List.of());

        // Act
        List<Director> result = directorService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(directorRepository, times(1)).findAll();
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("update - should update and return director when director exists")
    void update_shouldUpdateAndReturnDirector_whenDirectorExists() {
        // Arrange
        Director updatedDirector = new Director();
        updatedDirector.setName("Quentin Tarantino");
        updatedDirector.setTmdbId(138L);
        Director savedDirector = new Director();
        savedDirector.setId(testDirectorId);
        savedDirector.setName("Quentin Tarantino");
        savedDirector.setTmdbId(138L);

        when(directorRepository.existsById(testDirectorId)).thenReturn(true);
        when(directorRepository.save(any(Director.class))).thenReturn(savedDirector);

        // Act
        Director result = directorService.update(testDirectorId, updatedDirector);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testDirectorId);
        assertThat(result.getName()).isEqualTo("Quentin Tarantino");
        assertThat(result.getTmdbId()).isEqualTo(138L);
        verify(directorRepository, times(1)).existsById(testDirectorId);
        verify(directorRepository, times(1)).save(updatedDirector);
    }

    @Test
    @DisplayName("update - should set ID on director before saving")
    void update_shouldSetIdOnDirector_beforeSaving() {
        // Arrange
        Director updatedDirector = new Director();
        updatedDirector.setName("Quentin Tarantino");
        updatedDirector.setTmdbId(138L);

        when(directorRepository.existsById(testDirectorId)).thenReturn(true);
        when(directorRepository.save(any(Director.class))).thenReturn(updatedDirector);

        // Act
        directorService.update(testDirectorId, updatedDirector);

        // Assert
        assertThat(updatedDirector.getId()).isEqualTo(testDirectorId);
        verify(directorRepository, times(1)).save(updatedDirector);
    }

    @Test
    @DisplayName("update - should throw DirectorNotFoundException when director does not exist")
    void update_shouldThrowDirectorNotFoundException_whenDirectorDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        Director updatedDirector = new Director();
        updatedDirector.setName("Quentin Tarantino");
        updatedDirector.setTmdbId(138L);

        when(directorRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> directorService.update(nonExistentId, updatedDirector))
                .isInstanceOf(DirectorNotFoundException.class)
                .hasMessageContaining("Director not found: " + nonExistentId);
        verify(directorRepository, times(1)).existsById(nonExistentId);
        verify(directorRepository, never()).save(any());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("deleteById - should delete director when director exists")
    void deleteById_shouldDeleteDirector_whenDirectorExists() {
        // Arrange
        when(directorRepository.existsById(testDirectorId)).thenReturn(true);
        doNothing().when(directorRepository).deleteById(testDirectorId);

        // Act
        directorService.deleteById(testDirectorId);

        // Assert
        verify(directorRepository, times(1)).existsById(testDirectorId);
        verify(directorRepository, times(1)).deleteById(testDirectorId);
    }

    @Test
    @DisplayName("deleteById - should throw DirectorNotFoundException when director does not exist")
    void deleteById_shouldThrowDirectorNotFoundException_whenDirectorDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(directorRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> directorService.deleteById(nonExistentId))
                .isInstanceOf(DirectorNotFoundException.class)
                .hasMessageContaining("Director not found: " + nonExistentId);
        verify(directorRepository, times(1)).existsById(nonExistentId);
        verify(directorRepository, never()).deleteById(any());
    }
}
