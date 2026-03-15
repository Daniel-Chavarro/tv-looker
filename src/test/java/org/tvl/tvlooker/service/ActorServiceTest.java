package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.ActorNotFoundException;
import org.tvl.tvlooker.domain.model.entity.Actor;
import org.tvl.tvlooker.persistence.repository.ActorRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ActorService.
 * Tests all CRUD operations and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActorService Unit Tests")
class ActorServiceTest {

    @Mock
    private ActorRepository actorRepository;

    @InjectMocks
    private ActorService actorService;

    private Long testActorId;
    private Actor testActor;

    @BeforeEach
    void setUp() {
        testActorId = 1L;
        testActor = new Actor();
        testActor.setId(testActorId);
        testActor.setName("Leonardo DiCaprio");
        testActor.setTmdbId(6193L);
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("create - should save and return actor")
    void create_shouldSaveAndReturnActor() {
        // Arrange
        when(actorRepository.save(any(Actor.class))).thenReturn(testActor);

        // Act
        Actor result = actorService.create(testActor);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testActorId);
        assertThat(result.getName()).isEqualTo("Leonardo DiCaprio");
        assertThat(result.getTmdbId()).isEqualTo(6193L);
        verify(actorRepository, times(1)).save(testActor);
    }

    // ========== GET BY ID TESTS ==========

    @Test
    @DisplayName("getById - should return actor when actor exists")
    void getById_shouldReturnActor_whenActorExists() {
        // Arrange
        when(actorRepository.findById(testActorId)).thenReturn(Optional.of(testActor));

        // Act
        Actor result = actorService.getById(testActorId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testActorId);
        assertThat(result.getName()).isEqualTo("Leonardo DiCaprio");
        verify(actorRepository, times(1)).findById(testActorId);
    }

    @Test
    @DisplayName("getById - should throw ActorNotFoundException when actor does not exist")
    void getById_shouldThrowActorNotFoundException_whenActorDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(actorRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> actorService.getById(nonExistentId))
                .isInstanceOf(ActorNotFoundException.class)
                .hasMessageContaining("Actor not found: " + nonExistentId);
        verify(actorRepository, times(1)).findById(nonExistentId);
    }

    // ========== GET ALL TESTS ==========

    @Test
    @DisplayName("getAll - should return all actors")
    void getAll_shouldReturnAllActors() {
        // Arrange
        Actor actor2 = new Actor();
        actor2.setId(2L);
        actor2.setName("Tom Hanks");
        actor2.setTmdbId(31L);
        List<Actor> actors = List.of(testActor, actor2);
        when(actorRepository.findAll()).thenReturn(actors);

        // Act
        List<Actor> result = actorService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testActor, actor2);
        verify(actorRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no actors exist")
    void getAll_shouldReturnEmptyList_whenNoActorsExist() {
        // Arrange
        when(actorRepository.findAll()).thenReturn(List.of());

        // Act
        List<Actor> result = actorService.getAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(actorRepository, times(1)).findAll();
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("update - should update and return actor when actor exists")
    void update_shouldUpdateAndReturnActor_whenActorExists() {
        // Arrange
        Actor updatedActor = new Actor();
        updatedActor.setName("Brad Pitt");
        updatedActor.setTmdbId(287L);
        Actor savedActor = new Actor();
        savedActor.setId(testActorId);
        savedActor.setName("Brad Pitt");
        savedActor.setTmdbId(287L);

        when(actorRepository.existsById(testActorId)).thenReturn(true);
        when(actorRepository.save(any(Actor.class))).thenReturn(savedActor);

        // Act
        Actor result = actorService.update(testActorId, updatedActor);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testActorId);
        assertThat(result.getName()).isEqualTo("Brad Pitt");
        assertThat(result.getTmdbId()).isEqualTo(287L);
        verify(actorRepository, times(1)).existsById(testActorId);
        verify(actorRepository, times(1)).save(updatedActor);
    }

    @Test
    @DisplayName("update - should set ID on actor before saving")
    void update_shouldSetIdOnActor_beforeSaving() {
        // Arrange
        Actor updatedActor = new Actor();
        updatedActor.setName("Brad Pitt");
        updatedActor.setTmdbId(287L);

        when(actorRepository.existsById(testActorId)).thenReturn(true);
        when(actorRepository.save(any(Actor.class))).thenReturn(updatedActor);

        // Act
        actorService.update(testActorId, updatedActor);

        // Assert
        assertThat(updatedActor.getId()).isEqualTo(testActorId);
        verify(actorRepository, times(1)).save(updatedActor);
    }

    @Test
    @DisplayName("update - should throw ActorNotFoundException when actor does not exist")
    void update_shouldThrowActorNotFoundException_whenActorDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        Actor updatedActor = new Actor();
        updatedActor.setName("Brad Pitt");
        updatedActor.setTmdbId(287L);

        when(actorRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> actorService.update(nonExistentId, updatedActor))
                .isInstanceOf(ActorNotFoundException.class)
                .hasMessageContaining("Actor not found: " + nonExistentId);
        verify(actorRepository, times(1)).existsById(nonExistentId);
        verify(actorRepository, never()).save(any());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("deleteById - should delete actor when actor exists")
    void deleteById_shouldDeleteActor_whenActorExists() {
        // Arrange
        when(actorRepository.existsById(testActorId)).thenReturn(true);
        doNothing().when(actorRepository).deleteById(testActorId);

        // Act
        actorService.deleteById(testActorId);

        // Assert
        verify(actorRepository, times(1)).existsById(testActorId);
        verify(actorRepository, times(1)).deleteById(testActorId);
    }

    @Test
    @DisplayName("deleteById - should throw ActorNotFoundException when actor does not exist")
    void deleteById_shouldThrowActorNotFoundException_whenActorDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(actorRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> actorService.deleteById(nonExistentId))
                .isInstanceOf(ActorNotFoundException.class)
                .hasMessageContaining("Actor not found: " + nonExistentId);
        verify(actorRepository, times(1)).existsById(nonExistentId);
        verify(actorRepository, never()).deleteById(any());
    }
}
