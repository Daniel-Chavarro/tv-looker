package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class ActorItemResponse {
    private Long id;
    private Long actorId;
    private String actorName;
    private String characterName;
    private Integer billingOrder;
}
