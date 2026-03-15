package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tvl.tvlooker.domain.model.entity.Director;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * TDD Test class for DirectorRepository.
 * Tests cover CRUD operations and unique constraints.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-11
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DirectorRepository TDD Tests")
class DirectorRepositoryTest {

    @Autowired
    private DirectorRepository directorRepository;

    @BeforeEach
    void setUp() {
        directorRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        directorRepository.deleteAll();
    }

    // ==================== CREATE/SAVE TESTS ====================

    @Test
    @DisplayName("Should save a new director")
    void testSaveDirector() {
        // Given
        Director director = createDirector(1L, "Christopher Nolan");

        // When
        Director savedDirector = directorRepository.saveAndFlush(director);

        // Then
        assertThat(savedDirector).isNotNull();
        assertThat(savedDirector.getId()).isNotNull();
        assertThat(savedDirector.getName()).isEqualTo("Christopher Nolan");
        assertThat(savedDirector.getTmdbId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should save multiple directors")
    void testSaveMultipleDirectors() {
        // Given
        Director director1 = createDirector(1L, "Director One");
        Director director2 = createDirector(2L, "Director Two");
        Director director3 = createDirector(3L, "Director Three");

        // When
        List<Director> savedDirectors = directorRepository.saveAll(List.of(director1, director2, director3));

        // Then
        assertThat(savedDirectors).hasSize(3);
        assertThat(directorRepository.count()).isEqualTo(3);
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find director by ID")
    void testFindById() {
        // Given
        Director director = createDirector(100L, "Steven Spielberg");
        Director savedDirector = directorRepository.saveAndFlush(director);

        // When
        Optional<Director> foundDirector = directorRepository.findById(savedDirector.getId());

        // Then
        assertThat(foundDirector).isPresent();
        assertThat(foundDirector.get().getName()).isEqualTo("Steven Spielberg");
    }

    @Test
    @DisplayName("Should find all directors")
    void testFindAll() {
        // Given
        directorRepository.saveAll(List.of(
                createDirector(1L, "Director One"),
                createDirector(2L, "Director Two"),
                createDirector(3L, "Director Three")
        ));

        // When
        List<Director> allDirectors = directorRepository.findAll();

        // Then
        assertThat(allDirectors).hasSize(3);
    }

    @Test
    @DisplayName("Should count all directors")
    void testCount() {
        // Given
        directorRepository.saveAll(List.of(
                createDirector(1L, "Director One"),
                createDirector(2L, "Director Two")
        ));

        // When
        long count = directorRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update director name")
    void testUpdateDirector() {
        // Given
        Director director = createDirector(1L, "Old Name");
        Director savedDirector = directorRepository.saveAndFlush(director);

        // When
        savedDirector.setName("New Name");
        Director updatedDirector = directorRepository.saveAndFlush(savedDirector);

        // Then
        assertThat(updatedDirector.getId()).isEqualTo(savedDirector.getId());
        assertThat(updatedDirector.getName()).isEqualTo("New Name");
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete director by ID")
    void testDeleteById() {
        // Given
        Director director = createDirector(1L, "To Delete");
        Director savedDirector = directorRepository.saveAndFlush(director);

        // When
        directorRepository.deleteById(savedDirector.getId());

        // Then
        assertThat(directorRepository.findById(savedDirector.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete all directors")
    void testDeleteAll() {
        // Given
        directorRepository.saveAll(List.of(
                createDirector(1L, "Director One"),
                createDirector(2L, "Director Two")
        ));

        // When
        directorRepository.deleteAll();

        // Then
        assertThat(directorRepository.count()).isZero();
    }

    // ==================== CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should not allow null name")
    void testNullName() {
        // Given
        Director director = createDirector(1L, null);

        // When & Then
        try {
            directorRepository.saveAndFlush(director);
            fail("Should have thrown exception for null name");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should not allow null tmdbId")
    void testNullTmdbId() {
        // Given
        Director director = new Director();
        director.setName("Test Director");
        director.setTmdbId(null);

        // When & Then
        try {
            directorRepository.saveAndFlush(director);
            fail("Should have thrown exception for null tmdbId");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should enforce unique tmdbId constraint")
    void testUniqueTmdbId() {
        // Given
        Director director1 = createDirector(1L, "Director One");
        directorRepository.saveAndFlush(director1);

        Director director2 = createDirector(1L, "Director Two");

        // When & Then
        try {
            directorRepository.saveAndFlush(director2);
            fail("Should have thrown exception for duplicate tmdbId");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    // ==================== HELPER METHODS ====================

    private Director createDirector(Long tmdbId, String name) {
        Director director = new Director();
        director.setTmdbId(tmdbId);
        director.setName(name);
        return director;
    }
}
