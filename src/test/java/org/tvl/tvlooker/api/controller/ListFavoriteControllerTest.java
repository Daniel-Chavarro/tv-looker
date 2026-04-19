package org.tvl.tvlooker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.tvl.tvlooker.domain.exception.ListFavoriteNotFoundException;
import org.tvl.tvlooker.domain.model.dto.ListFavorite;
import org.tvl.tvlooker.service.ListFavoriteService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

@ExtendWith(MockitoExtension.class)
class ListFavoriteControllerTest {

    @Mock
    private ListFavoriteService listFavoriteService;

    @InjectMocks
    private ListFavoriteController listFavoriteController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID testUserId;
    private ListFavorite testListFavorite;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(listFavoriteController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testUserId = UUID.randomUUID();

        testListFavorite = ListFavorite.builder()
                .id(1L)
                .userId(testUserId)
                .name("My Favorites")
                .description("My favorite movies")
                .build();
    }

    @Nested
    class ListFavorites {

        @Test
        void givenListFavoritesExist_whenListFavorites_thenReturnsPaginatedList() throws Exception {
            ListFavorite secondList = ListFavorite.builder()
                    .id(2L)
                    .userId(testUserId)
                    .name("My Second List")
                    .build();
            List<ListFavorite> favorites = Arrays.asList(testListFavorite, secondList);
            Page<ListFavorite> favoritesPage = new PageImpl<>(favorites, PageRequest.of(0, 50), favorites.size());

            when(listFavoriteService.getAll(any(Pageable.class))).thenReturn(favoritesPage);

            mockMvc.perform(get("/api/v1/lists")
                            .param("page", "0")
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.totalItems", is(2)))
                    .andExpect(jsonPath("$.actualPage", is(0)));

            verify(listFavoriteService, times(1)).getAll(any(Pageable.class));
        }

        @Test
        void givenNoListFavorites_whenListFavorites_thenReturnsEmptyPage() throws Exception {
            Page<ListFavorite> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
            when(listFavoriteService.getAll(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/lists"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalItems", is(0)));

            verify(listFavoriteService, times(1)).getAll(any(Pageable.class));
        }
    }

    @Nested
    class CreateListFavorite {

        @Test
        void givenValidRequest_whenCreateListFavorite_thenReturnsCreatedList() throws Exception {
            String requestBody = """
                {
                    "userId": "%s",
                    "name": "My Favorites",
                    "description": "My favorite movies"
                }
                """.formatted(testUserId);

            ListFavorite createdList = ListFavorite.builder()
                    .id(1L)
                    .userId(testUserId)
                    .name("My Favorites")
                    .description("My favorite movies")
                    .items(new java.util.HashSet<>())
                    .build();

            when(listFavoriteService.create(any(ListFavorite.class))).thenReturn(createdList);

            mockMvc.perform(post("/api/v1/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("My Favorites")));

            verify(listFavoriteService, times(1)).create(any(ListFavorite.class));
        }

        @Test
        void givenServiceException_whenCreateListFavorite_thenReturns500() throws Exception {
            String requestBody = """
                {
                    "userId": "%s",
                    "name": "My Favorites",
                    "description": "My favorite movies"
                }
                """.formatted(testUserId);

            when(listFavoriteService.create(any(ListFavorite.class)))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(post("/api/v1/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError());

            verify(listFavoriteService, times(1)).create(any(ListFavorite.class));
        }
    }

    @Nested
    class GetListFavoriteById {

        @Test
        void givenListFavoriteNotExists_whenGetListFavoriteById_thenReturns404() throws Exception {
            when(listFavoriteService.getById(999L))
                    .thenThrow(new ListFavoriteNotFoundException("ListFavorite not found with id: 999"));

            mockMvc.perform(get("/api/v1/lists/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("List Favorite Not Found")))
                    .andExpect(jsonPath("$.body.detail", containsString("ListFavorite not found with id: 999")));

            verify(listFavoriteService, times(1)).getById(999L);
        }
    }

    @Nested
    class UpdateListFavorite {

        @Test
        void givenListFavoriteNotExists_whenUpdateListFavorite_thenReturns404() throws Exception {
            String requestBody = """
                {
                    "name": "Updated Favorites"
                }
                """;

            when(listFavoriteService.update(eq(999L), any(ListFavorite.class)))
                    .thenThrow(new ListFavoriteNotFoundException("ListFavorite not found with id: 999"));

            mockMvc.perform(put("/api/v1/lists/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("List Favorite Not Found")));

            verify(listFavoriteService, times(1)).update(eq(999L), any(ListFavorite.class));
        }
    }

    @Nested
    class DeleteListFavorite {

        @Test
        void givenListFavoriteExists_whenDeleteListFavorite_thenReturns204() throws Exception {
            doNothing().when(listFavoriteService).deleteById(1L);

            mockMvc.perform(delete("/api/v1/lists/{id}", 1L))
                    .andExpect(status().isNoContent());

            verify(listFavoriteService, times(1)).deleteById(1L);
        }

        @Test
        void givenListFavoriteNotExists_whenDeleteListFavorite_thenReturns404() throws Exception {
            doThrow(new ListFavoriteNotFoundException("ListFavorite not found with id: 999"))
                    .when(listFavoriteService).deleteById(999L);

            mockMvc.perform(delete("/api/v1/lists/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("List Favorite Not Found")));

            verify(listFavoriteService, times(1)).deleteById(999L);
        }
    }
}
