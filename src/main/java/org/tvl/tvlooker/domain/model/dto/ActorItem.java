package org.tvl.tvlooker.domain.model.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ActorItem {
    private final Long id;
    private final Long actorId;
    private final Long itemId;
    private final String characterName;
    private final Integer billingOrder;
}
