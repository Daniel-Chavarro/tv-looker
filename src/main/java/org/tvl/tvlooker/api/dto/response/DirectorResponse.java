package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DirectorResponse {
    private Long id;
    private String name;
    private Long tmdbId;
}
