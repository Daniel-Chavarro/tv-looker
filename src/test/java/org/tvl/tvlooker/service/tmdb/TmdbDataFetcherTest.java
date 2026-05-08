package org.tvl.tvlooker.service.tmdb;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.*;
import org.tvl.tvlooker.service.tmdb.support.TmdbEvidenceWriter;

import java.time.LocalDate;
import java.time.Duration;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Behavior-driven tests for TmdbDataFetcher.
 * Tests follow Given-When-Then pattern and focus on what methods do and return,
 * not on implementation details.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TmdbDataFetcher Behavior Tests")
class TmdbDataFetcherTest {

    private static final Path TASK_5_EVIDENCE_DIR = Path.of(
            ".sisyphus/evidence/collector-thread-blocking/task-5");

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private Executor executor;

    private TmdbDataFetcher dataFetcher;

    @BeforeEach
    void setUp() {
        // Execute tasks synchronously in tests for simplicity
        Executor syncExecutor = Runnable::run;
        dataFetcher = new TmdbDataFetcher(tmdbClient, syncExecutor, 35.0, 100);
    }

    @Test
    @DisplayName("Should return movie details with credits when fetching with generic method")
    void testFetchDetailsWithCreditsAsync_Movie_ReturnsMovieDetails() {
        // Given: A valid media type (MOVIE) and ID
        long movieId = 123L;
        TmdbMovieDetailsDto expectedMovie = createMockMovieDetails(movieId);
        when(tmdbClient.getDetailsWithCredits(TmdbMediaType.MOVIE, movieId))
                .thenReturn(expectedMovie);

        // When: Fetching details with credits
        CompletableFuture<TmdbMovieDetailsDto> result = 
                dataFetcher.fetchDetailsWithCreditsAsync(TmdbMediaType.MOVIE, movieId);

        // Then: Should return movie details with credits
        assertNotNull(result);
        TmdbMovieDetailsDto movie = result.join();
        assertNotNull(movie);
        assertEquals(movieId, movie.id());
        assertEquals("Test Movie", movie.title());
        assertNotNull(movie.credits());
    }

    @Test
    @DisplayName("Should return TV show details with credits when fetching with generic method")
    void testFetchDetailsWithCreditsAsync_TvShow_ReturnsTvShowDetails() {
        // Given: A valid media type (TV) and ID
        long tvShowId = 456L;
        TmdbTvShowDetailsDto expectedTvShow = createMockTvShowDetails(tvShowId);
        when(tmdbClient.getDetailsWithCredits(TmdbMediaType.TV, tvShowId))
                .thenReturn(expectedTvShow);

        // When: Fetching details with credits
        CompletableFuture<TmdbTvShowDetailsDto> result = 
                dataFetcher.fetchDetailsWithCreditsAsync(TmdbMediaType.TV, tvShowId);

        // Then: Should return TV show details with credits
        assertNotNull(result);
        TmdbTvShowDetailsDto tvShow = result.join();
        assertNotNull(tvShow);
        assertEquals(tvShowId, tvShow.id());
        assertEquals("Test TV Show", tvShow.name());
        assertNotNull(tvShow.credits());
    }

    @Test
    @DisplayName("Should return paged response with changes when fetching changes")
    void testFetchChangesAsync_ReturnsPagedChanges() {
        // Given: A media type, start date, end date, and page number
        TmdbMediaType mediaType = TmdbMediaType.MOVIE;
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 7);
        int page = 1;
        
        TmdbChangesDto change1 = new TmdbChangesDto(100L, false);
        TmdbChangesDto change2 = new TmdbChangesDto(200L, false);
        TmdbPagedResponseDto<TmdbChangesDto> expectedResponse = 
                new TmdbPagedResponseDto<>(1, List.of(change1, change2), 1, 2);
        
        when(tmdbClient.getChanges(mediaType, startDate, endDate, page))
                .thenReturn(expectedResponse);

        // When: Fetching changes
        CompletableFuture<TmdbPagedResponseDto<TmdbChangesDto>> result = 
                dataFetcher.fetchChangesAsync(mediaType, startDate, endDate, page);

