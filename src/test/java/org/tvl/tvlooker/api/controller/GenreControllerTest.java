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
import org.tvl.tvlooker.domain.exception.GenreNotFoundException;
import org.tvl.tvlooker.domain.model.Genre;
import org.tvl.tvlooker.service.GenreService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GenreControllerTest {

    @Mock
    private GenreService genreService;

    @InjectMocks
    private GenreController genreController;

    private MockMvc mockMvc;

    private Genre testGenre;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(genreController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testGenre = Genre.builder()
                .id(1L)
                .tmdbId(28L)
                .name("Action")
                .build();
    }

    @Nested
    class GetAllGenres {

        @Test
        void givenGenresExist_whenGetAllGenres_thenReturnsGenreList() throws Exception {
            Genre secondGenre = Genre.builder()
                    .id(2L)
                    .tmdbId(12L)
                    .name("Adventure")
                    .build();
            Genre thirdGenre = Genre.builder()
                    .id(3L)
                    .tmdbId(35L)
                    .name("Comedy")
                    .build();
            List<Genre> genres = Arrays.asList(testGenre, secondGenre, thirdGenre);

            when(genreService.getAll()).thenReturn(genres);

            mockMvc.perform(get("/api/v1/genres"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("Action")))
                    .andExpect(jsonPath("$[0].tmdbId", is(28)))
                    .andExpect(jsonPath("$[1].name", is("Adventure")))
                    .andExpect(jsonPath("$[2].name", is("Comedy")));

            verify(genreService, times(1)).getAll();
        }

        @Test
        void givenNoGenres_whenGetAllGenres_thenReturnsEmptyList() throws Exception {
            when(genreService.getAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/genres"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(genreService, times(1)).getAll();
        }
    }

    @Nested
    class GetGenreById {

        @Test
        void givenGenreExists_whenGetGenreById_thenReturnsGenre() throws Exception {
            when(genreService.getById(1L)).thenReturn(testGenre);

            mockMvc.perform(get("/api/v1/genres/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Action")))
                    .andExpect(jsonPath("$.tmdbId", is(28)));

            verify(genreService, times(1)).getById(1L);
        }

        @Test
        void givenGenreNotExists_whenGetGenreById_thenReturns404() throws Exception {
            when(genreService.getById(999L))
                    .thenThrow(new GenreNotFoundException("Genre not found with id: 999"));

            mockMvc.perform(get("/api/v1/genres/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Genre Not Found")))
                    .andExpect(jsonPath("$.body.detail", containsString("Genre not found with id: 999")));

            verify(genreService, times(1)).getById(999L);
        }

        @Test
        void givenDramaGenre_whenGetGenreById_thenReturnsDrama() throws Exception {
            Genre dramaGenre = Genre.builder()
                    .id(4L)
                    .tmdbId(18L)
                    .name("Drama")
                    .build();

            when(genreService.getById(4L)).thenReturn(dramaGenre);

            mockMvc.perform(get("/api/v1/genres/{id}", 4L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(4)))
                    .andExpect(jsonPath("$.name", is("Drama")))
                    .andExpect(jsonPath("$.tmdbId", is(18)));

            verify(genreService, times(1)).getById(4L);
        }
    }
}
