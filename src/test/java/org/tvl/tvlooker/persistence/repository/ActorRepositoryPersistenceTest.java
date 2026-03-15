package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tvl.tvlooker.domain.model.entity.Actor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * TDD Test class for ActorRepository.
 * Tests cover CRUD operations and unique constraints.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-11
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ActorRepository TDD Tests")
class ActorRepositoryPersistenceTest {

    @Autowired
    private ActorRepository actorRepository;

    @AfterEach
    void tearDown() {
        actorRepository.deleteAll();
    }

    // ==================== CREATE/SAVE TESTS ====================

    @Test
    @DisplayName("Should save a new actor")
    void testSaveActor() {
        // Given
        Actor actor = createActor(1L, "John Doe");

        // When
        Actor savedActor = actorRepository.saveAndFlush(actor);

        // Then
        assertThat(savedActor).isNotNull();
        assertThat(savedActor.getId()).isNotNull();
        assertThat(savedActor.getName()).isEqualTo("John Doe");
        assertThat(savedActor.getTmdbId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should save multiple actors")
    void testSaveMultipleActors() {
        // Given
        Actor actor1 = createActor(1L, "Actor One");
        Actor actor2 = createActor(2L, "Actor Two");
        Actor actor3 = createActor(3L, "Actor Three");

        // When
        List<Actor> savedActors = actorRepository.saveAll(List.of(actor1, actor2, actor3));

        // Then
        assertThat(savedActors).hasSize(3);
        assertThat(actorRepository.count()).isEqualTo(3);
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find actor by ID")
    void testFindById() {
        // Given
        Actor actor = createActor(100L, "Jane Smith");
        Actor savedActor = actorRepository.saveAndFlush(actor);

        // When
        Optional<Actor> foundActor = actorRepository.findById(savedActor.getId());

        // Then
        assertThat(foundActor).isPresent();
        assertThat(foundActor.get().getName()).isEqualTo("Jane Smith");
    }

    @Test
    @DisplayName("Should find all actors")
    void testFindAll() {
        // Given
        actorRepository.saveAll(List.of(
                createActor(1L, "Actor One"),
                createActor(2L, "Actor Two"),
                createActor(3L, "Actor Three")
        ));

        // When
        List<Actor> allActors = actorRepository.findAll();

        // Then
        assertThat(allActors).hasSize(3);
    }

    @Test
    @DisplayName("Should count all actors")
    void testCount() {
        // Given
        actorRepository.saveAll(List.of(
                createActor(1L, "Actor One"),
                createActor(2L, "Actor Two")
        ));

        // When
        long count = actorRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update actor name")
    void testUpdateActor() {
        // Given
        Actor actor = createActor(1L, "Old Name");
        Actor savedActor = actorRepository.saveAndFlush(actor);

        // When
        savedActor.setName("New Name");
        Actor updatedActor = actorRepository.saveAndFlush(savedActor);

        // Then
        assertThat(updatedActor.getId()).isEqualTo(savedActor.getId());
        assertThat(updatedActor.getName()).isEqualTo("New Name");
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete actor by ID")
    void testDeleteById() {
        // Given
        Actor actor = createActor(1L, "To Delete");
        Actor savedActor = actorRepository.saveAndFlush(actor);

        // When
        actorRepository.deleteById(savedActor.getId());

        // Then
        assertThat(actorRepository.findById(savedActor.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete all actors")
    void testDeleteAll() {
        // Given
        actorRepository.saveAll(List.of(
                createActor(1L, "Actor One"),
                createActor(2L, "Actor Two")
        ));

        // When
        actorRepository.deleteAll();

        // Then
        assertThat(actorRepository.count()).isZero();
    }

    // ==================== CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should not allow null name")
    void testNullName() {
        // Given
        Actor actor = createActor(1L, null);

        // When & Then
        try {
            actorRepository.saveAndFlush(actor);
            fail("Should have thrown exception for null name");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should not allow null tmdbId")
    void testNullTmdbId() {
        // Given
        Actor actor = new Actor();
        actor.setName("Test Actor");
        actor.setTmdbId(null);

        // When & Then
        try {
            actorRepository.saveAndFlush(actor);
            fail("Should have thrown exception for null tmdbId");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should enforce unique tmdbId constraint")
    void testUniqueTmdbId() {
        // Given
        Actor actor1 = createActor(1L, "Actor One");
        actorRepository.saveAndFlush(actor1);

        Actor actor2 = createActor(1L, "Actor Two");

        // When & Then
        try {
            actorRepository.saveAndFlush(actor2);
            fail("Should have thrown exception for duplicate tmdbId");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    // ==================== HELPER METHODS ====================

    private Actor createActor(Long tmdbId, String name) {
        Actor actor = new Actor();
        actor.setTmdbId(tmdbId);
        actor.setName(name);
        return actor;
    }
}
