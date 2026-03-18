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
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.service.RecommendationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;

    private final ItemMapper ITEM_MAPPER;

    @GetMapping
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        List<Item> recommendations =
                recommendationService.getUserRecommendations(userId, limit);

        List<ItemResponse> items = recommendations.stream()
                .map(ITEM_MAPPER::toResponse)
                .toList();

        RecommendationResponse response = RecommendationResponse.builder()
                .userId(userId)
                .count(items.size())
                .items(items)
                .build();

        return ResponseEntity.ok(response);
    }
}