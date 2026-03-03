package org.tvl.tvlooker.domain.model;

import java.time.LocalDateTime;

import org.tvl.tvlooker.domain.model.enums.InteractionType;

public class Interaction {
    private User user;
    private Item item;
    private InteractionType interactionType;
    private Review review;
    private LocalDateTime createdAt;
    
}