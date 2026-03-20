package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO for genre response.
 */
@Builder
@Getter
@AllArgsConstructor
public class GenreResponse {
    private Long id;
    private Long tmdbId;
    private String name;
}
