package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * DTO for item response.
 */
@Builder
@Getter
@AllArgsConstructor
public class ItemResponse {
    private Long id;
    private String title;
    private String overview;
    private LocalDate releaseDate;
    private BigDecimal popularity;
    private BigDecimal voteAverage;
    private TmdbType tmdbType;
    private Long tmdbId;
    private Set<GenreResponse> genreResponses;
    private Set<ActorItemResponse> actorItemResponses;
}