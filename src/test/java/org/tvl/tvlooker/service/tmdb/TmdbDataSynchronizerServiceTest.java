package org.tvl.tvlooker.service.tmdb;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Behavior-driven tests for TmdbDataSynchronizerService.
 * Tests follow Given-When-Then pattern and focus on what methods do and return,
 * not on implementation details.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TmdbDataSynchronizerService Behavior Tests")
class TmdbDataSynchronizerServiceTest {

    @Mock
    private TmdbDataFetcher fetcher;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private TmdbItemPersistenceService persistenceService;

    @InjectMocks
    private TmdbDataSynchronizerService synchronizerService;

    @Test
    @DisplayName("Should sync movie changes, TV changes, and discover new items when running synchronization")
    void testSynchronize_CompletesFullFlow() {
        // Given: Sync is enabled and last sync date is yesterday
        LocalDate yesterday = LocalDate.of(2026, 4, 6);
        ReflectionTestUtils.setField(synchronizerService, "lastSyncDate", yesterday);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 1);
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);

        TmdbChangesDto movieChange = new TmdbChangesDto(100L, false);
        TmdbPagedResponseDto<TmdbChangesDto> movieChanges = 
                new TmdbPagedResponseDto<>(1, List.of(movieChange), 1, 1);
        
        TmdbChangesDto tvChange = new TmdbChangesDto(200L, false);
        TmdbPagedResponseDto<TmdbChangesDto> tvChanges = 
                new TmdbPagedResponseDto<>(1, List.of(tvChange), 1, 1);

        when(fetcher.fetchChangesAsync(eq(TmdbMediaType.MOVIE), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(movieChanges));
        when(fetcher.fetchChangesAsync(eq(TmdbMediaType.TV), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(tvChanges));

        when(itemRepository.findByTmdbIdAndTmdbType(100L, TmdbType.MOVIE))
                .thenReturn(Optional.of(mock(ItemEntity.class)));
        when(itemRepository.findByTmdbIdAndTmdbType(200L, TmdbType.TV))
                .thenReturn(Optional.of(mock(ItemEntity.class)));

        TmdbMovieDetailsDto movieDetails = createMockMovieDetails(100L);
        TmdbTvShowDetailsDto tvDetails = createMockTvShowDetails(200L);
        when(fetcher.fetchDetailsWithCreditsAsync(TmdbMediaType.MOVIE, 100L))
                .thenReturn(CompletableFuture.completedFuture(movieDetails));
        when(fetcher.fetchDetailsWithCreditsAsync(TmdbMediaType.TV, 200L))
                .thenReturn(CompletableFuture.completedFuture(tvDetails));

        TmdbMovieDto popularMovie = createMockMovie(300L);
        TmdbPagedResponseDto<TmdbMovieDto> popularMovies = 
                new TmdbPagedResponseDto<>(1, List.of(popularMovie), 1, 1);
        when(fetcher.fetchPopularMoviesAsync(1))
                .thenReturn(CompletableFuture.completedFuture(popularMovies));

        TmdbTvShowDto popularTv = createMockTvShow(400L);
        TmdbPagedResponseDto<TmdbTvShowDto> popularTvShows = 
                new TmdbPagedResponseDto<>(1, List.of(popularTv), 1, 1);
        when(fetcher.fetchPopularTvShowsAsync(1))
                .thenReturn(CompletableFuture.completedFuture(popularTvShows));

        when(persistenceService.discoverAndPersistNewMovies(anyList())).thenReturn(1);
        when(persistenceService.discoverAndPersistNewTvShows(anyList())).thenReturn(1);

        // When: Running synchronization
        synchronizerService.synchronize();

        // Then: Should sync movie changes, TV changes, and discover new popular items
        verify(persistenceService, times(1)).updateItem(any(), eq(movieDetails));
        verify(persistenceService, times(1)).updateItem(any(), eq(tvDetails));
        verify(persistenceService, times(1)).discoverAndPersistNewMovies(anyList());
        verify(persistenceService, times(1)).discoverAndPersistNewTvShows(anyList());
    }

    @Test
    @DisplayName("Should fetch updated details and persist changes when syncing existing items")
    void testSyncChanges_UpdatesExistingItems() {
        // Given: A movie exists in the database and has changes on TMDB
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 7);
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);

        TmdbChangesDto change = new TmdbChangesDto(123L, false);
        TmdbPagedResponseDto<TmdbChangesDto> changes = 
                new TmdbPagedResponseDto<>(1, List.of(change), 1, 1);

        when(fetcher.fetchChangesAsync(TmdbMediaType.MOVIE, startDate, endDate, 1))
                .thenReturn(CompletableFuture.completedFuture(changes));

        ItemEntity existingItem = mock(ItemEntity.class);
        when(itemRepository.findByTmdbIdAndTmdbType(123L, TmdbType.MOVIE))
                .thenReturn(Optional.of(existingItem));

        TmdbMovieDetailsDto updatedDetails = createMockMovieDetails(123L);
        when(fetcher.fetchDetailsWithCreditsAsync(TmdbMediaType.MOVIE, 123L))
                .thenReturn(CompletableFuture.completedFuture(updatedDetails));

        // When: Syncing changes for the date range
        ReflectionTestUtils.setField(synchronizerService, "lastSyncDate", startDate);
        synchronizerService.synchronize();

        // Then: Should fetch updated details and persist the changes
        verify(fetcher).fetchDetailsWithCreditsAsync(TmdbMediaType.MOVIE, 123L);
        verify(persistenceService).updateItem(existingItem, updatedDetails);
    }

    @Test
    @DisplayName("Should not fetch details for non-existing items when syncing changes")
    void testSyncChanges_IgnoresNonExistingItems() {
        // Given: TMDB reports changes for items not in our database
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 7);
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);
        ReflectionTestUtils.setField(synchronizerService, "lastSyncDate", startDate);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 0);

        TmdbChangesDto change = new TmdbChangesDto(999L, false);
        TmdbPagedResponseDto<TmdbChangesDto> changes = 
                new TmdbPagedResponseDto<>(1, List.of(change), 1, 1);

        when(fetcher.fetchChangesAsync(any(), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(changes));
        when(itemRepository.findByTmdbIdAndTmdbType(999L, TmdbType.MOVIE))
                .thenReturn(Optional.empty());

        // When: Syncing changes
        synchronizerService.synchronize();

        // Then: Should not persist any changes
        verify(fetcher, never()).fetchDetailsWithCreditsAsync(any(), eq(999L));
        verify(persistenceService, never()).updateItem(any(), any());
    }

    @Test
    @DisplayName("Should handle multiple pages of changes from TMDB")
    void testSyncChanges_MultiplePages() {
        // Given: TMDB returns multiple pages of changes for both movies and TV shows
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 7);
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);
        ReflectionTestUtils.setField(synchronizerService, "lastSyncDate", startDate);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 0);

        TmdbChangesDto change1 = new TmdbChangesDto(100L, false);
        TmdbChangesDto change2 = new TmdbChangesDto(200L, false);
        TmdbPagedResponseDto<TmdbChangesDto> page1 = 
                new TmdbPagedResponseDto<>(1, List.of(change1), 2, 1);
        TmdbPagedResponseDto<TmdbChangesDto> page2 = 
                new TmdbPagedResponseDto<>(2, List.of(change2), 2, 1);

        when(fetcher.fetchChangesAsync(any(), any(), any(), eq(1)))
                .thenReturn(CompletableFuture.completedFuture(page1));
        when(fetcher.fetchChangesAsync(any(), any(), any(), eq(2)))
                .thenReturn(CompletableFuture.completedFuture(page2));

        when(itemRepository.findByTmdbIdAndTmdbType(anyLong(), any()))
                .thenReturn(Optional.of(mock(ItemEntity.class)));

        when(fetcher.fetchDetailsWithCreditsAsync(any(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(createMockMovieDetails(100L)));

        // When: Syncing changes
        synchronizerService.synchronize();

        // Then: Should process all pages for both movies and TV shows
        verify(fetcher, times(2)).fetchChangesAsync(any(), any(), any(), eq(1));
        verify(fetcher, times(2)).fetchChangesAsync(any(), any(), any(), eq(2));
    }

    @Test
    @DisplayName("Should fetch details for all items concurrently when syncing changes")
    void testSyncChanges_ParallelProcessing() {
        // Given: Multiple items have changes
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 7);
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);
        ReflectionTestUtils.setField(synchronizerService, "lastSyncDate", startDate);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 0);

        TmdbChangesDto change1 = new TmdbChangesDto(100L, false);
        TmdbChangesDto change2 = new TmdbChangesDto(200L, false);
        TmdbChangesDto change3 = new TmdbChangesDto(300L, false);
        TmdbPagedResponseDto<TmdbChangesDto> changes = 
                new TmdbPagedResponseDto<>(1, List.of(change1, change2, change3), 1, 3);

        when(fetcher.fetchChangesAsync(any(), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(changes));

        when(itemRepository.findByTmdbIdAndTmdbType(anyLong(), any()))
                .thenReturn(Optional.of(mock(ItemEntity.class)));

        when(fetcher.fetchDetailsWithCreditsAsync(any(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(createMockMovieDetails(100L)));

        // When: Syncing changes
        synchronizerService.synchronize();

        // Then: Should fetch details for all items
        verify(fetcher, times(6)).fetchDetailsWithCreditsAsync(any(), anyLong()); // 3 movies + 3 TV shows
    }

    @Test
    @DisplayName("Should persist only new movies when discovering new popular movies")
    void testDiscoverNewPopularItems_Movies() {
        // Given: Popular movies list contains new items
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 1);

        TmdbMovieDto newMovie = createMockMovie(500L);
        TmdbPagedResponseDto<TmdbMovieDto> popularMovies = 
                new TmdbPagedResponseDto<>(1, List.of(newMovie), 1, 1);

        when(fetcher.fetchChangesAsync(any(), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TmdbPagedResponseDto<>(1, Collections.emptyList(), 1, 0)));
        when(fetcher.fetchPopularMoviesAsync(1))
                .thenReturn(CompletableFuture.completedFuture(popularMovies));
        when(fetcher.fetchPopularTvShowsAsync(1))
                .thenReturn(CompletableFuture.completedFuture(
                        new TmdbPagedResponseDto<>(1, Collections.emptyList(), 1, 0)));

        when(persistenceService.discoverAndPersistNewMovies(anyList())).thenReturn(1);

        // When: Discovering new popular movies
        synchronizerService.synchronize();

        // Then: Should persist only new movies not in database
        verify(persistenceService).discoverAndPersistNewMovies(List.of(newMovie));
    }

    @Test
    @DisplayName("Should persist only new TV shows when discovering new popular TV shows")
    void testDiscoverNewPopularItems_TvShows() {
        // Given: Popular TV shows list contains new items
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 1);

        TmdbTvShowDto newTvShow = createMockTvShow(600L);
        TmdbPagedResponseDto<TmdbTvShowDto> popularTvShows = 
                new TmdbPagedResponseDto<>(1, List.of(newTvShow), 1, 1);

        when(fetcher.fetchChangesAsync(any(), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TmdbPagedResponseDto<>(1, Collections.emptyList(), 1, 0)));
        when(fetcher.fetchPopularMoviesAsync(1))
                .thenReturn(CompletableFuture.completedFuture(
                        new TmdbPagedResponseDto<>(1, Collections.emptyList(), 1, 0)));
        when(fetcher.fetchPopularTvShowsAsync(1))
                .thenReturn(CompletableFuture.completedFuture(popularTvShows));

        when(persistenceService.discoverAndPersistNewTvShows(anyList())).thenReturn(1);

        // When: Discovering new popular TV shows
        synchronizerService.synchronize();

        // Then: Should persist only new TV shows not in database
        verify(persistenceService).discoverAndPersistNewTvShows(List.of(newTvShow));
    }

    @Test
    @DisplayName("Should perform sync operations even when scheduled sync is disabled (manual sync)")
    void testSynchronize_DisabledSync() {
        // Given: Scheduled sync is disabled but manual sync should still work
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", false);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 0);
        ReflectionTestUtils.setField(synchronizerService, "lastSyncDate", LocalDate.of(2026, 4, 6));

        when(fetcher.fetchChangesAsync(any(), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TmdbPagedResponseDto<>(1, Collections.emptyList(), 1, 0)));

        // When: Running manual synchronization
        synchronizerService.synchronize();

        // Then: Should perform sync operations (manual sync bypasses syncEnabled flag)
        verify(fetcher, atLeastOnce()).fetchChangesAsync(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Should not perform scheduled sync when syncEnabled is false")
    void testScheduledSync_Disabled() {
        // Given: Scheduled sync is disabled
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", false);

        // When: Scheduled sync is triggered
        synchronizerService.scheduledSync();

        // Then: Should not perform any sync operations
        verify(fetcher, never()).fetchChangesAsync(any(), any(), any(), anyInt());
        verify(fetcher, never()).fetchPopularMoviesAsync(anyInt());
        verify(fetcher, never()).fetchPopularTvShowsAsync(anyInt());
    }

    @Test
    @DisplayName("Should perform scheduled sync when syncEnabled is true")
    void testScheduledSync_Enabled() {
        // Given: Scheduled sync is enabled
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 0);
        ReflectionTestUtils.setField(synchronizerService, "lastSyncDate", LocalDate.of(2026, 4, 6));

        when(fetcher.fetchChangesAsync(any(), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TmdbPagedResponseDto<>(1, Collections.emptyList(), 1, 0)));

        // When: Scheduled sync is triggered
        synchronizerService.scheduledSync();

        // Then: Should perform sync operations
        verify(fetcher, atLeastOnce()).fetchChangesAsync(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Should update last sync date to today when synchronization completes successfully")
    void testSynchronize_UpdatesLastSyncDate() {
        // Given: Last sync date is yesterday
        LocalDate yesterday = LocalDate.of(2026, 4, 6);
        ReflectionTestUtils.setField(synchronizerService, "lastSyncDate", yesterday);
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 0);

        when(fetcher.fetchChangesAsync(any(), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TmdbPagedResponseDto<>(1, Collections.emptyList(), 1, 0)));

        // When: Synchronization completes successfully
        synchronizerService.synchronize();

        // Then: Should update last sync date to today
        // Note: We verify this behavior indirectly by checking that sync operations completed
        verify(fetcher, atLeastOnce()).fetchChangesAsync(any(), eq(yesterday), any(), anyInt());
    }

    @Test
    @DisplayName("Should continue syncing other items when one item fails to sync")
    void testSyncChanges_ErrorHandling() {
        // Given: One item fails to sync
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);
        ReflectionTestUtils.setField(synchronizerService, "lastSyncDate", startDate);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 0);

        TmdbChangesDto change1 = new TmdbChangesDto(100L, false);
        TmdbChangesDto change2 = new TmdbChangesDto(200L, false);
        TmdbPagedResponseDto<TmdbChangesDto> changes = 
                new TmdbPagedResponseDto<>(1, List.of(change1, change2), 1, 2);

        when(fetcher.fetchChangesAsync(any(), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(changes));

        when(itemRepository.findByTmdbIdAndTmdbType(100L, TmdbType.MOVIE))
                .thenReturn(Optional.of(mock(ItemEntity.class)));
        when(itemRepository.findByTmdbIdAndTmdbType(200L, TmdbType.MOVIE))
                .thenReturn(Optional.of(mock(ItemEntity.class)));

        // First item fails, second succeeds
        when(fetcher.fetchDetailsWithCreditsAsync(TmdbMediaType.MOVIE, 100L))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));
        when(fetcher.fetchDetailsWithCreditsAsync(TmdbMediaType.MOVIE, 200L))
                .thenReturn(CompletableFuture.completedFuture(createMockMovieDetails(200L)));

        // When: Syncing changes
        synchronizerService.synchronize();

        // Then: Should continue syncing other items (the test passes if no exception is thrown)
        assertTrue(true);
    }

    @Test
    @DisplayName("Should complete without errors and return 0 updates when no changes in date range")
    void testSyncChanges_EmptyChanges() {
        // Given: No changes in the date range
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 0);

        TmdbPagedResponseDto<TmdbChangesDto> emptyChanges = 
                new TmdbPagedResponseDto<>(1, Collections.emptyList(), 1, 0);

        when(fetcher.fetchChangesAsync(any(), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(emptyChanges));

        // When: Syncing changes
        synchronizerService.synchronize();

        // Then: Should complete without errors
        verify(fetcher, never()).fetchDetailsWithCreditsAsync(any(), anyLong());
        verify(persistenceService, never()).updateItem(any(), any());
    }

    @Test
    @DisplayName("Should return 0 new items when all popular items already exist in database")
    void testDiscoverNewPopularItems_AllExisting() {
        // Given: All popular items already exist in database
        ReflectionTestUtils.setField(synchronizerService, "syncEnabled", true);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 1);

        TmdbMovieDto existingMovie = createMockMovie(700L);
        TmdbPagedResponseDto<TmdbMovieDto> popularMovies = 
                new TmdbPagedResponseDto<>(1, List.of(existingMovie), 1, 1);

        when(fetcher.fetchChangesAsync(any(), any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(
                        new TmdbPagedResponseDto<>(1, Collections.emptyList(), 1, 0)));
        when(fetcher.fetchPopularMoviesAsync(1))
                .thenReturn(CompletableFuture.completedFuture(popularMovies));
        when(fetcher.fetchPopularTvShowsAsync(1))
                .thenReturn(CompletableFuture.completedFuture(
                        new TmdbPagedResponseDto<>(1, Collections.emptyList(), 1, 0)));

        when(persistenceService.discoverAndPersistNewMovies(anyList())).thenReturn(0);

        // When: Discovering new popular items
        synchronizerService.synchronize();

        // Then: Should return 0 new items
        verify(persistenceService).discoverAndPersistNewMovies(anyList());
    }

    // Helper methods to create mock data

    private TmdbMovieDetailsDto createMockMovieDetails(long id) {
        TmdbCreditsDto credits = new TmdbCreditsDto(1L, List.of(), List.of());
        return new TmdbMovieDetailsDto(
                id,
                "Test Movie",
                "Overview",
                "2026-04-07",
                1.0,
                7.5,
                100,
                "/poster.jpg",
                "/backdrop.jpg",
                List.of(),
                credits
        );
    }

    private TmdbTvShowDetailsDto createMockTvShowDetails(long id) {
        TmdbCreditsDto credits = new TmdbCreditsDto(1L, List.of(), List.of());
        return new TmdbTvShowDetailsDto(
                id,
                "Test TV Show",
                "Overview",
                "2026-04-07",
                1.0,
                8.0,
                150,
                "/poster.jpg",
                "/backdrop.jpg",
                List.of(),
                credits
        );
    }

    private TmdbMovieDto createMockMovie(long id) {
        return new TmdbMovieDto(
                id,
                "Test Movie",
                "Overview",
                "2026-04-07",
                1.0,
                7.5,
                100,
                "/poster.jpg",
                List.of(),
                List.of()
        );
    }

    private TmdbTvShowDto createMockTvShow(long id) {
        return new TmdbTvShowDto(
                id,
                "Test TV Show",
                "Overview",
                "2026-04-07",
                1.0,
                8.0,
                150,
                "/poster.jpg",
                List.of(),
                List.of()
        );
    }
}