        // Then: Should return a paged response with change items
        assertNotNull(result);
        TmdbPagedResponseDto<TmdbChangesDto> changes = result.join();
        assertNotNull(changes);
        assertEquals(2, changes.results().size());
        assertEquals(100L, changes.results().get(0).id());
        assertEquals(200L, changes.results().get(1).id());
    }

    @Test
    @DisplayName("Should return paged response with popular movies when fetching with generic method")
    void testFetchPopularAsync_Movies_ReturnsPopularMovies() {
        // Given: A media type (MOVIE) and page number
        TmdbMediaType mediaType = TmdbMediaType.MOVIE;
        int page = 1;
        
        TmdbMovieDto movie1 = createMockMovie(100L, "Popular Movie 1");
        TmdbMovieDto movie2 = createMockMovie(200L, "Popular Movie 2");
        TmdbPagedResponseDto<TmdbMovieDto> expectedResponse = 
                new TmdbPagedResponseDto<>(1, List.of(movie1, movie2), 10, 2);
        
        when(tmdbClient.<TmdbMovieDto>getPopular(mediaType, page)).thenReturn(expectedResponse);

        // When: Fetching popular items
        CompletableFuture<TmdbPagedResponseDto<TmdbMovieDto>> result = 
                dataFetcher.fetchPopularAsync(mediaType, page);

        // Then: Should return a paged response with popular movies
        assertNotNull(result);
        TmdbPagedResponseDto<TmdbMovieDto> movies = result.join();
        assertNotNull(movies);
        assertEquals(2, movies.results().size());
        assertEquals("Popular Movie 1", movies.results().get(0).title());
        assertEquals("Popular Movie 2", movies.results().get(1).title());
    }

    @Test
    @DisplayName("Should return paged response with popular TV shows when fetching with generic method")
    void testFetchPopularAsync_TvShows_ReturnsPopularTvShows() {
        // Given: A media type (TV) and page number
        TmdbMediaType mediaType = TmdbMediaType.TV;
        int page = 1;
        
        TmdbTvShowDto tvShow1 = createMockTvShow(100L, "Popular TV 1");
        TmdbTvShowDto tvShow2 = createMockTvShow(200L, "Popular TV 2");
        TmdbPagedResponseDto<TmdbTvShowDto> expectedResponse = 
                new TmdbPagedResponseDto<>(1, List.of(tvShow1, tvShow2), 10, 2);
        
        when(tmdbClient.<TmdbTvShowDto>getPopular(mediaType, page)).thenReturn(expectedResponse);

        // When: Fetching popular items
        CompletableFuture<TmdbPagedResponseDto<TmdbTvShowDto>> result = 
                dataFetcher.fetchPopularAsync(mediaType, page);

        // Then: Should return a paged response with popular TV shows
        assertNotNull(result);
        TmdbPagedResponseDto<TmdbTvShowDto> tvShows = result.join();
        assertNotNull(tvShows);
        assertEquals(2, tvShows.results().size());
        assertEquals("Popular TV 1", tvShows.results().get(0).name());
        assertEquals("Popular TV 2", tvShows.results().get(1).name());
    }

    @Test
    @DisplayName("Should return movie details with credits when fetching by movie ID")
    void testFetchMovieDetailsAsync_ReturnsMovieDetails() {
        // Given: A valid movie ID
        long movieId = 789L;
        TmdbMovieDetailsDto expectedMovie = createMockMovieDetails(movieId);
        when(tmdbClient.getDetailsWithCredits(TmdbMediaType.MOVIE, movieId))
                .thenReturn(expectedMovie);

        // When: Fetching movie details
        CompletableFuture<TmdbMovieDetailsDto> result = 
                dataFetcher.fetchMovieDetailsAsync(movieId);

        // Then: Should return movie details with credits
        assertNotNull(result);
        TmdbMovieDetailsDto movie = result.join();
        assertNotNull(movie);
        assertEquals(movieId, movie.id());
        assertEquals("Test Movie", movie.title());
        assertNotNull(movie.credits());
    }

    @Test
    @DisplayName("Should return TV show details with credits when fetching by TV show ID")
    void testFetchTvShowDetailsAsync_ReturnsTvShowDetails() {
        // Given: A valid TV show ID
        long tvShowId = 321L;
        TmdbTvShowDetailsDto expectedTvShow = createMockTvShowDetails(tvShowId);
        when(tmdbClient.getDetailsWithCredits(TmdbMediaType.TV, tvShowId))
                .thenReturn(expectedTvShow);

        // When: Fetching TV show details
        CompletableFuture<TmdbTvShowDetailsDto> result = 
                dataFetcher.fetchTvShowDetailsAsync(tvShowId);

        // Then: Should return TV show details with credits
        assertNotNull(result);
        TmdbTvShowDetailsDto tvShow = result.join();
        assertNotNull(tvShow);
        assertEquals(tvShowId, tvShow.id());
        assertEquals("Test TV Show", tvShow.name());
        assertNotNull(tvShow.credits());
    }

    @Test
    @DisplayName("Should return list of genres when fetching genres for media type")
    void testFetchGenresAsync_ReturnsGenreList() {
        // Given: A media type
        TmdbMediaType mediaType = TmdbMediaType.MOVIE;
        TmdbGenreDto genre1 = new TmdbGenreDto(28, "Action");
        TmdbGenreDto genre2 = new TmdbGenreDto(18, "Drama");
        TmdbGenreListDto expectedGenres = new TmdbGenreListDto(List.of(genre1, genre2));
        
        when(tmdbClient.getGenres(mediaType)).thenReturn(expectedGenres);

        // When: Fetching genres
        CompletableFuture<TmdbGenreListDto> result = 
                dataFetcher.fetchGenresAsync(mediaType);

        // Then: Should return a list of genres for that media type
        assertNotNull(result);
        TmdbGenreListDto genres = result.join();
        assertNotNull(genres);
        assertEquals(2, genres.genres().size());
        assertEquals("Action", genres.genres().get(0).name());
        assertEquals("Drama", genres.genres().get(1).name());
    }

    @Test
    @DisplayName("Should return combined list of genres when fetching all genres")
    void testFetchGenres_ReturnsCombinedGenreList() {
        // Given: Request for all genres
        TmdbGenreDto actionGenre = new TmdbGenreDto(28, "Action");
        TmdbGenreDto dramaGenre = new TmdbGenreDto(18, "Drama");
        TmdbGenreDto comedyGenre = new TmdbGenreDto(35, "Comedy");
        
        TmdbGenreListDto movieGenres = new TmdbGenreListDto(List.of(actionGenre, dramaGenre));
        TmdbGenreListDto tvGenres = new TmdbGenreListDto(List.of(dramaGenre, comedyGenre));
        
        when(tmdbClient.getGenres(TmdbMediaType.MOVIE)).thenReturn(movieGenres);
        when(tmdbClient.getGenres(TmdbMediaType.TV)).thenReturn(tvGenres);

        // When: Fetching genres for both movies and TV
        List<TmdbGenreDto> result = dataFetcher.fetchGenres();

        // Then: Should return combined list of genres
        assertNotNull(result);
        assertEquals(4, result.size()); // Action, Drama (movie), Drama (TV), Comedy
        assertTrue(result.stream().anyMatch(g -> g.name().equals("Action")));
        assertTrue(result.stream().anyMatch(g -> g.name().equals("Drama")));
        assertTrue(result.stream().anyMatch(g -> g.name().equals("Comedy")));
    }

    @Test
    @DisplayName("Should return details for all movies when fetching movies in batch")
    void testFetchMoviesDetailsBatch_ReturnsAllMovieDetails() {
        // Given: A list of movie IDs
        List<Long> movieIds = List.of(100L, 200L, 300L);
        
        TmdbMovieDetailsDto movie1 = createMockMovieDetails(100L);
        TmdbMovieDetailsDto movie2 = createMockMovieDetails(200L);
        TmdbMovieDetailsDto movie3 = createMockMovieDetails(300L);
        
        when(tmdbClient.getDetailsWithCredits(eq(TmdbMediaType.MOVIE), eq(100L)))
                .thenReturn(movie1);
        when(tmdbClient.getDetailsWithCredits(eq(TmdbMediaType.MOVIE), eq(200L)))
                .thenReturn(movie2);
        when(tmdbClient.getDetailsWithCredits(eq(TmdbMediaType.MOVIE), eq(300L)))
                .thenReturn(movie3);

        // When: Fetching movies in batch
        List<TmdbMovieDetailsDto> result = dataFetcher.fetchMoviesDetailsBatch(movieIds);

        // Then: Should return details for all valid movies
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(m -> m.id() == 100L));
        assertTrue(result.stream().anyMatch(m -> m.id() == 200L));
        assertTrue(result.stream().anyMatch(m -> m.id() == 300L));
    }

    @Test
    @DisplayName("Should return details for all TV shows when fetching TV shows in batch")
    void testFetchTvShowsDetailsBatch_ReturnsAllTvShowDetails() {
        // Given: A list of TV show IDs
        List<Long> tvShowIds = List.of(100L, 200L);
        
        TmdbTvShowDetailsDto tvShow1 = createMockTvShowDetails(100L);
        TmdbTvShowDetailsDto tvShow2 = createMockTvShowDetails(200L);
        
        when(tmdbClient.getDetailsWithCredits(eq(TmdbMediaType.TV), eq(100L)))
                .thenReturn(tvShow1);
        when(tmdbClient.getDetailsWithCredits(eq(TmdbMediaType.TV), eq(200L)))
                .thenReturn(tvShow2);

        // When: Fetching TV shows in batch
        List<TmdbTvShowDetailsDto> result = dataFetcher.fetchTvShowsDetailsBatch(tvShowIds);

        // Then: Should return details for all valid TV shows
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(tv -> tv.id() == 100L));
        assertTrue(result.stream().anyMatch(tv -> tv.id() == 200L));
    }

    @Test
    @DisplayName("Should keep movie detail requests within configured window")
    void testFetchMoviesDetailsBatch_HonorsDetailWindow() throws InterruptedException {
        ExecutorService detailExecutor = Executors.newFixedThreadPool(6);
        TmdbDataFetcher windowedFetcher = new TmdbDataFetcher(tmdbClient, detailExecutor, 35.0, 2);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();

        when(tmdbClient.getDetailsWithCredits(eq(TmdbMediaType.MOVIE), anyLong())).thenAnswer(invocation -> {
            int active = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(active, Math::max);
            try {
                TimeUnit.MILLISECONDS.sleep(60);
                return createMockMovieDetails(invocation.getArgument(1, Long.class));
            } finally {
                inFlight.decrementAndGet();
            }
        });

        try {
            List<TmdbMovieDetailsDto> result = windowedFetcher.fetchMoviesDetailsBatch(List.of(1L, 2L, 3L, 4L, 5L));

            assertEquals(5, result.size());
            assertTrue(maxInFlight.get() <= 2, "maxInFlight=" + maxInFlight.get());
        } finally {
            detailExecutor.shutdownNow();
            assertTrue(detailExecutor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    @Test
    @DisplayName("Should return successful movie details when one detail fetch fails")
    void testFetchMoviesDetailsBatch_PartialFailureTerminates() throws Exception {
        TmdbMovieDetailsDto movie1 = createMockMovieDetails(100L);
        TmdbMovieDetailsDto movie3 = createMockMovieDetails(300L);
        when(tmdbClient.getDetailsWithCredits(TmdbMediaType.MOVIE, 100L)).thenReturn(movie1);
        when(tmdbClient.getDetailsWithCredits(TmdbMediaType.MOVIE, 200L))
                .thenThrow(new IllegalStateException("mock detail failure"));
        when(tmdbClient.getDetailsWithCredits(TmdbMediaType.MOVIE, 300L)).thenReturn(movie3);

        List<TmdbMovieDetailsDto> result = assertTimeoutPreemptively(Duration.ofSeconds(1),
                () -> dataFetcher.fetchMoviesDetailsBatch(List.of(100L, 200L, 300L)));

        assertEquals(List.of(100L, 300L), result.stream().map(TmdbMovieDetailsDto::id).toList());
        TmdbEvidenceWriter.write(TASK_5_EVIDENCE_DIR.resolve("task-5-partial-failure.txt"),
                "Task 5 partial detail failure" + System.lineSeparator()
                        + "terminalState=true" + System.lineSeparator()
                        + "inputDetails=3" + System.lineSeparator()
                        + "failedDetails=1" + System.lineSeparator()
                        + "successfulDetails=" + result.size() + System.lineSeparator()
                        + "resultIds=" + result.stream().map(TmdbMovieDetailsDto::id).toList() + System.lineSeparator()
                        + "networkAccess=false" + System.lineSeparator()
                        + "credentialRequired=false" + System.lineSeparator());
    }

    @Test
    @DisplayName("Should return empty list when fetching movies in batch with empty list")
    void testFetchMoviesDetailsBatch_EmptyList_ReturnsEmptyList() {
        // Given: An empty list of movie IDs
        List<Long> emptyList = Collections.emptyList();

        // When: Fetching movies in batch
        List<TmdbMovieDetailsDto> result = dataFetcher.fetchMoviesDetailsBatch(emptyList);

        // Then: Should return an empty list without errors
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return paged response with popular movies when fetching popular movies")
    void testFetchPopularMoviesAsync_ReturnsPopularMovies() {
        // Given: A valid page number
        int page = 1;
        TmdbMovieDto movie1 = createMockMovie(100L, "Movie 1");
        TmdbMovieDto movie2 = createMockMovie(200L, "Movie 2");
        TmdbPagedResponseDto<TmdbMovieDto> expectedResponse = 
                new TmdbPagedResponseDto<>(1, List.of(movie1, movie2), 10, 2);
        
        when(tmdbClient.<TmdbMovieDto>getPopular(TmdbMediaType.MOVIE, page)).thenReturn(expectedResponse);

        // When: Fetching popular movies
        CompletableFuture<TmdbPagedResponseDto<TmdbMovieDto>> result = 
                dataFetcher.fetchPopularMoviesAsync(page);

        // Then: Should return a paged response with popular movies
        assertNotNull(result);
        TmdbPagedResponseDto<TmdbMovieDto> movies = result.join();
        assertNotNull(movies);
        assertEquals(2, movies.results().size());
        assertEquals(10, movies.totalPages());
    }

    @Test
    @DisplayName("Should return paged response with popular TV shows when fetching popular TV shows")
    void testFetchPopularTvShowsAsync_ReturnsPopularTvShows() {
        // Given: A valid page number
        int page = 2;
        TmdbTvShowDto tvShow1 = createMockTvShow(100L, "TV Show 1");
        TmdbTvShowDto tvShow2 = createMockTvShow(200L, "TV Show 2");
        TmdbPagedResponseDto<TmdbTvShowDto> expectedResponse = 
                new TmdbPagedResponseDto<>(2, List.of(tvShow1, tvShow2), 15, 2);
        
        when(tmdbClient.<TmdbTvShowDto>getPopular(TmdbMediaType.TV, page)).thenReturn(expectedResponse);

        // When: Fetching popular TV shows
        CompletableFuture<TmdbPagedResponseDto<TmdbTvShowDto>> result = 
                dataFetcher.fetchPopularTvShowsAsync(page);

        // Then: Should return a paged response with popular TV shows
        assertNotNull(result);
        TmdbPagedResponseDto<TmdbTvShowDto> tvShows = result.join();
        assertNotNull(tvShows);
        assertEquals(2, tvShows.results().size());
        assertEquals(15, tvShows.totalPages());
        assertEquals(2, tvShows.page());
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

    private TmdbMovieDto createMockMovie(long id, String title) {
        return new TmdbMovieDto(
                id,
                title,
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

    private TmdbTvShowDto createMockTvShow(long id, String name) {
        return new TmdbTvShowDto(
                id,
                name,
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
