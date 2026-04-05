package org.tvl.tvlooker.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class Director {
    private final Long id;
    private final Long tmdbId;
    private final String name;
}
