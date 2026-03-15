package org.tvl.tvlooker.persistence.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Represents a movie from the TMDB API.
 * Used for both /movie/popular list results and /movie/{id} detail responses.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovieDto(
        long id,
        String title,
        String overview,
        @JsonProperty("release_date") String releaseDate,
        double popularity,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("vote_count") int voteCount,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("genre_ids") List<Integer> genreIds,
        List<TmdbGenreDto> genres
) {}

