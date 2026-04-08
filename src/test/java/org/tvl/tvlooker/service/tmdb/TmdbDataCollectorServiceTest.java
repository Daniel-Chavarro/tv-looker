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
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Behavior-driven tests for TmdbDataCollectorService.
 * Tests follow Given-When-Then pattern and focus on what methods do and return,
 * not on implementation details.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TmdbDataCollectorService Behavior Tests")
class TmdbDataCollectorServiceTest {

    @Mock
    private TmdbDataFetcher dataFetcher;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private TmdbItemPersistenceService persistenceService;

    @InjectMocks
    private TmdbDataCollectorService collectorService;

    @BeforeEach
    void setUp() {
        // Set maxPages to 1 for testing to avoid long loops
        ReflectionTestUtils.setField(collectorService, "maxPages", 1);
        ReflectionTestUtils.setField(collectorService, "batchSize", 50);
    }

    @Test
    @DisplayName("Should collect genres, movies, and TV shows when collecting all data")
    void testCollectAll_CompletesFullFlow() {
        // Given: Empty database
        TmdbGenreDto actionGenre = new TmdbGenreDto(28, "Action");
        TmdbGenreListDto movieGenres = new TmdbGenreListDto(List.of(actionGenre));
        TmdbGenreListDto tvGenres = new TmdbGenreListDto(List.of(actionGenre));

        when(dataFetcher.fetchGenresAsync(TmdbMediaType.MOVIE))
                .thenReturn(CompletableFuture.completedFuture(movieGenres));
        when(dataFetcher.fetchGenresAsync(TmdbMediaType.TV))
                .thenReturn(CompletableFuture.completedFuture(tvGenres));

        TmdbMovieDto movie = createMockMovie(100L);
        TmdbPagedResponseDto<TmdbMovieDto> movieResponse = 
                new TmdbPagedResponseDto<>(1, List.of(movie), 1, 1);
        when(dataFetcher.fetchPopularMoviesAsync(1))
                .thenReturn(CompletableFuture.completedFuture(movieResponse));

        TmdbTvShowDto tvShow = createMockTvShow(200L);
        TmdbPagedResponseDto<TmdbTvShowDto> tvResponse = 
                new TmdbPagedResponseDto<>(1, List.of(tvShow), 1, 1);
        when(dataFetcher.fetchPopularTvShowsAsync(1))
                .thenReturn(CompletableFuture.completedFuture(tvResponse));

        when(itemRepository.existsByTmdbIdAndTmdbType(200L, TmdbType.TV)).thenReturn(false);
        when(dataFetcher.fetchTvShowsDetailsBatch(anyList()))
                .thenReturn(List.of(createMockTvShowDetails(200L)));

        // When: Collecting all data
        collectorService.collectAll();

        // Then: Should collect genres, movies, and TV shows
        verify(persistenceService, times(2)).persistGenres(any());
        verify(persistenceService, times(1)).discoverAndPersistNewMovies(anyList());
        verify(persistenceService, times(1)).persistItems(anyList());
    }

    @Test
    @DisplayName("Should fetch and persist both movie and TV genres when collecting genres")
    void testCollectGenres_Success() {
        // Given: TMDB has movie and TV genres
        TmdbGenreDto actionGenre = new TmdbGenreDto(28, "Action");
        TmdbGenreDto dramaGenre = new TmdbGenreDto(18, "Drama");
        TmdbGenreListDto movieGenres = new TmdbGenreListDto(List.of(actionGenre, dramaGenre));
        TmdbGenreListDto tvGenres = new TmdbGenreListDto(List.of(dramaGenre));

        when(dataFetcher.fetchGenresAsync(TmdbMediaType.MOVIE))
                .thenReturn(CompletableFuture.completedFuture(movieGenres));
        when(dataFetcher.fetchGenresAsync(TmdbMediaType.TV))
                .thenReturn(CompletableFuture.completedFuture(tvGenres));

        // When: Collecting genres
        collectorService.collectGenres();

        // Then: Should fetch and persist both movie and TV genres
        verify(dataFetcher, times(1)).fetchGenresAsync(TmdbMediaType.MOVIE);
        verify(dataFetcher, times(1)).fetchGenresAsync(TmdbMediaType.TV);
        verify(persistenceService, times(1)).persistGenres(movieGenres);
        verify(persistenceService, times(1)).persistGenres(tvGenres);
    }

