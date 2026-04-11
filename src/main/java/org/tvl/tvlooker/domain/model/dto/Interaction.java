package org.tvl.tvlooker.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.tvl.tvlooker.domain.model.enums.InteractionType;

import java.sql.Timestamp;
import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class Interaction {
    private final Long id;
    private final UUID userId;
    private final Long itemId;
    @Builder.Default
    private final Long reviewId = null;
    private final InteractionType interactionType;
    private final Timestamp createdAt;
}
