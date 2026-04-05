package org.tvl.tvlooker.api.dto.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.api.dto.request.CreateListFavoriteRequest;
import org.tvl.tvlooker.api.dto.request.UpdateListFavoriteRequest;
import org.tvl.tvlooker.api.dto.response.ListFavoriteResponse;
import org.tvl.tvlooker.domain.model.dto.ListFavorite;

import java.util.stream.Collectors;

/**
 * Mapper for converting between ListFavorite DTOs and models.
 */
@Component
public class ListFavoriteMapper {
    public static ListFavoriteResponse toResponse(ListFavorite listFavorite) {
        return ListFavoriteResponse.builder()
                .id(listFavorite.getId())
                .name(listFavorite.getName())
                .description(listFavorite.getDescription())
                .userId(listFavorite.getUserId())
                .items(listFavorite.getItems().stream()
                        .map(ItemMapper::toResponse)
                        .collect(Collectors.toSet()))
                .build();
    }


    public static ListFavorite fromCreateRequest(CreateListFavoriteRequest request){
        return ListFavorite.builder()
                .name(request.getName())
                .description(request.getDescription())
                .userId(request.getUserId())
                .build();
    }

    public static ListFavorite fromUpdateRequest(UpdateListFavoriteRequest request){
        return ListFavorite.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }
}
