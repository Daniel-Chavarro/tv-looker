package org.tvl.tvlooker.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.tvl.tvlooker.domain.model.enums.InteractionType;


/**
 * DTO for updating an interaction request.
 */
@Getter
@Setter
public class UpdateInteractionRequest {
    @NotNull(message = "Interaction type is required")
    private InteractionType interactionType;

    private Long reviewId;
}
