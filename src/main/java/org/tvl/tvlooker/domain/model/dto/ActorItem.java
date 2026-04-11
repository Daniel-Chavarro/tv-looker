package org.tvl.tvlooker.domain.model.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ActorItem {
    private final Long id;
    private final Actor actor;
    private final Item item;
    private final String characterName;
    private final Integer billingOrder;
}
