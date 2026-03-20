package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * DTO for recommendation response.
 */
@Builder
@Getter
@AllArgsConstructor
public class RecommendationResponse {
    private UUID userId;
    private int count;
    private List<ItemResponse> items;
}