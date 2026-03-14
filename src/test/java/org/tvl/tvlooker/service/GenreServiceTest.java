package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.GenreNotFoundException;
import org.tvl.tvlooker.domain.model.entity.Genre;
import org.tvl.tvlooker.persistence.repository.GenreRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GenreService.
 * Tests all CRUD operations and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GenreService Unit Tests")
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreService genreService;

    private Long testGenreId;
    private Genre testGenre;

    @BeforeEach
    void setUp() {
        testGenreId = 1L;
        testGenre = new Genre();
        testGenre.setId(testGenreId);
        testGenre.setName("Action");
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("create - should save and return genre")
    void create_shouldSaveAndReturnGenre() {
        // Arrange
        when(genreRepository.save(any(Genre.class))).thenReturn(testGenre);

        // Act
        Genre result = genreService.create(testGenre);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testGenreId);
        assertThat(result.getName()).isEqualTo("Action");
        verify(genreRepository, times(1)).save(testGenre);
    }

    // ========== GET BY ID TESTS ==========

    @Test
    @DisplayName("getById - should return genre when genre exists")
    void getById_shouldReturnGenre_whenGenreExists() {
        // Arrange
        when(genreRepository.findById(testGenreId)).thenReturn(Optional.of(testGenre));

        // Act
        Genre result = genreService.getById(testGenreId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testGenreId);
        assertThat(result.getName()).isEqualTo("Action");
        verify(genreRepository, times(1)).findById(testGenreId);
    }

    @Test
    @DisplayName("getById - should throw GenreNotFoundException when genre does not exist")
    void getById_shouldThrowGenreNotFoundException_whenGenreDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(genreRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> genreService.getById(nonExistentId))
                .isInstanceOf(GenreNotFoundException.class)
                .hasMessageContaining("Genre not found: " + nonExistentId);
        verify(genreRepository, times(1)).findById(nonExistentId);
    }

    // ========== GET ALL TESTS ==========

    @Test
    @DisplayName("getAll - should return all genres")
    void getAll_shouldReturnAllGenres() {
        // Arrange
        Genre genre2 = new Genre();
        genre2.setId(2L);
        genre2.setName("Comedy");
        List<Genre> genres = List.of(testGenre, genre2);
        when(genreRepository.findAll()).thenReturn(genres);

        // Act
        List<Genre> result = genreService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testGenre, genre2);
        verify(genreRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no genres exist")
    void getAll_shouldReturnEmptyList_whenNoGenresExist() {
        // Arrange
        when(genreRepository.findAll()).thenReturn(List.of());

        // Act
        List<Genre> result = genreService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(genreRepository, times(1)).findAll();
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("update - should update and return genre when genre exists")
    void update_shouldUpdateAndReturnGenre_whenGenreExists() {
        // Arrange
        Genre updatedGenre = new Genre();
        updatedGenre.setName("Drama");
        Genre savedGenre = new Genre();
        savedGenre.setId(testGenreId);
        savedGenre.setName("Drama");

        when(genreRepository.existsById(testGenreId)).thenReturn(true);
        when(genreRepository.save(any(Genre.class))).thenReturn(savedGenre);

        // Act
        Genre result = genreService.update(testGenreId, updatedGenre);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testGenreId);
        assertThat(result.getName()).isEqualTo("Drama");
        verify(genreRepository, times(1)).existsById(testGenreId);
        verify(genreRepository, times(1)).save(updatedGenre);
    }

    @Test
    @DisplayName("update - should set ID on genre before saving")
    void update_shouldSetIdOnGenre_beforeSaving() {
        // Arrange
        Genre updatedGenre = new Genre();
        updatedGenre.setName("Drama");

        when(genreRepository.existsById(testGenreId)).thenReturn(true);
        when(genreRepository.save(any(Genre.class))).thenReturn(updatedGenre);

        // Act
        genreService.update(testGenreId, updatedGenre);

        // Assert
        assertThat(updatedGenre.getId()).isEqualTo(testGenreId);
        verify(genreRepository, times(1)).save(updatedGenre);
    }

    @Test
    @DisplayName("update - should throw GenreNotFoundException when genre does not exist")
    void update_shouldThrowGenreNotFoundException_whenGenreDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        Genre updatedGenre = new Genre();
        updatedGenre.setName("Drama");

        when(genreRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> genreService.update(nonExistentId, updatedGenre))
                .isInstanceOf(GenreNotFoundException.class)
                .hasMessageContaining("Genre not found: " + nonExistentId);
        verify(genreRepository, times(1)).existsById(nonExistentId);
        verify(genreRepository, never()).save(any());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("deleteById - should delete genre when genre exists")
    void deleteById_shouldDeleteGenre_whenGenreExists() {
        // Arrange
        when(genreRepository.existsById(testGenreId)).thenReturn(true);
        doNothing().when(genreRepository).deleteById(testGenreId);

        // Act
        genreService.deleteById(testGenreId);

        // Assert
        verify(genreRepository, times(1)).existsById(testGenreId);
        verify(genreRepository, times(1)).deleteById(testGenreId);
    }

    @Test
    @DisplayName("deleteById - should throw GenreNotFoundException when genre does not exist")
    void deleteById_shouldThrowGenreNotFoundException_whenGenreDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(genreRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> genreService.deleteById(nonExistentId))
                .isInstanceOf(GenreNotFoundException.class)
                .hasMessageContaining("Genre not found: " + nonExistentId);
        verify(genreRepository, times(1)).existsById(nonExistentId);
        verify(genreRepository, never()).deleteById(any());
    }
}
