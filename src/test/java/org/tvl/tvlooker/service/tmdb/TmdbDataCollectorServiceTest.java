package org.tvl.tvlooker.service.tmdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.tvl.tvlooker.domain.exception.TmdbCollectionInProgressException;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.service.tmdb.EntityCacheService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TmdbDataCollectorService.
 * Tests collection logic with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TmdbDataCollectorService Unit Tests")
class TmdbDataCollectorServiceTest {

    @Mock
    private TmdbDataFetcher dataFetcher;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private TmdbItemPersistenceService persistenceService;

    @InjectMocks
    private TmdbDataCollectorService collectorService;

    @BeforeEach
    void setUp() {
        // Set maxPages to 1 for testing to avoid long loops
        ReflectionTestUtils.setField(collectorService, "maxPages", 1);
    }

    @Test
    @DisplayName("Should successfully collect genres from TMDB")
    void testCollectGenres_Success() {
        // Given
        TmdbGenreDto actionGenre = new TmdbGenreDto(28, "Action");
        TmdbGenreDto dramaGenre = new TmdbGenreDto(18, "Drama");
        TmdbGenreListDto movieGenres = new TmdbGenreListDto(List.of(actionGenre, dramaGenre));
        TmdbGenreListDto tvGenres = new TmdbGenreListDto(List.of(dramaGenre));

        when(dataFetcher.fetchGenresAsync(TmdbMediaType.MOVIE)).thenReturn(CompletableFuture.completedFuture(movieGenres));
        when(dataFetcher.fetchGenresAsync(TmdbMediaType.TV)).thenReturn(CompletableFuture.completedFuture(tvGenres));
        when(genreRepository.findByTmdbId(anyLong())).thenReturn(Optional.empty());
        when(genreRepository.save(any(GenreEntity.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        // When
        collectorService.collectGenres();

        // Then
        verify(dataFetcher, times(1)).fetchGenresAsync(TmdbMediaType.MOVIE);
        verify(dataFetcher, times(1)).fetchGenresAsync(TmdbMediaType.TV);
        verify(genreRepository, atLeast(2)).save(any(GenreEntity.class));
    }

    @Test
    @DisplayName("Should not create duplicate genres when tmdbId already exists")
    void testCollectGenres_NoDuplicates() {
        // Given
        TmdbGenreDto actionGenre = new TmdbGenreDto(28, "Action");
        TmdbGenreListDto movieGenres = new TmdbGenreListDto(List.of(actionGenre));
        TmdbGenreListDto tvGenres = new TmdbGenreListDto(List.of());

        GenreEntity existingGenre = GenreEntity.builder()
                .tmdbId(28L)
                .name("Action")
                .build();

        when(dataFetcher.fetchGenresAsync(TmdbMediaType.MOVIE)).thenReturn(CompletableFuture.completedFuture(movieGenres));
        when(dataFetcher.fetchGenresAsync(TmdbMediaType.TV)).thenReturn(CompletableFuture.completedFuture(tvGenres));
        when(genreRepository.findByTmdbId(28L)).thenReturn(Optional.of(existingGenre));

        // When
        collectorService.collectGenres();

        // Then
        verify(dataFetcher, times(1)).fetchGenresAsync(TmdbMediaType.MOVIE);
        verify(dataFetcher, times(1)).fetchGenresAsync(TmdbMediaType.TV);
        verify(genreRepository, never()).save(any(GenreEntity.class));
    }

    @Test
    @DisplayName("Should successfully collect popular movies")
    void testCollectPopularMovies_Success() {
        // Given
        TmdbMovieDto movie = new TmdbMovieDto(
                123L,
                "Test Movie",
                "Overview",
                "2026-03-15",
                1.0,
                8.0,
                100,
                "/poster.jpg",
                List.of(),
                List.of()
        );

        TmdbPagedResponseDto<TmdbMovieDto> response = new TmdbPagedResponseDto<>(
                1,
                List.of(movie),
                1,
                1
        );

        when(dataFetcher.fetchPopularMoviesAsync(1)).thenReturn(CompletableFuture.completedFuture(response));
        when(itemRepository.existsByTmdbIdAndTmdbType(123L, TmdbType.MOVIE)).thenReturn(false);

        // When
        collectorService.collectPopularMovies();

        // Then
        verify(dataFetcher, times(1)).fetchPopularMoviesAsync(1);
        verify(persistenceService, times(1)).discoverAndPersistNewMovies(anyList());
    }

    @Test
    @DisplayName("Should successfully collect popular TV shows")
    void testCollectPopularTvShows_Success() {
        // Given
        TmdbTvShowDto tvShow = new TmdbTvShowDto(
                456L,
                "Test TV Show",
                "Overview",
                "2026-03-10",
                1.0,
                8.0,
                100,
                "/poster.jpg",
                List.of(),
                List.of()
        );

        TmdbPagedResponseDto<TmdbTvShowDto> response = new TmdbPagedResponseDto<>(
                1,
                List.of(tvShow),
                1,
                1
        );

        when(dataFetcher.fetchPopularTvShowsAsync(1)).thenReturn(CompletableFuture.completedFuture(response));
        when(itemRepository.existsByTmdbIdAndTmdbType(456L, TmdbType.TV)).thenReturn(false);

        // When
        collectorService.collectPopularTvShows();

        // Then
        verify(dataFetcher, times(1)).fetchPopularTvShowsAsync(1);
        verify(persistenceService, times(1)).discoverAndPersistNewTvShows(anyList());
    }

    @Test
    @DisplayName("Should prevent concurrent collection operations")
    void testCollectAllAsync_PreventsConcurrent() {
        // Given: manually set the flag to simulate concurrent execution
        ReflectionTestUtils.setField(collectorService, "collectionInProgress",
                new java.util.concurrent.atomic.AtomicBoolean(true));

        // When/Then
        assertThrows(TmdbCollectionInProgressException.class,
                () -> collectorService.collectAllAsync());
    }

    @Test
    @DisplayName("Should return isCollectionInProgress as false initially")
    void testIsCollectionInProgress_InitiallyFalse() {
        // Then
        assertFalse(collectorService.isCollectionInProgress());
    }

    @Test
    @DisplayName("Should skip already existing movies")
    void testCollectPopularMovies_SkipsExisting() {
        // Given
        TmdbMovieDto movie = new TmdbMovieDto(
                123L,
                "Existing Movie",
                "Overview",
                "2026-03-15",
                1.0,
                8.0,
                100,
                "/poster.jpg",
                List.of(),
                List.of()
        );

        TmdbPagedResponseDto<TmdbMovieDto> response = new TmdbPagedResponseDto<>(
                1,
                List.of(movie),
                1,
                1
        );

        when(dataFetcher.fetchPopularMoviesAsync(1)).thenReturn(CompletableFuture.completedFuture(response));
        when(itemRepository.existsByTmdbIdAndTmdbType(123L, TmdbType.MOVIE)).thenReturn(true); // Already exists

        // When
        collectorService.collectPopularMovies();

        // Then
        verify(dataFetcher, times(1)).fetchPopularMoviesAsync(1);
        verify(persistenceService, never()).discoverAndPersistNewMovies(anyList());
    }

    @Test
    @DisplayName("Should skip already existing TV shows")
    void testCollectPopularTvShows_SkipsExisting() {
        // Given
        TmdbTvShowDto tvShow = new TmdbTvShowDto(
                456L,
                "Existing TV Show",
                "Overview",
                "2026-03-10",
                1.0,
                8.0,
                100,
                "/poster.jpg",
                List.of(),
                List.of()
        );

        TmdbPagedResponseDto<TmdbTvShowDto> response = new TmdbPagedResponseDto<>(
                1,
                List.of(tvShow),
                1,
                1
        );

        when(dataFetcher.fetchPopularTvShowsAsync(1)).thenReturn(CompletableFuture.completedFuture(response));
        when(itemRepository.existsByTmdbIdAndTmdbType(456L, TmdbType.TV)).thenReturn(true); // Already exists

        // When
        collectorService.collectPopularTvShows();

        // Then
        verify(dataFetcher, times(1)).fetchPopularTvShowsAsync(1);
        verify(persistenceService, never()).discoverAndPersistNewTvShows(anyList());
    }

    @Test
    @DisplayName("Should handle empty results gracefully")
    void testCollectPopularMovies_EmptyResults() {
        // Given
        TmdbPagedResponseDto<TmdbMovieDto> emptyResponse = new TmdbPagedResponseDto<>(
                1,
                Collections.emptyList(),
                0,
                0
        );

        when(dataFetcher.fetchPopularMoviesAsync(1)).thenReturn(CompletableFuture.completedFuture(emptyResponse));

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> collectorService.collectPopularMovies());
        verify(persistenceService, never()).discoverAndPersistNewMovies(anyList());
    }

    @Test
    @DisplayName("Should handle empty genre list gracefully")
    void testCollectGenres_EmptyList() {
        // Given
        TmdbGenreListDto emptyGenres = new TmdbGenreListDto(List.of());

        when(dataFetcher.fetchGenresAsync(TmdbMediaType.MOVIE)).thenReturn(CompletableFuture.completedFuture(emptyGenres));
        when(dataFetcher.fetchGenresAsync(TmdbMediaType.TV)).thenReturn(CompletableFuture.completedFuture(emptyGenres));

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> collectorService.collectGenres());
        verify(genreRepository, never()).save(any());
    }
}