package org.tvl.tvlooker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.domain.exception.ActorNotFoundException;
import org.tvl.tvlooker.domain.model.Actor;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.persistence.repository.ActorRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActorService Unit Tests")
class ActorServiceTest {

    @Mock
    private ActorRepository actorRepository;

    @InjectMocks
    private ActorService actorService;

    private Long testActorId;
    private Actor testActor;
    private ActorEntity testActorEntity;

    @BeforeEach
    void setUp() {
        testActorId = 1L;
        testActor = Actor.builder()
                .id(testActorId)
                .tmdbId(6193L)
                .name("Leonardo DiCaprio")
                .build();

        testActorEntity = ActorEntity.builder()
                .id(testActorId)
                .tmdbId(6193L)
                .name("Leonardo DiCaprio")
                .build();
    }

    @Test
    @DisplayName("create - should save and return actor")
    void create_shouldSaveAndReturnActor() {
        when(actorRepository.save(any(ActorEntity.class))).thenReturn(testActorEntity);

        Actor result = actorService.create(testActor);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testActorId);
        assertThat(result.getName()).isEqualTo("Leonardo DiCaprio");
        assertThat(result.getTmdbId()).isEqualTo(6193L);
        verify(actorRepository, times(1)).save(any(ActorEntity.class));
    }

    @Test
    @DisplayName("getById - should return actor when actor exists")
    void getById_shouldReturnActor_whenActorExists() {
        when(actorRepository.findById(testActorId)).thenReturn(Optional.of(testActorEntity));

        Actor result = actorService.getById(testActorId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testActorId);
        assertThat(result.getName()).isEqualTo("Leonardo DiCaprio");
        verify(actorRepository, times(1)).findById(testActorId);
    }

    @Test
    @DisplayName("getById - should throw ActorNotFoundException when actor does not exist")
    void getById_shouldThrowActorNotFoundException_whenActorDoesNotExist() {
        Long nonExistentId = 999L;
        when(actorRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actorService.getById(nonExistentId))
                .isInstanceOf(ActorNotFoundException.class)
                .hasMessageContaining("Actor not found: " + nonExistentId);
        verify(actorRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("getAll - should return all actors")
    void getAll_shouldReturnAllActors() {
        ActorEntity actor2 = ActorEntity.builder()
                .id(2L)
                .tmdbId(31L)
                .name("Tom Hanks")
                .build();
        List<ActorEntity> actors = List.of(testActorEntity, actor2);
        when(actorRepository.findAll()).thenReturn(actors);

        List<Actor> result = actorService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(actorRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll - should return empty list when no actors exist")
    void getAll_shouldReturnEmptyList_whenNoActorsExist() {
        when(actorRepository.findAll()).thenReturn(List.of());

        List<Actor> result = actorService.getAll();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(actorRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("update - should update and return actor when actor exists")
    void update_shouldUpdateAndReturnActor_whenActorExists() {
        Actor updatedActor = Actor.builder()
                .tmdbId(287L)
                .name("Brad Pitt")
                .build();
        ActorEntity savedActor = ActorEntity.builder()
                .id(testActorId)
                .tmdbId(287L)
                .name("Brad Pitt")
                .build();

        when(actorRepository.existsById(testActorId)).thenReturn(true);
        when(actorRepository.getReferenceById(testActorId)).thenReturn(testActorEntity);
        when(actorRepository.save(any(ActorEntity.class))).thenReturn(savedActor);

        Actor result = actorService.update(testActorId, updatedActor);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testActorId);
        assertThat(result.getName()).isEqualTo("Brad Pitt");
        assertThat(result.getTmdbId()).isEqualTo(287L);
        verify(actorRepository, times(1)).existsById(testActorId);
        verify(actorRepository, times(1)).save(any(ActorEntity.class));
    }

    @Test
    @DisplayName("update - should set ID on actor before saving")
    void update_shouldSetIdOnActor_beforeSaving() {
        Actor updatedActor = Actor.builder()
                .tmdbId(287L)
                .name("Brad Pitt")
                .build();

        when(actorRepository.existsById(testActorId)).thenReturn(true);
        when(actorRepository.getReferenceById(testActorId)).thenReturn(testActorEntity);
        when(actorRepository.save(any(ActorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        actorService.update(testActorId, updatedActor);

        verify(actorRepository, times(1)).save(any(ActorEntity.class));
    }

    @Test
    @DisplayName("update - should throw ActorNotFoundException when actor does not exist")
    void update_shouldThrowActorNotFoundException_whenActorDoesNotExist() {
        Long nonExistentId = 999L;
        Actor updatedActor = Actor.builder()
                .tmdbId(287L)
                .name("Brad Pitt")
                .build();

        when(actorRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> actorService.update(nonExistentId, updatedActor))
                .isInstanceOf(ActorNotFoundException.class)
                .hasMessageContaining("Actor not found: " + nonExistentId);
        verify(actorRepository, times(1)).existsById(nonExistentId);
        verify(actorRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - should delete actor when actor exists")
    void deleteById_shouldDeleteActor_whenActorExists() {
        when(actorRepository.existsById(testActorId)).thenReturn(true);
        doNothing().when(actorRepository).deleteById(testActorId);

        actorService.deleteById(testActorId);

        verify(actorRepository, times(1)).existsById(testActorId);
        verify(actorRepository, times(1)).deleteById(testActorId);
    }

    @Test
    @DisplayName("delete - should throw ActorNotFoundException when actor does not exist")
    void deleteById_shouldThrowActorNotFoundException_whenActorDoesNotExist() {
        Long nonExistentId = 999L;
        when(actorRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> actorService.deleteById(nonExistentId))
                .isInstanceOf(ActorNotFoundException.class)
                .hasMessageContaining("Actor not found: " + nonExistentId);
        verify(actorRepository, times(1)).existsById(nonExistentId);
        verify(actorRepository, never()).deleteById(any());
    }
}
