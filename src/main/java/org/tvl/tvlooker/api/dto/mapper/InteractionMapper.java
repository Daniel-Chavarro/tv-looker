package org.tvl.tvlooker.api.dto.mapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.tvl.tvlooker.api.dto.request.CreateInteractionRequest;
import org.tvl.tvlooker.api.dto.response.InteractionResponse;
import org.tvl.tvlooker.domain.model.entity.Interaction;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.Review;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.service.ItemService;
import org.tvl.tvlooker.service.ReviewService;
import org.tvl.tvlooker.service.UserService;

@RequiredArgsConstructor
@Component
public class InteractionMapper {
    private final UserService USER_SERVICE;
    private final ItemService ITEM_SERVICE;
    private final ReviewService REVIEW_SERVICE;

    public Interaction toModel(InteractionResponse interactionResponse) {
        User user = USER_SERVICE.getById(interactionResponse.getUserId());
        Item item = ITEM_SERVICE.getById(interactionResponse.getItemId());
        Review review = REVIEW_SERVICE.findByUserIdAndItemId(user.getId(), item.getId()).orElse(null);

        return Interaction.builder()
                .id(interactionResponse.getId())
                .user(user)
                .item(item)
                .review(review)
                .interactionType(interactionResponse.getInteractionType())
                .createdAt(interactionResponse.getCreatedAt())
                .build();
    }

    public InteractionResponse toResponse(Interaction interaction) {
        return InteractionResponse.builder()
                .id(interaction.getId())
                .userId(interaction.getUser().getId())
                .itemId(interaction.getItem().getId())
                .interactionType(interaction.getInteractionType())
                .createdAt(interaction.getCreatedAt())
                .build();
    }

    public Interaction fromCreateRequest(CreateInteractionRequest request) {
        User user = USER_SERVICE.getById(request.getUserId());
        Item item = ITEM_SERVICE.getById(request.getItemId());
        Review review = REVIEW_SERVICE.findByUserIdAndItemId(user.getId(), item.getId()).orElse(null);
        return Interaction.builder()
                .user(user)
                .item(item)
                .review(review)
                .interactionType(request.getInteractionType())
                .build();
    }
}
