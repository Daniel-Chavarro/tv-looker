package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tvl.tvlooker.domain.model.entity.Genre;

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
class GenreRepositoryPersistenceTest {

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
        Genre genre = createGenre("Action");

        // When
        Genre savedGenre = genreRepository.saveAndFlush(genre);

        // Then
        assertThat(savedGenre).isNotNull();
        assertThat(savedGenre.getId()).isNotNull();
        assertThat(savedGenre.getName()).isEqualTo("Action");
    }

    @Test
    @DisplayName("Should save multiple genres")
    void testSaveMultipleGenres() {
        // Given
        Genre genre1 = createGenre("Action");
        Genre genre2 = createGenre("Comedy");
        Genre genre3 = createGenre("Drama");

        // When
        List<Genre> savedGenres = genreRepository.saveAll(List.of(genre1, genre2, genre3));

        // Then
        assertThat(savedGenres).hasSize(3);
        assertThat(genreRepository.count()).isEqualTo(3);
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find genre by ID")
    void testFindById() {
        // Given
        Genre genre = createGenre("Sci-Fi");
        Genre savedGenre = genreRepository.saveAndFlush(genre);

        // When
        Optional<Genre> foundGenre = genreRepository.findById(savedGenre.getId());

        // Then
        assertThat(foundGenre).isPresent();
        assertThat(foundGenre.get().getName()).isEqualTo("Sci-Fi");
    }

    @Test
    @DisplayName("Should find all genres")
    void testFindAll() {
        // Given
        genreRepository.saveAll(List.of(
                createGenre("Action"),
                createGenre("Comedy"),
                createGenre("Drama")
        ));

        // When
        List<Genre> allGenres = genreRepository.findAll();

        // Then
        assertThat(allGenres).hasSize(3);
    }

    @Test
    @DisplayName("Should count all genres")
    void testCount() {
        // Given
        genreRepository.saveAll(List.of(
                createGenre("Action"),
                createGenre("Comedy")
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
        Genre genre = createGenre("Old Name");
        Genre savedGenre = genreRepository.saveAndFlush(genre);

        // When
        savedGenre.setName("New Name");
        Genre updatedGenre = genreRepository.saveAndFlush(savedGenre);

        // Then
        assertThat(updatedGenre.getId()).isEqualTo(savedGenre.getId());
        assertThat(updatedGenre.getName()).isEqualTo("New Name");
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete genre by ID")
    void testDeleteById() {
        // Given
        Genre genre = createGenre("To Delete");
        Genre savedGenre = genreRepository.saveAndFlush(genre);

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
                createGenre("Action"),
                createGenre("Comedy")
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
        Genre genre = new Genre();
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

    private Genre createGenre(String name) {
        Genre genre = new Genre();
        genre.setName(name);
        return genre;
    }
}
