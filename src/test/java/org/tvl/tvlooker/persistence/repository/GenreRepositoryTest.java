package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * TDD Test class for GenreRepository.
 * Tests cover CRUD operations.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-11
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("GenreRepository TDD Tests")
class GenreRepositoryTest {

    @Autowired
    private GenreRepository genreRepository;

    @AfterEach
    void tearDown() {
        genreRepository.deleteAll();
    }

    // ==================== CREATE/SAVE TESTS ====================

    @Test
    @DisplayName("Should save a new genre")
    void testSaveGenre() {
        // Given
        GenreEntity genre = createGenreEntity("Action", 1L);

        // When
        GenreEntity savedGenre = genreRepository.saveAndFlush(genre);

        // Then
        assertThat(savedGenre).isNotNull();
        assertThat(savedGenre.getId()).isNotNull();
        assertThat(savedGenre.getName()).isEqualTo("Action");
        assertThat(savedGenre.getTmdbId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should save multiple genres")
    void testSaveMultipleGenres() {
        // Given
        GenreEntity genre1 = createGenreEntity("Action", 1L);
        GenreEntity genre2 = createGenreEntity("Comedy", 2L);
        GenreEntity genre3 = createGenreEntity("Drama", 3L);

        // When
        List<GenreEntity> savedGenres = genreRepository.saveAll(List.of(genre1, genre2, genre3));

        // Then
        assertThat(savedGenres).hasSize(3);
        assertThat(genreRepository.count()).isEqualTo(3);
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find genre by ID")
    void testFindById() {
        // Given
        GenreEntity genre = createGenreEntity("Sci-Fi", 1L);
        GenreEntity savedGenre = genreRepository.saveAndFlush(genre);

        // When
        Optional<GenreEntity> foundGenre = genreRepository.findById(savedGenre.getId());

        // Then
        assertThat(foundGenre).isPresent();
        assertThat(foundGenre.get().getName()).isEqualTo("Sci-Fi");
    }

    @Test
    @DisplayName("Should find all genres")
    void testFindAll() {
        // Given
        genreRepository.saveAll(List.of(
                createGenreEntity("Action", 1L),
                createGenreEntity("Comedy", 2L),
                createGenreEntity("Drama", 3L)
        ));

        // When
        List<GenreEntity> allGenres = genreRepository.findAll();

        // Then
        assertThat(allGenres).hasSize(3);
    }

    @Test
    @DisplayName("Should count all genres")
    void testCount() {
        // Given
        genreRepository.saveAll(List.of(
                createGenreEntity("Action", 1L),
                createGenreEntity("Comedy", 2L)
        ));

        // When
        long count = genreRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update genre name")
    void testUpdateGenre() {
        // Given
        GenreEntity genre = createGenreEntity("Old Name", 1L);
        GenreEntity savedGenre = genreRepository.saveAndFlush(genre);

        // When
        savedGenre.setName("New Name");
        GenreEntity updatedGenre = genreRepository.saveAndFlush(savedGenre);

        // Then
        assertThat(updatedGenre.getId()).isEqualTo(savedGenre.getId());
        assertThat(updatedGenre.getName()).isEqualTo("New Name");
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete genre by ID")
    void testDeleteById() {
        // Given
        GenreEntity genre = createGenreEntity("To Delete", 1L);
        GenreEntity savedGenre = genreRepository.saveAndFlush(genre);

        // When
        genreRepository.deleteById(savedGenre.getId());

        // Then
        assertThat(genreRepository.findById(savedGenre.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete all genres")
    void testDeleteAll() {
        // Given
        genreRepository.saveAll(List.of(
                createGenreEntity("Action", 1L),
                createGenreEntity("Comedy", 2L)
        ));

        // When
        genreRepository.deleteAll();

        // Then
        assertThat(genreRepository.count()).isZero();
    }

    // ==================== CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should not allow null name")
    void testNullName() {
        // Given
        GenreEntity genre = new GenreEntity();
        genre.setName(null);

        // When & Then
        try {
            genreRepository.saveAndFlush(genre);
            fail("Should have thrown exception for null name");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    // ==================== HELPER METHODS ====================

    private GenreEntity createGenreEntity(String name, Long tmdbId) {
        GenreEntity genre = new GenreEntity();
        genre.setName(name);
        genre.setTmdbId(tmdbId);
        return genre;
    }
}
