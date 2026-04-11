package org.tvl.tvlooker.api.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.mapper.ItemMapper;
import org.tvl.tvlooker.api.dto.response.ItemResponse;
import org.tvl.tvlooker.api.dto.response.RecommendationResponse;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.service.RecommendationService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing recommendations.
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;

    /**
     * Retrieves recommendations for a specific user.
     * @param userId the user ID
     * @param limit the maximum number of recommendations to retrieve
     * @return the recommendation response
     */
    @GetMapping
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        List<Item> recommendations =
                recommendationService.getUserRecommendations(userId, limit);

        List<ItemResponse> items = recommendations.stream()
                .map(ItemMapper::toResponse)
                .toList();

        RecommendationResponse response = RecommendationResponse.builder()
                .userId(userId)
                .count(items.size())
                .items(items)
                .build();

        return ResponseEntity.ok(response);
    }
}