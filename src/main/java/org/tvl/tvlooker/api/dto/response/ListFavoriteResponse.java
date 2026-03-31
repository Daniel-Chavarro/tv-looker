package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

/**
 * DTO for favorite list response.
 */
@Builder
@Getter
@AllArgsConstructor
public class ListFavoriteResponse {
    private Long id;
    private UUID userId;
    private String name;
    private String description;
    private Set<ItemResponse> items;
}