    @Test
    @DisplayName("Should complete without errors when TMDB returns empty genre lists")
    void testCollectGenres_EmptyResults() {
        // Given: TMDB returns empty genre lists
        TmdbGenreListDto emptyGenres = new TmdbGenreListDto(List.of());

        when(dataFetcher.fetchGenresAsync(TmdbMediaType.MOVIE))
                .thenReturn(CompletableFuture.completedFuture(emptyGenres));
        when(dataFetcher.fetchGenresAsync(TmdbMediaType.TV))
                .thenReturn(CompletableFuture.completedFuture(emptyGenres));

        // When: Collecting genres
        // Then: Should complete without errors
        assertDoesNotThrow(() -> collectorService.collectGenres());
        verify(persistenceService, times(2)).persistGenres(emptyGenres);
    }

    @Test
    @DisplayName("Should discover and persist new movies when collecting popular movies")
    void testCollectPopularMovies_NewMovies() {
        // Given: Popular movies list contains items not in database
        TmdbMovieDto movie = createMockMovie(123L);
        TmdbPagedResponseDto<TmdbMovieDto> response = 
                new TmdbPagedResponseDto<>(1, List.of(movie), 1, 1);

        when(dataFetcher.fetchPopularMoviesAsync(1))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(persistenceService.discoverAndPersistNewMovies(anyList())).thenReturn(1);

        // When: Collecting popular movies
        collectorService.collectPopularMovies();

        // Then: Should discover and persist new movies
        verify(dataFetcher, times(1)).fetchPopularMoviesAsync(1);
        verify(persistenceService, times(1)).discoverAndPersistNewMovies(List.of(movie));
    }

    @Test
    @DisplayName("Should skip existing movies and only add new ones when collecting popular movies")
    void testCollectPopularMovies_ExistingMovies() {
        // Given: Some movies already exist in database
        TmdbMovieDto existingMovie = createMockMovie(123L);
        TmdbMovieDto newMovie = createMockMovie(456L);
        TmdbPagedResponseDto<TmdbMovieDto> response = 
                new TmdbPagedResponseDto<>(1, List.of(existingMovie, newMovie), 1, 2);

        when(dataFetcher.fetchPopularMoviesAsync(1))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(persistenceService.discoverAndPersistNewMovies(anyList())).thenReturn(1);

        // When: Collecting popular movies
        collectorService.collectPopularMovies();

        // Then: Should skip existing movies and only add new ones
        verify(persistenceService, times(1)).discoverAndPersistNewMovies(anyList());
    }

    @Test
    @DisplayName("Should fetch and process 3 pages when max pages is set to 3")
    void testCollectPopularMovies_MultiplePages() {
        // Given: Max pages is set to 3
        ReflectionTestUtils.setField(collectorService, "maxPages", 3);

        TmdbMovieDto movie = createMockMovie(123L);
        TmdbPagedResponseDto<TmdbMovieDto> response = 
                new TmdbPagedResponseDto<>(1, List.of(movie), 10, 1);

        when(dataFetcher.fetchPopularMoviesAsync(anyInt()))
                .thenReturn(CompletableFuture.completedFuture(response));

        // When: Collecting popular movies
        collectorService.collectPopularMovies();

        // Then: Should fetch and process 3 pages
        verify(dataFetcher, times(3)).fetchPopularMoviesAsync(anyInt());
    }

    @Test
    @DisplayName("Should complete without errors when TMDB returns empty results")
    void testCollectPopularMovies_EmptyResults() {
        // Given: TMDB returns empty results
        TmdbPagedResponseDto<TmdbMovieDto> emptyResponse = 
                new TmdbPagedResponseDto<>(1, Collections.emptyList(), 0, 0);

        when(dataFetcher.fetchPopularMoviesAsync(1))
                .thenReturn(CompletableFuture.completedFuture(emptyResponse));

        // When: Collecting popular movies
        // Then: Should complete without errors
        assertDoesNotThrow(() -> collectorService.collectPopularMovies());
        verify(persistenceService, never()).discoverAndPersistNewMovies(anyList());
    }

