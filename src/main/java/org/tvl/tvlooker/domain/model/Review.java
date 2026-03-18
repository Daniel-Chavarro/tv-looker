package org.tvl.tvlooker.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class Review {
    private final Long id;
    private final UUID userId;
    private final Long itemId;
    private final Integer score;
    @Builder.Default
    private final String reviewText = "";
    private final Timestamp reviewDate;
}
