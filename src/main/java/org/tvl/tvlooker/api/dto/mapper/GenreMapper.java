package org.tvl.tvlooker.api.dto.mapper;

import org.tvl.tvlooker.api.dto.response.GenreResponse;
import org.tvl.tvlooker.domain.model.dto.Genre;
/**
 * Mapper for converting between Genre DTOs and models.
 */
public class GenreMapper {
    public static Genre toModel(GenreResponse response){
        return Genre.builder()
                .id(response.getId())
                .tmdbId(response.getTmdbId())
                .name(response.getName())
                .build();
    }

    public static GenreResponse toResponse(Genre genre){
        return GenreResponse.builder()
                .id(genre.getId())
                .tmdbId(genre.getTmdbId())
                .name(genre.getName())
                .build();
    }
}
