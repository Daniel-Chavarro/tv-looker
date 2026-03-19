package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.GenreNotFoundException;
import org.tvl.tvlooker.domain.model.Genre;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.persistence.repository.GenreRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenreService Unit Tests")
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreService genreService;

    private Long testGenreId;
    private Genre testGenre;
    private GenreEntity testGenreEntity;

    @BeforeEach
    void setUp() {
        testGenreId = 1L;
        testGenre = Genre.builder()
                .id(testGenreId)
                .tmdbId(28L)
                .name("Action")
                .build();

        testGenreEntity = GenreEntity.builder()
                .id(testGenreId)
                .tmdbId(28L)
                .name("Action")
                .build();
    }

    @Test
    @DisplayName("create - should save and return genre")
    void create_shouldSaveAndReturnGenre() {
        when(genreRepository.save(any(GenreEntity.class))).thenReturn(testGenreEntity);

        Genre result = genreService.create(testGenre);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testGenreId);
        assertThat(result.getName()).isEqualTo("Action");
        verify(genreRepository, times(1)).save(any(GenreEntity.class));
    }

    @Test
    @DisplayName("getById - should return genre when genre exists")
    void getById_shouldReturnGenre_whenGenreExists() {
        when(genreRepository.findById(testGenreId)).thenReturn(Optional.of(testGenreEntity));

        Genre result = genreService.getById(testGenreId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testGenreId);
        assertThat(result.getName()).isEqualTo("Action");
        verify(genreRepository, times(1)).findById(testGenreId);
    }

    @Test
    @DisplayName("getById - should throw GenreNotFoundException when genre does not exist")
    void getById_shouldThrowGenreNotFoundException_whenGenreDoesNotExist() {
        Long nonExistentId = 999L;
        when(genreRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.getById(nonExistentId))
                .isInstanceOf(GenreNotFoundException.class)
                .hasMessageContaining("Genre not found: " + nonExistentId);
        verify(genreRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAll - should return all genres")
    void getAll_shouldReturnAllGenres() {
        GenreEntity genre2 = GenreEntity.builder()
                .id(2L)
                .tmdbId(35L)
                .name("Comedy")
                .build();
        List<GenreEntity> genres = List.of(testGenreEntity, genre2);
        when(genreRepository.findAll()).thenReturn(genres);

        List<Genre> result = genreService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(genreRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no genres exist")
    void getAll_shouldReturnEmptyList_whenNoGenresExist() {
        when(genreRepository.findAll()).thenReturn(List.of());

        List<Genre> result = genreService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(genreRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("update - should update and return genre when genre exists")
    void update_shouldUpdateAndReturnGenre_whenGenreExists() {
        Genre updatedGenre = Genre.builder()
                .tmdbId(18L)
                .name("Drama")
                .build();
        GenreEntity savedGenre = GenreEntity.builder()
                .id(testGenreId)
                .tmdbId(18L)
                .name("Drama")
                .build();

        when(genreRepository.existsById(testGenreId)).thenReturn(true);
        when(genreRepository.getReferenceById(testGenreId)).thenReturn(testGenreEntity);
        when(genreRepository.save(any(GenreEntity.class))).thenReturn(savedGenre);

        Genre result = genreService.update(testGenreId, updatedGenre);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testGenreId);
        assertThat(result.getName()).isEqualTo("Drama");
        verify(genreRepository, times(1)).existsById(testGenreId);
        verify(genreRepository, times(1)).save(any(GenreEntity.class));
    }

    @Test
    @DisplayName("update - should set ID on genre before saving")
    void update_shouldSetIdOnGenre_beforeSaving() {
        Genre updatedGenre = Genre.builder()
                .tmdbId(18L)
                .name("Drama")
                .build();

        when(genreRepository.existsById(testGenreId)).thenReturn(true);
        when(genreRepository.getReferenceById(testGenreId)).thenReturn(testGenreEntity);
        when(genreRepository.save(any(GenreEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        genreService.update(testGenreId, updatedGenre);

        verify(genreRepository, times(1)).save(any(GenreEntity.class));
    }

    @Test
    @DisplayName("update - should throw GenreNotFoundException when genre does not exist")
    void update_shouldThrowGenreNotFoundException_whenGenreDoesNotExist() {
        Long nonExistentId = 999L;
        Genre updatedGenre = Genre.builder()
                .tmdbId(18L)
                .name("Drama")
                .build();

        when(genreRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> genreService.update(nonExistentId, updatedGenre))
                .isInstanceOf(GenreNotFoundException.class)
                .hasMessageContaining("Genre not found: " + nonExistentId);
        verify(genreRepository, times(1)).existsById(nonExistentId);
        verify(genreRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - should delete genre when genre exists")
    void deleteById_shouldDeleteGenre_whenGenreExists() {
        when(genreRepository.existsById(testGenreId)).thenReturn(true);
        doNothing().when(genreRepository).deleteById(testGenreId);

        genreService.deleteById(testGenreId);

        verify(genreRepository, times(1)).existsById(testGenreId);
        verify(genreRepository, times(1)).deleteById(testGenreId);
    }

    @Test
    @DisplayName("delete - should throw GenreNotFoundException when genre does not exist")
    void deleteById_shouldThrowGenreNotFoundException_whenGenreDoesNotExist() {
        Long nonExistentId = 999L;
        when(genreRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> genreService.deleteById(nonExistentId))
                .isInstanceOf(GenreNotFoundException.class)
                .hasMessageContaining("Genre not found: " + nonExistentId);
        verify(genreRepository, times(1)).existsById(nonExistentId);
        verify(genreRepository, never()).deleteById(any());
    }
}
