package org.tvl.tvlooker.api.dto.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.tvl.tvlooker.api.dto.request.CreateInteractionRequest;
import org.tvl.tvlooker.api.dto.request.UpdateInteractionRequest;
import org.tvl.tvlooker.api.dto.response.InteractionResponse;
import org.tvl.tvlooker.domain.model.dto.Interaction;


/**
 * Mapper for converting between Interaction DTOs and models.
 */
@RequiredArgsConstructor
@Component
public class InteractionMapper {

    public static Interaction toModel(InteractionResponse interactionResponse) {
        return Interaction.builder()
                .id(interactionResponse.getId())
                .userId(interactionResponse.getUserId())
                .itemId(interactionResponse.getItemId())
                .reviewId(interactionResponse.getReviewId())
                .interactionType(interactionResponse.getInteractionType())
                .createdAt(interactionResponse.getCreatedAt())
                .build();
    }

    public static InteractionResponse toResponse(Interaction interaction) {
        return InteractionResponse.builder()
                .id(interaction.getId())
                .userId(interaction.getUserId())
                .itemId(interaction.getItemId())
                .reviewId(interaction.getReviewId())
                .interactionType(interaction.getInteractionType())
                .createdAt(interaction.getCreatedAt())
                .build();
    }

    public static Interaction fromCreateRequest(CreateInteractionRequest request) {
        return Interaction.builder()
                .userId(request.getUserId())
                .itemId(request.getItemId())
                .reviewId(request.getReviewId())
                .interactionType(request.getInteractionType())
                .build();
    }

    public static Interaction fromUpdateRequest(UpdateInteractionRequest request) {
        return Interaction.builder()
                .interactionType(request.getInteractionType())
                .reviewId(request.getReviewId())
                .build();
    }
}
