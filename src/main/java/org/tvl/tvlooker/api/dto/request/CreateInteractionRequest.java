package org.tvl.tvlooker.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.tvl.tvlooker.domain.model.enums.InteractionType;

import java.util.UUID;

/** * DTO for creating a new interaction.
 */
@Getter
@Setter
public class CreateInteractionRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotNull(message = "Interaction type is required")
    private InteractionType interactionType;
}