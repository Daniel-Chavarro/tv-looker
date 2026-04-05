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
                .actors(new HashSet<>())
                .build();
    }

    @Nested
    class GetAllItems {

        @Test
        void givenItemsExist_whenGetAllItems_thenReturnsItemList() throws Exception {
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
                    .actors(new HashSet<>())
                    .build();
            List<Item> items = Arrays.asList(testItem, secondItem);

            when(itemService.getAll()).thenReturn(items);

            mockMvc.perform(get("/api/v1/items"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].title", is("Test Movie")))
                    .andExpect(jsonPath("$[0].overview", is("This is a test movie overview")))
                    .andExpect(jsonPath("$[0].tmdbType", is("MOVIE")))
                    .andExpect(jsonPath("$[1].title", is("Test TV Show")));

            verify(itemService, times(1)).getAll();
        }

        @Test
        void givenNoItems_whenGetAllItems_thenReturnsEmptyList() throws Exception {
            when(itemService.getAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/items"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(itemService, times(1)).getAll();
        }

        @Test
        void givenItemsWithNullReleaseDate_whenGetAllItems_thenReturnsItemList() throws Exception {
            Item itemWithNullDate = Item.builder()
                    .id(3L)
                    .title("Movie Without Date")
                    .overview("No release date")
                    .releaseDate(null)
                    .popularity(new BigDecimal("5.0"))
                    .voteAverage(new BigDecimal("6.0"))
                    .tmdbType(TmdbType.MOVIE)
                    .tmdbId(11111L)
                    .genres(new HashSet<>())
                    .directors(new HashSet<>())
                    .actors(new HashSet<>())
                    .build();

            when(itemService.getAll()).thenReturn(Collections.singletonList(itemWithNullDate));

            mockMvc.perform(get("/api/v1/items"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title", is("Movie Without Date")))
                    .andExpect(jsonPath("$[0].releaseDate").doesNotExist());

            verify(itemService, times(1)).getAll();
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
                    .actors(new HashSet<>())
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
