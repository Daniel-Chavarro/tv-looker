package org.tvl.tvlooker.persistence.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Complete movie data with appended credits.
 * Returned by: GET /movie/{id}?append_to_response=credits
 * <p>
 * This DTO combines what was previously fetched in two separate calls:
 * 1. GET /movie/{id} (details + genres)
 * 2. GET /movie/{id}/credits (actors + directors)
 * <p>
 * The "credits" field is populated due to the append_to_response parameter.
 */
public record TmdbMovieDetailsDto(
        long id,
        String title,
        String overview,
        @JsonProperty("release_date") String releaseDate,
        double popularity,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("backdrop_path") String backdropPath,
        List<TmdbGenreDto> genres,
        TmdbCreditsDto credits
) {}