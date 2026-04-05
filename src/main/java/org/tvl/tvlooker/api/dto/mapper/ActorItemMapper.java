package org.tvl.tvlooker.api.dto.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.api.dto.response.ActorItemResponse;
import org.tvl.tvlooker.domain.model.dto.ActorItem;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for converting between ActorItem DTOs and models.
 */
@Component
public class ActorItemMapper {

    /**
     * Converts an ActorItem domain model to an ActorItemResponse DTO.
     * @param actorItem the domain model
     * @return the DTO
     */
    public static ActorItemResponse toResponse(ActorItem actorItem) {
        return ActorItemResponse.builder()
                .id(actorItem.getId())
                .actorId(actorItem.getActor().getId())
                .actorName(actorItem.getActor().getName())
                .characterName(actorItem.getCharacterName())
                .billingOrder(actorItem.getBillingOrder())
                .build();
    }

    /**
     * Converts a list of ActorItem domain models to a list of ActorItemResponse DTOs.
     * @param actorItems the list of domain models
     * @return the set of DTOs
     */
    public static Set<ActorItemResponse> toResponse(Set<ActorItem> actorItems) {
        return actorItems.stream()
                .map(ActorItemMapper::toResponse)
                .collect(Collectors.toSet());
    }
}
