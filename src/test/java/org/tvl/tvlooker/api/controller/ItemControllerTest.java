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
import org.tvl.tvlooker.domain.exception.ItemNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.service.ItemService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

    @Mock
    private ItemService itemService;

    @InjectMocks
    private ItemController itemController;

    private MockMvc mockMvc;

    private Item testItem;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(itemController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testItem = Item.builder()
                .id(1L)
                .title("Test Movie")
                .overview("This is a test movie overview")
                .releaseDate(LocalDate.of(2024, 1, 15))
                .popularity(new BigDecimal("7.5"))
                .voteAverage(new BigDecimal("8.2"))
                .tmdbType(TmdbType.MOVIE)
                .tmdbId(12345L)
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actorsInItem(new HashSet<>())
                .build();
    }

    @Nested
    class GetAllItems {

        @Test
        void givenItemsExist_whenGetAllItems_thenReturnsPaginatedList() throws Exception {
            Item secondItem = Item.builder()
                    .id(2L)
                    .title("Test TV Show")
                    .overview("This is a test TV show overview")
                    .releaseDate(LocalDate.of(2024, 2, 20))
                    .popularity(new BigDecimal("8.0"))
                    .voteAverage(new BigDecimal("8.5"))
                    .tmdbType(TmdbType.TV)
                    .tmdbId(67890L)
                    .genres(new HashSet<>())
                    .directors(new HashSet<>())
                    .actorsInItem(new HashSet<>())
                    .build();
            List<Item> items = Arrays.asList(testItem, secondItem);
            Page<Item> itemPage = new PageImpl<>(items, PageRequest.of(0, 50), items.size());

            when(itemService.getAll(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(itemPage);

            mockMvc.perform(get("/api/v1/items")
                            .param("page", "0")
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.content[0].title", is("Test Movie")))
                    .andExpect(jsonPath("$.content[1].title", is("Test TV Show")))
                    .andExpect(jsonPath("$.totalElements", is(2)))
                    .andExpect(jsonPath("$.totalPages", is(1)))
                    .andExpect(jsonPath("$.size", is(50)))
                    .andExpect(jsonPath("$.number", is(0)));

            verify(itemService, times(1)).getAll(org.mockito.ArgumentMatchers.any(Pageable.class));
        }

        @Test
        void givenNoItems_whenGetAllItems_thenReturnsEmptyPage() throws Exception {
            Page<Item> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
            when(itemService.getAll(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/items"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));

            verify(itemService, times(1)).getAll(org.mockito.ArgumentMatchers.any(Pageable.class));
        }
    }

    @Nested
    class GetItemById {

        @Test
        void givenItemExists_whenGetItemById_thenReturnsItem() throws Exception {
            when(itemService.getById(1L)).thenReturn(testItem);

            mockMvc.perform(get("/api/v1/items/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.title", is("Test Movie")))
                    .andExpect(jsonPath("$.overview", is("This is a test movie overview")))
                    .andExpect(jsonPath("$.releaseDate", is("2024-01-15")))
                    .andExpect(jsonPath("$.popularity", is(7.5)))
                    .andExpect(jsonPath("$.voteAverage", is(8.2)))
                    .andExpect(jsonPath("$.tmdbType", is("MOVIE")))
                    .andExpect(jsonPath("$.tmdbId", is(12345)));

            verify(itemService, times(1)).getById(1L);
        }

        @Test
        void givenItemNotExists_whenGetItemById_thenReturns404() throws Exception {
            when(itemService.getById(999L))
                    .thenThrow(new ItemNotFoundException("Item not found with id: 999"));

            mockMvc.perform(get("/api/v1/items/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Item Not Found")))
                    .andExpect(jsonPath("$.body.detail", containsString("Item not found with id: 999")));

            verify(itemService, times(1)).getById(999L);
        }

        @Test
        void givenTvShowItem_whenGetItemById_thenReturnsTvShow() throws Exception {
            Item tvShow = Item.builder()
                    .id(2L)
                    .title("Breaking Bad")
                    .overview("A chemistry teacher turns to cooking meth")
                    .releaseDate(LocalDate.of(2008, 1, 20))
                    .popularity(new BigDecimal("9.5"))
                    .voteAverage(new BigDecimal("9.5"))
                    .tmdbType(TmdbType.TV)
                    .tmdbId(1396L)
                    .genres(new HashSet<>())
                    .directors(new HashSet<>())
                    .actorsInItem(new HashSet<>())
                    .build();

            when(itemService.getById(2L)).thenReturn(tvShow);

            mockMvc.perform(get("/api/v1/items/{id}", 2L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.title", is("Breaking Bad")))
                    .andExpect(jsonPath("$.tmdbType", is("TV")));

            verify(itemService, times(1)).getById(2L);
        }
    }
}
