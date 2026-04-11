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
import org.tvl.tvlooker.domain.exception.ReviewNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Review;
import org.tvl.tvlooker.service.ReviewService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID testUserId;
    private Review testReview;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testUserId = UUID.randomUUID();
        testReview = Review.builder()
                .id(1L)
                .userId(testUserId)
                .itemId(100L)
                .score(5)
                .reviewText("Great movie!")
                .reviewDate(Timestamp.from(Instant.now()))
                .build();
    }

    @Nested
    class GetAllReviews {

        @Test
        void givenNoReviews_whenGetAllReviews_thenReturnsEmptyList() throws Exception {
            when(reviewService.getAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/reviews"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(reviewService, times(1)).getAll();
        }
    }

    @Nested
    class CreateReview {

        @Test
        void givenValidRequest_whenCreateReview_thenReturnsCreatedReview() throws Exception {
            String requestBody = """
                {
                    "userId": "%s",
                    "itemId": 100,
                    "rating": 5,
                    "comment": "Great movie!"
                }
                """.formatted(testUserId);

            Review createdReview = Review.builder()
                    .id(1L)
                    .userId(testUserId)
                    .itemId(100L)
                    .score(5)
                    .reviewText("Great movie!")
                    .reviewDate(Timestamp.from(Instant.now()))
                    .build();

            when(reviewService.create(any(Review.class))).thenReturn(createdReview);

            mockMvc.perform(post("/api/v1/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.itemId", is(100)));

            verify(reviewService, times(1)).create(any(Review.class));
        }

        @Test
        void givenServiceException_whenCreateReview_thenReturns500() throws Exception {
            String requestBody = """
                {
                    "userId": "%s",
                    "itemId": 100,
                    "rating": 5,
                    "comment": "Great movie!"
                }
                """.formatted(testUserId);

            when(reviewService.create(any(Review.class)))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(post("/api/v1/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError());

            verify(reviewService, times(1)).create(any(Review.class));
        }
    }

    @Nested
    class GetReviewById {

        @Test
        void givenReviewExists_whenGetReviewById_thenReturnsReview() throws Exception {
            when(reviewService.getById(1L)).thenReturn(testReview);

            mockMvc.perform(get("/api/v1/reviews/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.itemId", is(100)));

            verify(reviewService, times(1)).getById(1L);
        }

        @Test
        void givenReviewNotExists_whenGetReviewById_thenReturns404() throws Exception {
            when(reviewService.getById(999L))
                    .thenThrow(new ReviewNotFoundException("Review not found with id: 999"));

            mockMvc.perform(get("/api/v1/reviews/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Review Not Found")))
                    .andExpect(jsonPath("$.body.detail", containsString("Review not found with id: 999")));

            verify(reviewService, times(1)).getById(999L);
        }
    }

    @Nested
    class UpdateReview {

        @Test
        void givenValidRequest_whenUpdateReview_thenReturnsUpdatedReview() throws Exception {
            String requestBody = """
                {
                    "userId": "%s",
                    "itemId": 100,
                    "rating": 4,
                    "comment": "Updated review"
                }
                """.formatted(testUserId);

            Review updatedReview = Review.builder()
                    .id(1L)
                    .userId(testUserId)
                    .itemId(100L)
                    .score(4)
                    .reviewText("Updated review")
                    .reviewDate(Timestamp.from(Instant.now()))
                    .build();

            when(reviewService.update(eq(1L), any(Review.class))).thenReturn(updatedReview);

            mockMvc.perform(put("/api/v1/reviews/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)));

            verify(reviewService, times(1)).update(eq(1L), any(Review.class));
        }

        @Test
        void givenReviewNotExists_whenUpdateReview_thenReturns404() throws Exception {
            String requestBody = """
                {
                    "userId": "%s",
                    "itemId": 100,
                    "rating": 4,
                    "comment": "Updated review"
                }
                """.formatted(testUserId);

            when(reviewService.update(eq(999L), any(Review.class)))
                    .thenThrow(new ReviewNotFoundException("Review not found with id: 999"));

            mockMvc.perform(put("/api/v1/reviews/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Review Not Found")));

            verify(reviewService, times(1)).update(eq(999L), any(Review.class));
        }
    }

    @Nested
    class DeleteReview {

        @Test
        void givenReviewExists_whenDeleteReview_thenReturns204() throws Exception {
            doNothing().when(reviewService).deleteById(1L);

            mockMvc.perform(delete("/api/v1/reviews/{id}", 1L))
                    .andExpect(status().isNoContent());

            verify(reviewService, times(1)).deleteById(1L);
        }

        @Test
        void givenReviewNotExists_whenDeleteReview_thenReturns404() throws Exception {
            doThrow(new ReviewNotFoundException("Review not found with id: 999"))
                    .when(reviewService).deleteById(999L);

            mockMvc.perform(delete("/api/v1/reviews/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Review Not Found")));

            verify(reviewService, times(1)).deleteById(999L);
        }
    }
}
