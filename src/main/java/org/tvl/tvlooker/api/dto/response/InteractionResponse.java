package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.tvl.tvlooker.domain.model.enums.InteractionType;

import java.security.Timestamp;
import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class InteractionResponse {
    private Long id;
    private UUID userId;
    private Long itemId;
    private InteractionType interactionType;
    private Timestamp timestamp;
}