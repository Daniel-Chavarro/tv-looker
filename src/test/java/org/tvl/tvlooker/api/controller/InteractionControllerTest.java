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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tvl.tvlooker.api.exception.GlobalExceptionHandler;
import org.tvl.tvlooker.domain.exception.InteractionNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.service.InteractionService;

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
class InteractionControllerTest {

    @Mock
    private InteractionService interactionService;

    @InjectMocks
    private InteractionController interactionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID testUserId;
    private Interaction testInteraction;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(interactionController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testUserId = UUID.randomUUID();
        testInteraction = Interaction.builder()
                .id(1L)
                .userId(testUserId)
                .itemId(100L)
                .interactionType(InteractionType.VIEW)
                .createdAt(Timestamp.from(Instant.now()))
                .build();
    }

    @Nested
    class GetAllInteractions {

        @Test
        void givenInteractionsExist_whenGetAllInteractions_thenReturnsPaginatedList() throws Exception {
            Interaction secondInteraction = Interaction.builder()
                    .id(2L)
                    .userId(testUserId)
                    .itemId(2L)
                    .interactionType(InteractionType.VIEW)
                    .build();
            List<Interaction> interactions = Arrays.asList(testInteraction, secondInteraction);
            Page<Interaction> interactionPage = new PageImpl<>(interactions, PageRequest.of(0, 50), interactions.size());

            when(interactionService.getAll(any(Pageable.class))).thenReturn(interactionPage);

            mockMvc.perform(get("/api/v1/interactions")
                            .param("page", "0")
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.totalItems", is(2)))
                    .andExpect(jsonPath("$.actualPage", is(0)));

            verify(interactionService, times(1)).getAll(any(Pageable.class));
        }

        @Test
        void givenNoInteractions_whenGetAllInteractions_thenReturnsEmptyPage() throws Exception {
            Page<Interaction> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
            when(interactionService.getAll(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/interactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalItems", is(0)));

            verify(interactionService, times(1)).getAll(any(Pageable.class));
        }
    }

    @Nested
    class CreateInteraction {

        @Test
        void givenValidRequest_whenCreateInteraction_thenReturnsCreatedInteraction() throws Exception {
            String requestBody = """
                {
                    "userId": "%s",
                    "itemId": 100,
                    "interactionType": "VIEW"
                }
                """.formatted(testUserId);

            Interaction createdInteraction = Interaction.builder()
                    .id(1L)
                    .userId(testUserId)
                    .itemId(100L)
                    .interactionType(InteractionType.VIEW)
                    .createdAt(Timestamp.from(Instant.now()))
                    .build();

            when(interactionService.create(any(Interaction.class))).thenReturn(createdInteraction);

            mockMvc.perform(post("/api/v1/interactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.itemId", is(100)))
                    .andExpect(jsonPath("$.interactionType", is("VIEW")));

            verify(interactionService, times(1)).create(any(Interaction.class));
        }

        @Test
        void givenServiceException_whenCreateInteraction_thenReturns500() throws Exception {
            String requestBody = """
                {
                    "userId": "%s",
                    "itemId": 100,
                    "interactionType": "VIEW"
                }
                """.formatted(testUserId);

            when(interactionService.create(any(Interaction.class)))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(post("/api/v1/interactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError());

            verify(interactionService, times(1)).create(any(Interaction.class));
        }
    }

    @Nested
    class GetInteractionById {

        @Test
        void givenInteractionExists_whenGetInteractionById_thenReturnsInteraction() throws Exception {
            when(interactionService.getById(1L)).thenReturn(testInteraction);

            mockMvc.perform(get("/api/v1/interactions/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.itemId", is(100)))
                    .andExpect(jsonPath("$.interactionType", is("VIEW")));

            verify(interactionService, times(1)).getById(1L);
        }

        @Test
        void givenInteractionNotExists_whenGetInteractionById_thenReturns404() throws Exception {
            when(interactionService.getById(999L))
                    .thenThrow(new InteractionNotFoundException("Interaction not found with id: 999"));

            mockMvc.perform(get("/api/v1/interactions/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Interaction Not Found")))
                    .andExpect(jsonPath("$.body.detail", containsString("Interaction not found with id: 999")));

            verify(interactionService, times(1)).getById(999L);
        }
    }

    @Nested
    class UpdateInteraction {

        @Test
        void givenValidRequest_whenUpdateInteraction_thenReturnsUpdatedInteraction() throws Exception {
            String requestBody = """
                {
                    "interactionType": "LIKE"
                }
                """;

            Interaction updatedInteraction = Interaction.builder()
                    .id(1L)
                    .userId(testUserId)
                    .itemId(100L)
                    .interactionType(InteractionType.LIKE)
                    .createdAt(Timestamp.from(Instant.now()))
                    .build();

            when(interactionService.update(eq(1L), any(Interaction.class))).thenReturn(updatedInteraction);

            mockMvc.perform(put("/api/v1/interactions/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.interactionType", is("LIKE")));

            verify(interactionService, times(1)).update(eq(1L), any(Interaction.class));
        }

        @Test
        void givenInteractionNotExists_whenUpdateInteraction_thenReturns404() throws Exception {
            String requestBody = """
                {
                    "interactionType": "LIKE"
                }
                """;

            when(interactionService.update(eq(999L), any(Interaction.class)))
                    .thenThrow(new InteractionNotFoundException("Interaction not found with id: 999"));

            mockMvc.perform(put("/api/v1/interactions/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Interaction Not Found")));

            verify(interactionService, times(1)).update(eq(999L), any(Interaction.class));
        }
    }

    @Nested
    class DeleteInteraction {

        @Test
        void givenInteractionExists_whenDeleteInteraction_thenReturns204() throws Exception {
            doNothing().when(interactionService).delete(1L);

            mockMvc.perform(delete("/api/v1/interactions/{id}", 1L))
                    .andExpect(status().isNoContent());

            verify(interactionService, times(1)).delete(1L);
        }

        @Test
        void givenInteractionNotExists_whenDeleteInteraction_thenReturns404() throws Exception {
            doThrow(new InteractionNotFoundException("Interaction not found with id: 999"))
                    .when(interactionService).delete(999L);

            mockMvc.perform(delete("/api/v1/interactions/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Interaction Not Found")));

            verify(interactionService, times(1)).delete(999L);
        }
    }
}
