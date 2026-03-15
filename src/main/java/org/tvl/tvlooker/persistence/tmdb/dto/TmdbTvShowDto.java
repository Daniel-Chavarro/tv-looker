package org.tvl.tvlooker.persistence.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a TV show from the TMDB API.
 * Used for both /tv/popular list results and /tv/{id} detail responses.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbTvShowDto(
        long id,
        String name,
        String overview,
        @JsonProperty("first_air_date") String firstAirDate,
        double popularity,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("genre_ids") List<Integer> genreIds,
        List<TmdbGenreDto> genres
) {}

