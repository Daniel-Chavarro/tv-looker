package org.tvl.tvlooker.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tvl.tvlooker.api.exception.GlobalExceptionHandler;
import org.tvl.tvlooker.domain.exception.DirectorNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Director;
import org.tvl.tvlooker.service.DirectorService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DirectorControllerTest {

    @Mock
    private DirectorService directorService;

    @InjectMocks
    private DirectorController directorController;

    private MockMvc mockMvc;

    private Director testDirector;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(directorController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testDirector = Director.builder()
                .id(1L)
                .tmdbId(100L)
                .name("Christopher Nolan")
                .build();
    }

    @Nested
    class GetAllDirectors {

        @Test
        void givenDirectorsExist_whenGetAllDirectors_thenReturnsPaginatedList() throws Exception {
            Director secondDirector = Director.builder()
                    .id(2L)
                    .tmdbId(200L)
                    .name("Steven Spielberg")
                    .build();
            List<Director> directors = Arrays.asList(testDirector, secondDirector);
            Page<Director> directorPage = new PageImpl<>(directors, PageRequest.of(0, 50), directors.size());

            when(directorService.getAll(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(directorPage);

            mockMvc.perform(get("/api/v1/directors")
                            .param("page", "0")
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.totalItems", is(2)))
                    .andExpect(jsonPath("$.actualPage", is(0)));

            verify(directorService, times(1)).getAll(org.mockito.ArgumentMatchers.any(Pageable.class));
        }

        @Test
        void givenNoDirectors_whenGetAllDirectors_thenReturnsEmptyPage() throws Exception {
            Page<Director> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
            when(directorService.getAll(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/directors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalItems", is(0)));

            verify(directorService, times(1)).getAll(org.mockito.ArgumentMatchers.any(Pageable.class));
        }
    }

    @Nested
    class GetDirectorById {

        @Test
        void givenDirectorExists_whenGetDirectorById_thenReturnsDirector() throws Exception {
            when(directorService.getById(1L)).thenReturn(testDirector);

            mockMvc.perform(get("/api/v1/directors/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Christopher Nolan")))
                    .andExpect(jsonPath("$.tmdbId", is(100)));

            verify(directorService, times(1)).getById(1L);
        }

        @Test
        void givenDirectorNotExists_whenGetDirectorById_thenReturns404() throws Exception {
            when(directorService.getById(999L))
                    .thenThrow(new DirectorNotFoundException("Director not found with id: 999"));

            mockMvc.perform(get("/api/v1/directors/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Director Not Found")))
                    .andExpect(jsonPath("$.body.detail", containsString("Director not found with id: 999")));

            verify(directorService, times(1)).getById(999L);
        }

        @Test
        void givenSpielbergDirector_whenGetDirectorById_thenReturnsDirector() throws Exception {
            Director spielbergDirector = Director.builder()
                    .id(2L)
                    .tmdbId(200L)
                    .name("Steven Spielberg")
                    .build();

            when(directorService.getById(2L)).thenReturn(spielbergDirector);

            mockMvc.perform(get("/api/v1/directors/{id}", 2L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.name", is("Steven Spielberg")))
                    .andExpect(jsonPath("$.tmdbId", is(200)));

            verify(directorService, times(1)).getById(2L);
        }
    }
}
