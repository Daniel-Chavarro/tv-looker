package org.tvl.tvlooker.service.tmdb;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaDetails;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaItem;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Handles fetching TMDB data in parallel while respecting rate limits.
 * <p>
 * Uses Google's RateLimiter to ensure we never exceed TMDB's 40 req/s limit.
 * All API calls are made asynchronously in a thread pool.
 * <p>
 * Key Innovation: Leverages append_to_response parameter to combine multiple
 * API calls (details + credits) into a single request.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-04-03
 */
@Service
@Slf4j
public class TmdbDataFetcher {

    private final TmdbClient tmdbClient;
    private final Executor tmdbTaskExecutor;
    private final RateLimiter rateLimiter;

    public TmdbDataFetcher(
            TmdbClient tmdbClient,
            @Qualifier("taskExecutor") Executor tmdbTaskExecutor,
            @Value("${tmdb.api.rate-limit:35}") double requestsPerSecond) {

        if (requestsPerSecond <= 0 || requestsPerSecond > 40) {
            throw new IllegalArgumentException("requestsPerSecond must be between 0 and 40.");
        }

        this.tmdbClient = tmdbClient;
        this.tmdbTaskExecutor = tmdbTaskExecutor;
        // Set to 35 req/s for safety margin (TMDB hard limit is 40)
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
        log.info("TmdbDataFetcher initialized with rate limit: {} req/s", requestsPerSecond);
    }

    /**
     * Fetches movie details with credits asynchronously, respecting rate limits.
     * <p>
     * RateLimiter.acquire() will block until the request can be made without
     * exceeding the configured rate limit.
     *
     * @param tmdbId TMDB movie ID
     * @return CompletableFuture containing movie details with credits
     */
    public CompletableFuture<TmdbMovieDetailsDto> fetchMovieDetailsAsync(long tmdbId) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire();
            return tmdbClient.getDetailsWithCredits(TmdbMediaType.MOVIE, tmdbId);
        }, tmdbTaskExecutor);
    }

    /**
     * Fetches TV show details with credits asynchronously, respecting rate limits.
     *
     * @param tvShowId TMDB TV show ID
     * @return CompletableFuture containing TV show details with credits
     */
    public CompletableFuture<TmdbTvShowDetailsDto> fetchTvShowDetailsAsync(long tvShowId) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire();
            return tmdbClient.getDetailsWithCredits(TmdbMediaType.TV, tvShowId);
        }, tmdbTaskExecutor);
    }

    /**
     * Fetches genre list for a given media type asynchronously, respecting rate limits.
     *
     * @param mediaType the TMDB media type (MOVIE or TV)
     * @return CompletableFuture containing the list of genres for the specified media type
     */
    public CompletableFuture<TmdbGenreListDto> fetchGenresAsync(TmdbMediaType mediaType) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire();
            return tmdbClient.getGenres(mediaType);
        }, tmdbTaskExecutor);
    }

    /**
     * Fetches a page of popular movies or TV shows asynchronously, respecting rate limits.
     *
     * @param page the page number to fetch (1-based index)
     * @return CompletableFuture containing a paged response of movies or TV shows for the specified media type and page
     */
    public CompletableFuture<TmdbPagedResponseDto<TmdbMovieDto>> fetchPopularMoviesAsync(int page) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire();
            return tmdbClient.getPopular(TmdbMediaType.MOVIE, page);
        }, tmdbTaskExecutor);
    }

    /**
     * Fetches a page of popular TV shows asynchronously, respecting rate limits.
     *
     * @param page the page number to fetch (1-based index)
     * @return CompletableFuture containing a paged response of TV shows for the specified page
     */
    public CompletableFuture<TmdbPagedResponseDto<TmdbTvShowDto>> fetchPopularTvShowsAsync(int page) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire();
            return tmdbClient.getPopular(TmdbMediaType.TV, page);
        }, tmdbTaskExecutor);
    }

    public CompletableFuture<TmdbPagedResponseDto<TmdbChangesDto>> fetchChangesAsync(
            TmdbMediaType type, LocalDate startDate, LocalDate endDate, int page) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire();
            return tmdbClient.getChanges(type, startDate, endDate, page);
        }, tmdbTaskExecutor);
    }

    public <T extends TmdbMediaDetails> CompletableFuture<T> fetchDetailsWithCreditsAsync(
            TmdbMediaType type, long id) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire();
            return tmdbClient.getDetailsWithCredits(type, id);
        }, tmdbTaskExecutor);
    }

    public <T extends TmdbMediaItem> CompletableFuture<TmdbPagedResponseDto<T>> fetchPopularAsync(
            TmdbMediaType type, int page) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire();
            return tmdbClient.getPopular(type, page);
        }, tmdbTaskExecutor);
    }

    /**
     * Batch fetches multiple movies in parallel with rate limiting.
     * <p>
     * Each movie fetch respects the global rate limiter, so the total throughput
     * never exceeds the configured limit. With 20 parallel threads and a 35 req/s
     * limit, this will take approximately:
     * - 1000 items / 35 req/s = ~28-30 seconds
     *
     * @param tmdbIds List of TMDB movie IDs to fetch
     * @return List of movie details with credits (in any order)
     */
    public List<TmdbMovieDetailsDto> fetchMoviesDetailsBatch(List<Long> tmdbIds) {
        log.debug("Fetching {} movies in parallel batch", tmdbIds.size());

        List<CompletableFuture<TmdbMovieDetailsDto>> futures = tmdbIds.stream()
                .map(this::fetchMovieDetailsAsync)
                .toList();

        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        return allOf.thenApply(v -> futures.stream()
                        .map(f -> {
                            try {
                                return f.join();
                            } catch (CompletionException e) {
                                log.error("Future individual falló: {}", e.getCause().getMessage(), e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList())
                .exceptionally(ex -> {
                    log.error("Error en fetchTvShowsDetailsBatch: {}", ex.getMessage(), ex);
                    return List.of();
                })
                .join();
    }

    /**
     * Batch fetches multiple TV shows in parallel with rate limiting.
     *
     * @param tvShowIds List of TMDB TV show IDs to fetch
     * @return List of TV show details with credits
     */
    public List<TmdbTvShowDetailsDto> fetchTvShowsDetailsBatch(List<Long> tvShowIds) {
        log.debug("Fetching {} TV shows in parallel batch", tvShowIds.size());

        List<CompletableFuture<TmdbTvShowDetailsDto>> futures = tvShowIds.stream()
                .map(this::fetchTvShowDetailsAsync)
                .toList();

        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        return allOf.thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList())
                .join();
    }

    /**
     * Batch fetches genres for both movies and TV shows in parallel with rate limiting.
     *
     * @return List of all genres for movies and TV shows combined.
     */
    public List<TmdbGenreDto> fetchGenres() {
        log.debug("Fetching genres for both movies and TV shows in parallel batch");

        CompletableFuture<TmdbGenreListDto> movieGenresFuture = fetchGenresAsync(TmdbMediaType.MOVIE);
        CompletableFuture<TmdbGenreListDto> tvGenresFuture = fetchGenresAsync(TmdbMediaType.TV);

        CompletableFuture<Void> allOf = CompletableFuture.allOf(movieGenresFuture, tvGenresFuture);

        return allOf.thenApply(v -> {
            List<TmdbGenreDto> genres = new ArrayList<>();
            genres.addAll(movieGenresFuture.join().genres());
            genres.addAll(tvGenresFuture.join().genres());
            return genres;
        }).join();
    }
}