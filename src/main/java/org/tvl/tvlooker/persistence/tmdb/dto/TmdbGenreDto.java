package org.tvl.tvlooker.persistence.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single genre from TMDB API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbGenreDto(
        int id,
        String name
) {}

