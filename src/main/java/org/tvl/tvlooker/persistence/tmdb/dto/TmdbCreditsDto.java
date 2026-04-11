package org.tvl.tvlooker.persistence.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Credits (cast and crew) for a movie or TV show from TMDB API.
 * Now used in TmdbMediaDetails responses (GET /movie/{id} and GET /tv/{id} with append_to_response=credits).
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
public class TmdbCreditsDto{
        private Long id;
        private List<CastMember> cast;
        private List<CrewMember> crew;

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

