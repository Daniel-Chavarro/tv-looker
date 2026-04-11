package org.tvl.tvlooker.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tvl.tvlooker.api.exception.GlobalExceptionHandler;
import org.tvl.tvlooker.domain.exception.ActorNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Actor;
import org.tvl.tvlooker.service.ActorService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ActorControllerTest {

    @Mock
    private ActorService actorService;

    @InjectMocks
    private ActorController actorController;

    private MockMvc mockMvc;

    private Actor testActor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(actorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testActor = Actor.builder()
                .id(1L)
                .tmdbId(100L)
                .name("Leonardo DiCaprio")
                .build();
    }

    @Nested
    class GetAllActors {

        @Test
        void givenActorsExist_whenGetAllActors_thenReturnsActorList() throws Exception {
            Actor secondActor = Actor.builder()
                    .id(2L)
                    .tmdbId(200L)
                    .name("Tom Hanks")
                    .build();
            Actor thirdActor = Actor.builder()
                    .id(3L)
                    .tmdbId(300L)
                    .name("Brad Pitt")
                    .build();
            List<Actor> actors = Arrays.asList(testActor, secondActor, thirdActor);

            when(actorService.getAll()).thenReturn(actors);

            mockMvc.perform(get("/api/v1/actors"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("Leonardo DiCaprio")))
                    .andExpect(jsonPath("$[0].tmdbId", is(100)))
                    .andExpect(jsonPath("$[1].name", is("Tom Hanks")))
                    .andExpect(jsonPath("$[2].name", is("Brad Pitt")));

            verify(actorService, times(1)).getAll();
        }

        @Test
        void givenNoActors_whenGetAllActors_thenReturnsEmptyList() throws Exception {
            when(actorService.getAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/actors"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(actorService, times(1)).getAll();
        }
    }

    @Nested
    class GetActorById {

        @Test
        void givenActorExists_whenGetActorById_thenReturnsActor() throws Exception {
            when(actorService.getById(1L)).thenReturn(testActor);

            mockMvc.perform(get("/api/v1/actors/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Leonardo DiCaprio")))
                    .andExpect(jsonPath("$.tmdbId", is(100)));

            verify(actorService, times(1)).getById(1L);
        }

        @Test
        void givenActorNotExists_whenGetActorById_thenReturns404() throws Exception {
            when(actorService.getById(999L))
                    .thenThrow(new ActorNotFoundException("Actor not found with id: 999"));

            mockMvc.perform(get("/api/v1/actors/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Actor Not Found")))
                    .andExpect(jsonPath("$.body.detail", containsString("Actor not found with id: 999")));

            verify(actorService, times(1)).getById(999L);
        }

        @Test
        void givenHanksActor_whenGetActorById_thenReturnsActor() throws Exception {
            Actor hanksActor = Actor.builder()
                    .id(2L)
                    .tmdbId(200L)
                    .name("Tom Hanks")
                    .build();

            when(actorService.getById(2L)).thenReturn(hanksActor);

            mockMvc.perform(get("/api/v1/actors/{id}", 2L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.name", is("Tom Hanks")))
                    .andExpect(jsonPath("$.tmdbId", is(200)));

            verify(actorService, times(1)).getById(2L);
        }
    }
}
