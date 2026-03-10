package org.tvl.tvlooker.infrastructure.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response from GET /genre/movie/list or /genre/tv/list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbGenreListDto(
        List<TmdbGenreDto> genres
) {}

