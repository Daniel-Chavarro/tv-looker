package org.tvl.tvlooker.persistence.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;


import java.time.LocalDate;

/**
 * Client for the TMDB API v3.
 * Encapsulates all HTTP communication with the TMDB API.
 * No other class in the application should call the TMDB API directly.
 *
 * <p>Rate Limit: TMDB allows ~40 requests/second.
 * This client does NOT handle rate limiting — that responsibility belongs to the caller
 * (TmdbDataCollector and TmdbDataSynchronizer) which add delays between calls.</p>
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-10
 */
@Component
public class TmdbClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbClient.class);

    private final RestClient restClient;
    private final String language;

    public TmdbClient(
            RestClient tmdbRestClient,
            @Value("${tmdb.api.language:es-MX}") String language) {
        this.restClient = tmdbRestClient;
        this.language = language;
    }

    // ===================== MOVIES =====================

    /**
     * GET /movie/popular — Paginated list of popular movies.
     *
     * @param page Page number (1-based, max 500)
     * @return paginated response with movie DTOs
     */
    public TmdbPagedResponseDto<TmdbMovieDto> getPopularMovies(int page) {
        LOGGER.debug("Fetching popular movies page {}", page);
        return restClient.get()
                .uri("/movie/popular?language={lang}&page={page}", language, page)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /**
     * GET /movie/{id} — Full details for a movie (includes genre objects).
     *
     * @param movieId the TMDB movie ID
     * @return movie details DTO
     */
    public TmdbMovieDto getMovieDetails(long movieId) {
        LOGGER.debug("Fetching movie details for ID {}", movieId);
        return restClient.get()
                .uri("/movie/{id}?language={lang}", movieId, language)
                .retrieve()
                .body(TmdbMovieDto.class);
    }

    /**
     * GET /movie/{id}/credits — Cast and crew for a movie.
     *
     * @param movieId the TMDB movie ID
     * @return credits DTO with cast and crew lists
     */
    public TmdbCreditsDto getMovieCredits(long movieId) {
        LOGGER.debug("Fetching movie credits for ID {}", movieId);
        return restClient.get()
                .uri("/movie/{id}/credits?language={lang}", movieId, language)
                .retrieve()
                .body(TmdbCreditsDto.class);
    }

    /**
     * GET /movie/changes — IDs of movies that changed in a date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @param page      Page number
     * @return paginated response with changed item IDs
     */
    public TmdbPagedResponseDto<TmdbChangesDto> getMovieChanges(
            LocalDate startDate, LocalDate endDate, int page) {
        LOGGER.debug("Fetching movie changes from {} to {}, page {}", startDate, endDate, page);
        return restClient.get()
                .uri("/movie/changes?start_date={start}&end_date={end}&page={page}",
                        startDate, endDate, page)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    // ===================== TV SHOWS =====================

    /**
     * GET /tv/popular — Paginated list of popular TV shows.
     *
     * @param page Page number (1-based, max 500)
     * @return paginated response with TV show DTOs
     */
    public TmdbPagedResponseDto<TmdbTvShowDto> getPopularTvShows(int page) {
        LOGGER.debug("Fetching popular TV shows page {}", page);
        return restClient.get()
                .uri("/tv/popular?language={lang}&page={page}", language, page)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /**
     * GET /tv/{id} — Full details for a TV show (includes genre objects).
     *
     * @param tvShowId the TMDB TV show ID
     * @return TV show details DTO
     */
    public TmdbTvShowDto getTvShowDetails(long tvShowId) {
        LOGGER.debug("Fetching TV show details for ID {}", tvShowId);
        return restClient.get()
                .uri("/tv/{id}?language={lang}", tvShowId, language)
                .retrieve()
                .body(TmdbTvShowDto.class);
    }

    /**
     * GET /tv/{id}/credits — Cast and crew for a TV show.
     *
     * @param tvShowId the TMDB TV show ID
     * @return credits DTO with cast and crew lists
     */
    public TmdbCreditsDto getTvShowCredits(long tvShowId) {
        LOGGER.debug("Fetching TV show credits for ID {}", tvShowId);
        return restClient.get()
                .uri("/tv/{id}/credits?language={lang}", tvShowId, language)
                .retrieve()
                .body(TmdbCreditsDto.class);
    }

    /**
     * GET /tv/changes — IDs of TV shows that changed in a date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @param page      Page number
     * @return paginated response with changed item IDs
     */
    public TmdbPagedResponseDto<TmdbChangesDto> getTvShowChanges(
            LocalDate startDate, LocalDate endDate, int page) {
        LOGGER.debug("Fetching TV show changes from {} to {}, page {}", startDate, endDate, page);
        return restClient.get()
                .uri("/tv/changes?start_date={start}&end_date={end}&page={page}",
                        startDate, endDate, page)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    // ===================== GENRES =====================

    /**
     * GET /genre/movie/list — All movie genres.
     *
     * @return list of movie genres
     */
    public TmdbGenreListDto getMovieGenres() {
        LOGGER.debug("Fetching movie genre list");
        return restClient.get()
                .uri("/genre/movie/list?language={lang}", language)
                .retrieve()
                .body(TmdbGenreListDto.class);
    }

    /**
     * GET /genre/tv/list — All TV genres.
     *
     * @return list of TV genres
     */
    public TmdbGenreListDto getTvGenres() {
        LOGGER.debug("Fetching TV genre list");
        return restClient.get()
                .uri("/genre/tv/list?language={lang}", language)
                .retrieve()
                .body(TmdbGenreListDto.class);
    }
}

