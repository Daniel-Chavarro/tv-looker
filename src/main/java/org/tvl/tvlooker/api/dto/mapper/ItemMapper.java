package org.tvl.tvlooker.api.dto.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.api.dto.response.ItemResponse;
import org.tvl.tvlooker.domain.model.dto.Item;


/**
 * Mapper for converting between Item DTOs and models.
 */
@Component
public class ItemMapper {

    /**
     * Converts an Item domain model to an ItemResponse DTO.
     *
     * @param item the domain model
     * @return the DTO
     */
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
                .genreResponses(item.getGenres() != null
                        ? item.getGenres().stream()
                        .map(GenreMapper::toResponse)
                        .collect(java.util.stream.Collectors.toSet())
                        : null)
                .actorItemResponses(item.getActorsInItem() != null
                        ? ActorItemMapper.toResponse(item.getActorsInItem())
                        : null)
                .directorResponses(item.getDirectors() != null
                        ? item.getDirectors().stream()
                        .map(DirectorMapper::toResponse)
                        .collect(java.util.stream.Collectors.toSet())
                        : null)
                .build();
    }
}

