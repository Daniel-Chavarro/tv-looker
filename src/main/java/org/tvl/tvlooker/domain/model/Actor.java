package org.tvl.tvlooker.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class Actor {
    private final Long id;
    private final Long tmdbId;
    private final String name;
}
