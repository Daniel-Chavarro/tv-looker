package org.tvl.tvlooker.service.tmdb;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TmdbDataSynchronizerService.
 * Tests synchronization logic with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TmdbDataSynchronizerService Unit Tests")
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
    @DisplayName("Should synchronize changes and discover new popular items")
    void testSynchronize_UpdatesLastSyncDateAndPersistsNewItems() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 3, 10);
        ReflectionTestUtils.setField(synchronizerService, "lastSyncDate", startDate);
        ReflectionTestUtils.setField(synchronizerService, "popularPages", 1);

        TmdbChangesDto movieChange = new TmdbChangesDto(100L, false);
        TmdbChangesDto tvChange = new TmdbChangesDto(200L, false);

        TmdbPagedResponseDto<TmdbChangesDto> movieChanges = new TmdbPagedResponseDto<>(
                1,
                List.of(movieChange),
                1,
                1
        );
        TmdbPagedResponseDto<TmdbChangesDto> tvChanges = new TmdbPagedResponseDto<>(
                1,
                List.of(tvChange),
                1,
                1
        );

        when(fetcher.fetchChangesAsync(eq(TmdbMediaType.MOVIE), eq(startDate), any(LocalDate.class), eq(1)))
                .thenReturn(CompletableFuture.completedFuture(movieChanges));
        when(fetcher.fetchChangesAsync(eq(TmdbMediaType.TV), eq(startDate), any(LocalDate.class), eq(1)))
                .thenReturn(CompletableFuture.completedFuture(tvChanges));

        ItemEntity existingMovie = new ItemEntity();
        existingMovie.setTmdbId(100L);
        existingMovie.setTmdbType(TmdbType.MOVIE);
        existingMovie.setTitle("Existing Movie");
        ItemEntity existingTvShow = new ItemEntity();
        existingTvShow.setTmdbId(200L);
        existingTvShow.setTmdbType(TmdbType.TV);
        existingTvShow.setTitle("Existing TV");

        when(itemRepository.findByTmdbIdAndTmdbType(100L, TmdbType.MOVIE))
                .thenReturn(Optional.of(existingMovie));
        when(itemRepository.findByTmdbIdAndTmdbType(200L, TmdbType.TV))
                .thenReturn(Optional.of(existingTvShow));

        TmdbMovieDto newMovie = new TmdbMovieDto(
                300L,
                "New Movie",
                "Overview",
                "2026-03-15",
                1.0,
                7.5,
                50,
                "/poster.jpg",
                List.of(),
                List.of()
        );
        TmdbTvShowDto newTvShow = new TmdbTvShowDto(
                400L,
                "New TV",
                "Overview",
                "2026-03-10",
                1.0,
                7.5,
                50,
                "/poster.jpg",
                List.of(),
                List.of()
        );

        TmdbPagedResponseDto<TmdbMovieDto> popularMovies = new TmdbPagedResponseDto<>(
                1,
                List.of(newMovie),
                1,
                1
        );
        TmdbPagedResponseDto<TmdbTvShowDto> popularTvShows = new TmdbPagedResponseDto<>(
                1,
                List.of(newTvShow),
                1,
                1
        );

        when(fetcher.fetchPopularMoviesAsync(1)).thenReturn(CompletableFuture.completedFuture(popularMovies));
        when(fetcher.fetchPopularTvShowsAsync(1)).thenReturn(CompletableFuture.completedFuture(popularTvShows));
        when(itemRepository.existsByTmdbIdAndTmdbType(300L, TmdbType.MOVIE)).thenReturn(false);
        when(itemRepository.existsByTmdbIdAndTmdbType(400L, TmdbType.TV)).thenReturn(false);

        // When
        synchronizerService.synchronize();

        // Then
        ArgumentCaptor<LocalDate> endDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(fetcher).fetchChangesAsync(eq(TmdbMediaType.MOVIE), eq(startDate), endDateCaptor.capture(), eq(1));
        LocalDate endDate = endDateCaptor.getValue();


        verify(persistenceService, times(1)).discoverAndPersistNewMovies(anyList());
        verify(persistenceService, times(1)).discoverAndPersistNewTvShows(anyList());
    }
}