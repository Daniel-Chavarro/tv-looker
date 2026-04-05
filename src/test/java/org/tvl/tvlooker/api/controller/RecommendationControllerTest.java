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
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.service.RecommendationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private RecommendationController recommendationController;

    private MockMvc mockMvc;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(recommendationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUserId = UUID.randomUUID();
    }

    private Item createTestItem(Long id, String title) {
        return Item.builder()
                .id(id)
                .title(title)
                .overview("Test overview")
                .releaseDate(LocalDate.of(2024, 1, 1))
                .popularity(new BigDecimal("7.5"))
                .voteAverage(new BigDecimal("8.0"))
                .tmdbType(TmdbType.MOVIE)
                .tmdbId(1000L + id)
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();
    }

    @Nested
    class GetRecommendations {

        @Test
        void givenValidUserIdAndDefaultLimit_whenGetRecommendations_thenReturnsRecommendations() throws Exception {
            List<Item> recommendations = java.util.Arrays.asList(
                    createTestItem(1L, "Movie 1"),
                    createTestItem(2L, "Movie 2"),
                    createTestItem(3L, "Movie 3")
            );

            when(recommendationService.getUserRecommendations(eq(testUserId), anyInt()))
                    .thenReturn(recommendations);

            mockMvc.perform(get("/api/v1/users/{userId}/recommendations", testUserId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.userId", is(testUserId.toString())))
                    .andExpect(jsonPath("$.count", is(3)))
                    .andExpect(jsonPath("$.items", hasSize(3)))
                    .andExpect(jsonPath("$.items[0].title", is("Movie 1")))
                    .andExpect(jsonPath("$.items[1].title", is("Movie 2")))
                    .andExpect(jsonPath("$.items[2].title", is("Movie 3")));

            verify(recommendationService, times(1)).getUserRecommendations(eq(testUserId), eq(10));
        }

        @Test
        void givenValidUserIdAndCustomLimit_whenGetRecommendations_thenReturnsLimitedRecommendations() throws Exception {
            List<Item> recommendations = java.util.Arrays.asList(
                    createTestItem(1L, "Movie 1"),
                    createTestItem(2L, "Movie 2")
            );

            when(recommendationService.getUserRecommendations(eq(testUserId), eq(2)))
                    .thenReturn(recommendations);

            mockMvc.perform(get("/api/v1/users/{userId}/recommendations", testUserId)
                            .param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.userId", is(testUserId.toString())))
                    .andExpect(jsonPath("$.count", is(2)))
                    .andExpect(jsonPath("$.items", hasSize(2)));

            verify(recommendationService, times(1)).getUserRecommendations(eq(testUserId), eq(2));
        }

        @Test
        void givenNoRecommendations_whenGetRecommendations_thenReturnsEmptyList() throws Exception {
            when(recommendationService.getUserRecommendations(eq(testUserId), anyInt()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/users/{userId}/recommendations", testUserId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.userId", is(testUserId.toString())))
                    .andExpect(jsonPath("$.count", is(0)))
                    .andExpect(jsonPath("$.items", hasSize(0)));

            verify(recommendationService, times(1)).getUserRecommendations(eq(testUserId), eq(10));
        }

        @Test
        void givenMaxLimit_whenGetRecommendations_thenReturnsUpToMaxRecommendations() throws Exception {
            List<Item> recommendations = java.util.Arrays.asList(
                    createTestItem(1L, "Movie 1"),
                    createTestItem(2L, "Movie 2")
            );

            when(recommendationService.getUserRecommendations(eq(testUserId), eq(100)))
                    .thenReturn(recommendations);

            mockMvc.perform(get("/api/v1/users/{userId}/recommendations", testUserId)
                            .param("limit", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count", is(2)));

            verify(recommendationService, times(1)).getUserRecommendations(eq(testUserId), eq(100));
        }

        @Test
        void givenManyRecommendations_whenGetRecommendations_thenReturnsCorrectCount() throws Exception {
            List<Item> recommendations = java.util.Arrays.asList(
                    createTestItem(1L, "Movie 1"),
                    createTestItem(2L, "Movie 2"),
                    createTestItem(3L, "Movie 3"),
                    createTestItem(4L, "Movie 4"),
                    createTestItem(5L, "Movie 5")
            );

            when(recommendationService.getUserRecommendations(eq(testUserId), eq(5)))
                    .thenReturn(recommendations);

            mockMvc.perform(get("/api/v1/users/{userId}/recommendations", testUserId)
                            .param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count", is(5)))
                    .andExpect(jsonPath("$.items", hasSize(5)));

            verify(recommendationService, times(1)).getUserRecommendations(eq(testUserId), eq(5));
        }
    }
}
