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

    private UsernamePasswordAuthenticationToken userAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                testUserId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("USER"))
        );
    }

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

            when(interactionService.createForUser(any(Interaction.class), eq(testUserId))).thenReturn(createdInteraction);

            mockMvc.perform(post("/api/v1/interactions")
                            .principal(userAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.itemId", is(100)))
                    .andExpect(jsonPath("$.interactionType", is("VIEW")));

            verify(interactionService, times(1)).createForUser(any(Interaction.class), eq(testUserId));
            verify(interactionService, never()).create(any(Interaction.class));
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

            when(interactionService.createForUser(any(Interaction.class), eq(testUserId)))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(post("/api/v1/interactions")
                            .principal(userAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError());

            verify(interactionService, times(1)).createForUser(any(Interaction.class), eq(testUserId));
        }
    }

    @Nested
    class GetMyInteractions {

        @Test
        void givenCurrentUser_whenGetMyInteractions_thenReturnsCurrentUserInteractions() throws Exception {
            Page<Interaction> interactionsPage = new PageImpl<>(List.of(testInteraction), PageRequest.of(0, 50), 1);
            when(interactionService.getByUserId(eq(testUserId), any(Pageable.class))).thenReturn(interactionsPage);

            mockMvc.perform(get("/api/v1/interactions/me")
                            .principal(userAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].userId", is(testUserId.toString())));

            verify(interactionService, times(1)).getByUserId(eq(testUserId), any(Pageable.class));
        }
    }

    @Nested
    class GetInteractionById {

        @Test
        void givenInteractionExists_whenGetInteractionById_thenReturnsInteraction() throws Exception {
            when(interactionService.getByIdForUser(1L, testUserId)).thenReturn(testInteraction);

            mockMvc.perform(get("/api/v1/interactions/{id}", 1L)
                            .principal(userAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.itemId", is(100)))
                    .andExpect(jsonPath("$.interactionType", is("VIEW")));

            verify(interactionService, times(1)).getByIdForUser(1L, testUserId);
            verify(interactionService, never()).getById(1L);
        }

        @Test
        void givenInteractionNotExists_whenGetInteractionById_thenReturns404() throws Exception {
            when(interactionService.getByIdForUser(999L, testUserId))
                    .thenThrow(new InteractionNotFoundException("Interaction not found with id: 999"));

            mockMvc.perform(get("/api/v1/interactions/{id}", 999L)
                            .principal(userAuthentication()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Interaction Not Found")))
                    .andExpect(jsonPath("$.body.detail", containsString("Interaction not found with id: 999")));

            verify(interactionService, times(1)).getByIdForUser(999L, testUserId);
            verify(interactionService, never()).getById(999L);
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

            when(interactionService.updateForUser(eq(1L), any(Interaction.class), eq(testUserId))).thenReturn(updatedInteraction);

            mockMvc.perform(patch("/api/v1/interactions/{id}", 1L)
                            .principal(userAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.interactionType", is("LIKE")));

            verify(interactionService, times(1)).updateForUser(eq(1L), any(Interaction.class), eq(testUserId));
            verify(interactionService, never()).update(eq(1L), any(Interaction.class));
        }

        @Test
        void givenInteractionNotExists_whenUpdateInteraction_thenReturns404() throws Exception {
            String requestBody = """
                {
                    "interactionType": "LIKE"
                }
                """;

            when(interactionService.updateForUser(eq(999L), any(Interaction.class), eq(testUserId)))
                    .thenThrow(new InteractionNotFoundException("Interaction not found with id: 999"));

            mockMvc.perform(patch("/api/v1/interactions/{id}", 999L)
                            .principal(userAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Interaction Not Found")));

            verify(interactionService, times(1)).updateForUser(eq(999L), any(Interaction.class), eq(testUserId));
        }
    }

    @Nested
    class DeleteInteraction {

        @Test
        void givenInteractionExists_whenDeleteInteraction_thenReturns204() throws Exception {
            doNothing().when(interactionService).deleteForUser(1L, testUserId);

            mockMvc.perform(delete("/api/v1/interactions/{id}", 1L)
                            .principal(userAuthentication()))
                    .andExpect(status().isNoContent());

            verify(interactionService, times(1)).deleteForUser(1L, testUserId);
            verify(interactionService, never()).delete(1L);
        }

        @Test
        void givenInteractionNotExists_whenDeleteInteraction_thenReturns404() throws Exception {
            doThrow(new InteractionNotFoundException("Interaction not found with id: 999"))
                    .when(interactionService).deleteForUser(999L, testUserId);

            mockMvc.perform(delete("/api/v1/interactions/{id}", 999L)
                            .principal(userAuthentication()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("Interaction Not Found")));

            verify(interactionService, times(1)).deleteForUser(999L, testUserId);
        }
    }
}
