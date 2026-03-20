package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class ListFavoriteResponse {
    private final Long id;
    private final UUID userId;
    private final String name;
    private final String description;
    private final Set<ItemResponse> items;
}
