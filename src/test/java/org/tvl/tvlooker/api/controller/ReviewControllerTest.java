package org.tvl.tvlooker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tvl.tvlooker.api.exception.GlobalExceptionHandler;
import org.tvl.tvlooker.domain.exception.ReviewNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Review;
import org.tvl.tvlooker.service.ReviewService;

import java.sql.Timestamp;
import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID testUserId;
    private UUID otherUserId;
    private Review testReview;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testUserId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        testReview = Review.builder()
                .id(1L)
                .userId(testUserId)
                .itemId(100L)
                .score(5)
                .reviewText("Great movie!")
                .reviewDate(Timestamp.from(Instant.now()))
                .build();
    }

    private UsernamePasswordAuthenticationToken userAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                testUserId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("USER"))
        );
    }

    private UsernamePasswordAuthenticationToken adminAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                otherUserId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ADMIN"))
        );
    }

    @Nested
    class GetAllReviews {

        @Test
        void givenReviewsExist_whenGetAllReviews_thenReturnsPaginatedList() throws Exception {
            UUID userId2 = UUID.randomUUID();
            Review secondReview = Review.builder()
                    .id(2L)
                    .userId(userId2)
                    .itemId(2L)
                    .score(4)
                    .reviewText("Great show!")
                    .build();
            List<Review> reviews = Arrays.asList(testReview, secondReview);
            Page<Review> reviewsPage = new PageImpl<>(reviews, PageRequest.of(0, 50), reviews.size());

            when(reviewService.getAll(any(Pageable.class))).thenReturn(reviewsPage);

            mockMvc.perform(get("/api/v1/reviews")
                            .param("page", "0")
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.totalItems", is(2)))
                    .andExpect(jsonPath("$.actualPage", is(0)));

            verify(reviewService, times(1)).getAll(any(Pageable.class));
        }

        @Test
        void givenNoReviews_whenGetAllReviews_thenReturnsEmptyPage() throws Exception {
            Page<Review> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
            when(reviewService.getAll(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/reviews"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalItems", is(0)));

            verify(reviewService, times(1)).getAll(any(Pageable.class));
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

            when(reviewService.createForUser(any(Review.class), eq(testUserId))).thenReturn(createdReview);

            mockMvc.perform(post("/api/v1/reviews")
                            .principal(userAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.itemId", is(100)));

            verify(reviewService, times(1)).createForUser(any(Review.class), eq(testUserId));
            verify(reviewService, never()).create(any(Review.class));
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

            when(reviewService.createForUser(any(Review.class), eq(testUserId)))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(post("/api/v1/reviews")
                            .principal(userAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError());

            verify(reviewService, times(1)).createForUser(any(Review.class), eq(testUserId));
        }
    }

    @Nested
    class GetReviewById {

        @Test
        void givenUserOwnsReview_whenGetReviewById_thenReturnsReview() throws Exception {
            when(reviewService.getByIdForUser(1L, testUserId)).thenReturn(testReview);

            mockMvc.perform(get("/api/v1/reviews/{id}", 1L)
                            .principal(userAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.itemId", is(100)));

            verify(reviewService, times(1)).getByIdForUser(1L, testUserId);
            verify(reviewService, never()).getByIdForAdmin(1L);
        }

        @Test
        void givenAdmin_whenGetReviewById_thenUsesAdminPath() throws Exception {
            when(reviewService.getByIdForAdmin(1L)).thenReturn(testReview);

            mockMvc.perform(get("/api/v1/reviews/{id}", 1L)
                            .principal(adminAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)));

            verify(reviewService, times(1)).getByIdForAdmin(1L);
            verify(reviewService, never()).getByIdForUser(eq(1L), any(UUID.class));
        }

        @Test
        void givenReviewNotExists_whenGetReviewById_thenReturns404() throws Exception {
            when(reviewService.getByIdForUser(999L, testUserId))
                    .thenThrow(new ReviewNotFoundException("Review not found with id: 999"));

            mockMvc.perform(get("/api/v1/reviews/{id}", 999L)
                            .principal(userAuthentication()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Review Not Found")))
                    .andExpect(jsonPath("$.body.detail", containsString("Review not found with id: 999")));

            verify(reviewService, times(1)).getByIdForUser(999L, testUserId);
        }
    }

    @Nested
    class GetMyReviews {

        @Test
        void givenCurrentUser_whenGetMyReviews_thenReturnsCurrentUserReviews() throws Exception {
            Page<Review> reviewsPage = new PageImpl<>(List.of(testReview), PageRequest.of(0, 50), 1);
            when(reviewService.getByUserId(eq(testUserId), any(Pageable.class))).thenReturn(reviewsPage);

            mockMvc.perform(get("/api/v1/reviews/me")
                            .principal(userAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].userId", is(testUserId.toString())));

            verify(reviewService, times(1)).getByUserId(eq(testUserId), any(Pageable.class));
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

            when(reviewService.updateForUser(eq(1L), any(Review.class), eq(testUserId))).thenReturn(updatedReview);

            mockMvc.perform(patch("/api/v1/reviews/{id}", 1L)
                            .principal(userAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)));

            verify(reviewService, times(1)).updateForUser(eq(1L), any(Review.class), eq(testUserId));
            verify(reviewService, never()).update(eq(1L), any(Review.class));
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

            when(reviewService.updateForUser(eq(999L), any(Review.class), eq(testUserId)))
                    .thenThrow(new ReviewNotFoundException("Review not found with id: 999"));

            mockMvc.perform(patch("/api/v1/reviews/{id}", 999L)
                            .principal(userAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Review Not Found")));

            verify(reviewService, times(1)).updateForUser(eq(999L), any(Review.class), eq(testUserId));
        }
    }

    @Nested
    class DeleteReview {

        @Test
        void givenReviewExists_whenDeleteReview_thenReturns204() throws Exception {
            doNothing().when(reviewService).deleteByIdForUser(1L, testUserId);

            mockMvc.perform(delete("/api/v1/reviews/{id}", 1L)
                            .principal(userAuthentication()))
                    .andExpect(status().isNoContent());

            verify(reviewService, times(1)).deleteByIdForUser(1L, testUserId);
            verify(reviewService, never()).deleteById(1L);
        }

        @Test
        void givenReviewNotExists_whenDeleteReview_thenReturns404() throws Exception {
            doThrow(new ReviewNotFoundException("Review not found with id: 999"))
                    .when(reviewService).deleteByIdForUser(999L, testUserId);

            mockMvc.perform(delete("/api/v1/reviews/{id}", 999L)
                            .principal(userAuthentication()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Review Not Found")));

            verify(reviewService, times(1)).deleteByIdForUser(999L, testUserId);
        }
    }
}
