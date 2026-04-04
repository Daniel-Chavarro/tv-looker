package org.tvl.tvlooker.persistence.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Complete TV show data with appended credits.
 * Returned by: GET /tv/{id}?append_to_response=credits
 */
public record TmdbTvShowDetailsDto(
        long id,
        String name,
        String overview,
        @JsonProperty("first_air_date") String firstAirDate,
        double popularity,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("backdrop_path") String backdropPath,
        List<TmdbGenreDto> genres,
        TmdbCreditsDto credits
) {}