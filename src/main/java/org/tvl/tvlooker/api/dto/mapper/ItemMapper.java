package org.tvl.tvlooker.api.dto.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.api.dto.response.ItemResponse;
import org.tvl.tvlooker.domain.model.entity.Item;

@Component
public class ItemMapper {
    public ItemResponse toResponse(Item item) {
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

    public Item toModel(ItemResponse itemResponse) {
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
