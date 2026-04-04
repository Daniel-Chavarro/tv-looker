package org.tvl.tvlooker.service.tmdb;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;


import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Handles fetching TMDB data in parallel while respecting rate limits.
 *
 * Uses Google's RateLimiter to ensure we never exceed TMDB's 40 req/s limit.
 * All API calls are made asynchronously in a thread pool.
 *
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
     *
     * RateLimiter.acquire() will block until the request can be made without
     * exceeding the configured rate limit.
     *
     * @param tmdbId TMDB movie ID
     * @return CompletableFuture containing movie details with credits
     */
    public CompletableFuture<TmdbMovieDetailsDto> fetchMovieDetailsAsync(long tmdbId) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquire();
            return tmdbClient.getMovieDetailsWithCredits(tmdbId);
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
            return tmdbClient.getTvShowDetailsWithCredits(tvShowId);
        }, tmdbTaskExecutor);
    }

    /**
     * Batch fetches multiple movies in parallel with rate limiting.
     *
     * Each movie fetch respects the global rate limiter, so the total throughput
     * never exceeds the configured limit. With 20 parallel threads and a 35 req/s
     * limit, this will take approximately:
     * - 1000 items / 35 req/s = ~28-30 seconds
     *
     * @param tmdbIds List of TMDB movie IDs to fetch
     * @return List of movie details with credits (in any order)
     */
    public List<TmdbMovieDetailsDto> fetchMoviesBatch(List<Long> tmdbIds) {
        log.debug("Fetching {} movies in parallel batch", tmdbIds.size());

        List<CompletableFuture<TmdbMovieDetailsDto>> futures = tmdbIds.stream()
                .map(this::fetchMovieDetailsAsync)
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
     * Batch fetches multiple TV shows in parallel with rate limiting.
     *
     * @param tvShowIds List of TMDB TV show IDs to fetch
     * @return List of TV show details with credits
     */
    public List<TmdbTvShowDetailsDto> fetchTvShowsBatch(List<Long> tvShowIds) {
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
}