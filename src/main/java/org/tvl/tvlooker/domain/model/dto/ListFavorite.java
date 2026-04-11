package org.tvl.tvlooker.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class ListFavorite {
    private final Long id;
    private final UUID userId;
    private final String name;
    private final String description;
    private final Set<Item> items;
}
