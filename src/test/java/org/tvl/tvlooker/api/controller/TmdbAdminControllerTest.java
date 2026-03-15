package org.tvl.tvlooker.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tvl.tvlooker.api.exception.GlobalExceptionHandler;
import org.tvl.tvlooker.domain.exception.TmdbCollectionInProgressException;
import org.tvl.tvlooker.service.tmdb.TmdbDataCollectorService;
import org.tvl.tvlooker.service.tmdb.TmdbDataSynchronizerService;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TmdbAdminController.
 * Uses Mockito to test controller endpoints with mocked services.
 */
@ExtendWith(MockitoExtension.class)
class TmdbAdminControllerTest {

    @Mock
    private TmdbDataCollectorService collectorService;

    @Mock
    private TmdbDataSynchronizerService synchronizerService;

    @InjectMocks
    private TmdbAdminController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testCollectAll_Success() throws Exception {
        // When
        when(collectorService.collectAllAsync())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // Then
        mockMvc.perform(post("/api/v1/admin/tmdb/collect"))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("started")))
                .andExpect(jsonPath("$.message", is("TMDB data collection initiated")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(collectorService, times(1)).collectAllAsync();
    }

    @Test
    void testCollectAll_AlreadyRunning() throws Exception {
        // When
        when(collectorService.collectAllAsync())
                .thenThrow(new TmdbCollectionInProgressException("TMDB data collection is already in progress"));

        // Then
        mockMvc.perform(post("/api/v1/admin/tmdb/collect"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("TMDB data collection is already in progress")));

        verify(collectorService, times(1)).collectAllAsync();
    }

    @Test
    void testCollectGenres_Success() throws Exception {
        // When
        doNothing().when(collectorService).collectGenres();

        // Then
        mockMvc.perform(post("/api/v1/admin/tmdb/collect/genres"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("completed")))
                .andExpect(jsonPath("$.message", is("Genre collection completed")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(collectorService, times(1)).collectGenres();
    }

    @Test
    void testCollectMovies_Success() throws Exception {
        // When
        when(collectorService.collectPopularMoviesAsync())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // Then
        mockMvc.perform(post("/api/v1/admin/tmdb/collect/movies"))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("started")))
                .andExpect(jsonPath("$.message", is("Movie collection initiated")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(collectorService, times(1)).collectPopularMoviesAsync();
    }

    @Test
    void testCollectMovies_AlreadyRunning() throws Exception {
        // When
        when(collectorService.collectPopularMoviesAsync())
                .thenThrow(new TmdbCollectionInProgressException("TMDB data collection is already in progress"));

        // Then
        mockMvc.perform(post("/api/v1/admin/tmdb/collect/movies"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));

        verify(collectorService, times(1)).collectPopularMoviesAsync();
    }

    @Test
    void testCollectTvShows_Success() throws Exception {
        // When
        when(collectorService.collectPopularTvShowsAsync())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // Then
        mockMvc.perform(post("/api/v1/admin/tmdb/collect/tvshows"))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("started")))
                .andExpect(jsonPath("$.message", is("TV show collection initiated")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(collectorService, times(1)).collectPopularTvShowsAsync();
    }

    @Test
    void testCollectTvShows_AlreadyRunning() throws Exception {
        // When
        when(collectorService.collectPopularTvShowsAsync())
                .thenThrow(new TmdbCollectionInProgressException("TMDB data collection is already in progress"));

        // Then
        mockMvc.perform(post("/api/v1/admin/tmdb/collect/tvshows"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));

        verify(collectorService, times(1)).collectPopularTvShowsAsync();
    }

    @Test
    void testSync_Success() throws Exception {
        // When
        doNothing().when(synchronizerService).synchronize();

        // Then
        mockMvc.perform(post("/api/v1/admin/tmdb/sync"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("completed")))
                .andExpect(jsonPath("$.message", is("TMDB synchronization completed")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(synchronizerService, times(1)).synchronize();
    }

    @Test
    void testGetStatus_CollectorNotRunning() throws Exception {
        // Given
        LocalDate lastSyncDate = LocalDate.of(2026, 3, 15);
        when(collectorService.isCollectionInProgress()).thenReturn(false);
        when(synchronizerService.getLastSyncDate()).thenReturn(lastSyncDate);

        // Then
        mockMvc.perform(get("/api/v1/admin/tmdb/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.collectorRunning", is(false)))
                .andExpect(jsonPath("$.lastSyncDate", is("2026-03-15")))
                .andExpect(jsonPath("$.syncEnabled", is(true)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(collectorService, times(1)).isCollectionInProgress();
        verify(synchronizerService, times(1)).getLastSyncDate();
    }

    @Test
    void testGetStatus_CollectorRunning() throws Exception {
        // Given
        LocalDate lastSyncDate = LocalDate.of(2026, 3, 14);
        when(collectorService.isCollectionInProgress()).thenReturn(true);
        when(synchronizerService.getLastSyncDate()).thenReturn(lastSyncDate);

        // Then
        mockMvc.perform(get("/api/v1/admin/tmdb/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.collectorRunning", is(true)))
                .andExpect(jsonPath("$.lastSyncDate", is("2026-03-14")))
                .andExpect(jsonPath("$.syncEnabled", is(true)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(collectorService, times(1)).isCollectionInProgress();
        verify(synchronizerService, times(1)).getLastSyncDate();
    }

    @Test
    void testGetStatus_NullLastSyncDate() throws Exception {
        // Given
        when(collectorService.isCollectionInProgress()).thenReturn(false);
        when(synchronizerService.getLastSyncDate()).thenReturn(null);

        // Then
        mockMvc.perform(get("/api/v1/admin/tmdb/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.collectorRunning", is(false)))
                .andExpect(jsonPath("$.lastSyncDate").doesNotExist())
                .andExpect(jsonPath("$.syncEnabled", is(true)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(collectorService, times(1)).isCollectionInProgress();
        verify(synchronizerService, times(1)).getLastSyncDate();
    }
}
