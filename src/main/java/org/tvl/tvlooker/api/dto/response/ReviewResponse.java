package org.tvl.tvlooker.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.security.Timestamp;
import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private UUID userId;
    private Long itemId;
    private Integer rating;
    private String comment;
    private Timestamp timestamp;
}