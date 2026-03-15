package org.tvl.tvlooker.persistence.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single changed item from GET /movie/changes or /tv/changes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbChangesDto(
        long id,
        Boolean adult
) {}

