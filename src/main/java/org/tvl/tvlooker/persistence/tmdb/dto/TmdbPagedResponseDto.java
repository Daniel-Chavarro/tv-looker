package org.tvl.tvlooker.persistence.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Generic paginated response from TMDB API.
 * Used for /movie/popular, /tv/popular, /movie/changes, etc.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbPagedResponseDto<T>(
        int page,
        List<T> results,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("total_results") int totalResults
) {}

