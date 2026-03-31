package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO for actor response.
 */
@Getter
@Builder
@AllArgsConstructor
public class ActorResponse {
    private Long id;
    private String name;
    private Long tmdbId;
}
