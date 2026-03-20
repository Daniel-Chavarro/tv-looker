package org.tvl.tvlooker.api.dto.mapper;

import org.tvl.tvlooker.api.dto.response.DirectorResponse;
import org.tvl.tvlooker.domain.model.Director;

public class DirectorMapper {
    public static Director toModel(DirectorResponse response){
        return Director.builder()
                .id(response.getId())
                .tmdbId(response.getTmdbId())
                .name(response.getName())
                .build();
    }

    public static DirectorResponse toResponse(Director director){
        return DirectorResponse.builder()
                .id(director.getId())
                .tmdbId(director.getTmdbId())
                .name(director.getName())
                .build();
    }
}
