package org.tvl.tvlooker.persistence.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaDetails;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaItem;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;

import java.time.LocalDate;
import java.util.List;

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
@Slf4j
public class TmdbClient {

    private final RestClient restClient;
    private final String language;

    public TmdbClient(
            RestClient tmdbRestClient,
            @Value("${tmdb.api.language:es-MX}") String language) {
        this.restClient = tmdbRestClient;
        this.language = language;
    }

    /**
     * GET /movie/{id} — Full details for a movie (includes genre objects).
     *
     * @param movieId the TMDB movie ID
     * @return movie details DTO
     */
    @Deprecated
    public TmdbMovieDto getMovieDetails(long movieId) {
        log.debug("Fetching movie details for ID {}", movieId);
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
    @Deprecated
    public TmdbCreditsDto getMovieCredits(long movieId) {
        log.debug("Fetching movie credits for ID {}", movieId);
        return restClient.get()
                .uri("/movie/{id}/credits?language={lang}", movieId, language)
                .retrieve()
                .body(TmdbCreditsDto.class);
    }

    /**
     * GET /tv/{id} — Full details for a TV show (includes genre objects).
     *
     * @param tvShowId the TMDB TV show ID
     * @return TV show details DTO
     */
    @Deprecated
    public TmdbTvShowDto getTvShowDetails(long tvShowId) {
        log.debug("Fetching TV show details for ID {}", tvShowId);
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
    @Deprecated
    public TmdbCreditsDto getTvShowCredits(long tvShowId) {
        log.debug("Fetching TV show credits for ID {}", tvShowId);
        return restClient.get()
                .uri("/tv/{id}/credits?language={lang}", tvShowId, language)
                .retrieve()
                .body(TmdbCreditsDto.class);
    }

    // ===================== GENERIC METHODS =====================

    public <T extends TmdbMediaItem> TmdbPagedResponseDto<T> getPopular(
            TmdbMediaType type, int page) {
        log.debug("Fetching popular {} page {}", type, page);
        return restClient.get()
                .uri("/{type}/popular?language={lang}&page={page}",
                        type.getPath(), language, page)
                .retrieve()
                .body(ParameterizedTypeReference.forType(
                                ResolvableType.forClassWithGenerics(
                                        TmdbPagedResponseDto.class, type.getMediaItemClass()).getType()
                        )
                );
    }

    public <T extends TmdbMediaDetails> T getDetailsWithCredits(
            TmdbMediaType type, long id) {
        log.debug("Fetching {} details + credits for ID {}", type, id);
        T details = restClient.get()
                .uri("/{type}/{id}?language={lang}&append_to_response=credits",
                        type.getPath(), id, language)
                .retrieve()
                .body(ParameterizedTypeReference.forType(
                        ResolvableType.forClass(type.getMediaDetailsClass()).getType()
                ));
        return filterUncreditedCast(details);
    }

    private <T extends TmdbMediaDetails> T filterUncreditedCast(T details) {
        if (details == null || details.credits() == null || details.credits().getCast() == null) {
            return details;
        }
        List<TmdbCreditsDto.CastMember> filteredCast = details.credits().getCast().stream()
                .filter(cast -> cast.character() == null || !cast.character().contains("(uncredited)"))
                .toList();


        details.credits().setCast(filteredCast);
        return details;
    }

    public TmdbPagedResponseDto<TmdbChangesDto> getChanges(
            TmdbMediaType type, LocalDate startDate, LocalDate endDate, int page) {
        log.debug("Fetching {} changes from {} to {}, page {}",
                type, startDate, endDate, page);
        return restClient.get()
                .uri("/{type}/changes?start_date={start}&end_date={end}&page={page}",
                        type.getPath(), startDate, endDate, page)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public TmdbGenreListDto getGenres(TmdbMediaType type) {
        log.debug("Fetching {} genres", type);
        return restClient.get()
                .uri("/genre/{type}/list?language={lang}", type.getPath(), language)
                .retrieve()
                .body(TmdbGenreListDto.class);
    }
}
