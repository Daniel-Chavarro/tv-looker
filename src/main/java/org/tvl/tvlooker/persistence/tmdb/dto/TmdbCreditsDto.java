package org.tvl.tvlooker.persistence.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Credits (cast and crew) for a movie or TV show from TMDB API.
 * GET /movie/{id}/credits or /tv/{id}/credits
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbCreditsDto(
        long id,
        List<CastMember> cast,
        List<CrewMember> crew
) {
    /**
     * Represents an actor in the cast.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CastMember(
            long id,
            String name,
            String character,
            @JsonProperty("known_for_department") String knownForDepartment,
            int order
    ) {}

    /**
     * Represents a crew member (filter by job="Director" for directors).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CrewMember(
            long id,
            String name,
            String department,
            String job
    ) {}
}

