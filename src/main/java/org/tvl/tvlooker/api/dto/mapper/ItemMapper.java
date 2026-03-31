package org.tvl.tvlooker.api.dto.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.api.dto.response.ItemResponse;
import org.tvl.tvlooker.domain.model.Item;

/**
 * Mapper for converting between Item DTOs and models.
 */
@Component
public class ItemMapper {
    public static ItemResponse toResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .overview(item.getOverview())
                .releaseDate(item.getReleaseDate())
                .popularity(item.getPopularity())
                .voteAverage(item.getVoteAverage())
                .tmdbType(item.getTmdbType())
                .tmdbId(item.getTmdbId())
                .build();
    }

    public static Item toModel(ItemResponse itemResponse) {
        return Item.builder()
                .id(itemResponse.getId())
                .title(itemResponse.getTitle())
                .overview(itemResponse.getOverview())
                .releaseDate(itemResponse.getReleaseDate())
                .popularity(itemResponse.getPopularity())
                .voteAverage(itemResponse.getVoteAverage())
                .tmdbType(itemResponse.getTmdbType())
                .tmdbId(itemResponse.getTmdbId())
                .build();
    }
}