    @Test
    @DisplayName("Should batch fetch details and persist new TV shows when collecting popular TV shows")
    void testCollectPopularTvShows_NewTvShows() {
        // Given: Popular TV shows list contains items not in database
        TmdbTvShowDto tvShow = createMockTvShow(456L);
        TmdbPagedResponseDto<TmdbTvShowDto> response = 
                new TmdbPagedResponseDto<>(1, List.of(tvShow), 1, 1);

        when(dataFetcher.fetchPopularTvShowsAsync(1))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(itemRepository.existsByTmdbIdAndTmdbType(456L, TmdbType.TV)).thenReturn(false);

        TmdbTvShowDetailsDto tvShowDetails = createMockTvShowDetails(456L);
        when(dataFetcher.fetchTvShowsDetailsBatch(List.of(456L)))
                .thenReturn(List.of(tvShowDetails));

        // When: Collecting popular TV shows
        collectorService.collectPopularTvShows();

        // Then: Should batch fetch details and persist new TV shows
        verify(dataFetcher, times(1)).fetchPopularTvShowsAsync(1);
        verify(dataFetcher, times(1)).fetchTvShowsDetailsBatch(List.of(456L));
        verify(persistenceService, times(1)).persistItems(List.of(tvShowDetails));
    }

    @Test
    @DisplayName("Should skip TV shows that already exist in database")
    void testCollectPopularTvShows_ExistingTvShows() {
        // Given: Some TV shows already exist in database
        TmdbTvShowDto existingTvShow = createMockTvShow(456L);
        TmdbPagedResponseDto<TmdbTvShowDto> response = 
                new TmdbPagedResponseDto<>(1, List.of(existingTvShow), 1, 1);

        when(dataFetcher.fetchPopularTvShowsAsync(1))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(itemRepository.existsByTmdbIdAndTmdbType(456L, TmdbType.TV)).thenReturn(true);
        when(dataFetcher.fetchTvShowsDetailsBatch(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When: Collecting popular TV shows
        collectorService.collectPopularTvShows();

        // Then: Should skip existing TV shows and call batch fetch with empty list
        verify(dataFetcher, times(1)).fetchTvShowsDetailsBatch(Collections.emptyList());
        verify(persistenceService, times(1)).persistItems(Collections.emptyList());
    }

    @Test
    @DisplayName("Should complete without errors when TMDB returns empty TV show results")
    void testCollectPopularTvShows_EmptyResults() {
        // Given: TMDB returns empty results
        TmdbPagedResponseDto<TmdbTvShowDto> emptyResponse = 
                new TmdbPagedResponseDto<>(1, Collections.emptyList(), 0, 0);

        when(dataFetcher.fetchPopularTvShowsAsync(1))
                .thenReturn(CompletableFuture.completedFuture(emptyResponse));

        // When: Collecting popular TV shows
        // Then: Should complete without errors
        assertDoesNotThrow(() -> collectorService.collectPopularTvShows());
        verify(persistenceService, times(1)).persistItems(Collections.emptyList());
    }

    @Test
    @DisplayName("Should throw TmdbCollectionInProgressException when collection is already in progress")
    void testCollectAllAsync_PreventsConcurrent() {
        // Given: Collection is already in progress
        ReflectionTestUtils.setField(collectorService, "collectionInProgress",
                new java.util.concurrent.atomic.AtomicBoolean(true));

        // When/Then: Trying to start another collection should throw exception
        assertThrows(TmdbCollectionInProgressException.class,
                () -> collectorService.collectAllAsync());
    }

    @Test
    @DisplayName("Should complete successfully and reset progress flag when collecting movies asynchronously")
    void testCollectPopularMoviesAsync_Success() {
        // Given: Valid configuration
        TmdbMovieDto movie = createMockMovie(123L);
        TmdbPagedResponseDto<TmdbMovieDto> response = 
                new TmdbPagedResponseDto<>(1, List.of(movie), 1, 1);

        when(dataFetcher.fetchPopularMoviesAsync(1))
                .thenReturn(CompletableFuture.completedFuture(response));

        // When: Collecting movies asynchronously
        CompletableFuture<Void> result = collectorService.collectPopularMoviesAsync();

        // Then: Should complete successfully and reset progress flag
        assertDoesNotThrow(result::join);
        assertFalse(collectorService.isCollectionInProgress());
    }

    @Test
    @DisplayName("Should complete successfully and reset progress flag when collecting TV shows asynchronously")
    void testCollectPopularTvShowsAsync_Success() {
        // Given: Valid configuration
        TmdbTvShowDto tvShow = createMockTvShow(456L);
        TmdbPagedResponseDto<TmdbTvShowDto> response = 
                new TmdbPagedResponseDto<>(1, List.of(tvShow), 1, 1);

        when(dataFetcher.fetchPopularTvShowsAsync(1))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(itemRepository.existsByTmdbIdAndTmdbType(456L, TmdbType.TV)).thenReturn(false);
        when(dataFetcher.fetchTvShowsDetailsBatch(anyList()))
                .thenReturn(List.of(createMockTvShowDetails(456L)));

        // When: Collecting TV shows asynchronously
        CompletableFuture<Void> result = collectorService.collectPopularTvShowsAsync();

        // Then: Should complete successfully and reset progress flag
        assertDoesNotThrow(result::join);
        assertFalse(collectorService.isCollectionInProgress());
    }

    @Test
    @DisplayName("Should return false when checking collection progress initially")
    void testIsCollectionInProgress_InitiallyFalse() {
        // Given: Service just initialized
        // When: Checking collection progress
        // Then: Should return false
        assertFalse(collectorService.isCollectionInProgress());
    }

    @Test
    @DisplayName("Should only fetch 2 pages when max pages is set to 2 but TMDB has 10 pages")
    void testCollectPopularMovies_StopsAtMaxPages() {
        // Given: Max pages is set to 2 but TMDB has 10 pages
        ReflectionTestUtils.setField(collectorService, "maxPages", 2);

        TmdbMovieDto movie = createMockMovie(123L);
        TmdbPagedResponseDto<TmdbMovieDto> response = 
                new TmdbPagedResponseDto<>(1, List.of(movie), 10, 1);

        when(dataFetcher.fetchPopularMoviesAsync(anyInt()))
                .thenReturn(CompletableFuture.completedFuture(response));

        // When: Collecting popular movies
        collectorService.collectPopularMovies();

        // Then: Should only fetch 2 pages
        verify(dataFetcher, times(2)).fetchPopularMoviesAsync(anyInt());
    }

    @Test
    @DisplayName("Should only fetch 1 page when TMDB has only 1 page but max pages is 5")
    void testCollectPopularTvShows_StopsAtTotalPages() {
        // Given: TMDB has only 1 page but max pages is 5
        ReflectionTestUtils.setField(collectorService, "maxPages", 5);

        TmdbTvShowDto tvShow = createMockTvShow(456L);
        TmdbPagedResponseDto<TmdbTvShowDto> response = 
                new TmdbPagedResponseDto<>(1, List.of(tvShow), 1, 1);

        when(dataFetcher.fetchPopularTvShowsAsync(1))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(itemRepository.existsByTmdbIdAndTmdbType(456L, TmdbType.TV)).thenReturn(false);
        when(dataFetcher.fetchTvShowsDetailsBatch(anyList()))
                .thenReturn(List.of(createMockTvShowDetails(456L)));

        // When: Collecting popular TV shows
        collectorService.collectPopularTvShows();

        // Then: Should only fetch 1 page
        verify(dataFetcher, times(1)).fetchPopularTvShowsAsync(1);
        verify(dataFetcher, never()).fetchPopularTvShowsAsync(2);
    }

    // Helper methods to create mock data

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
}
