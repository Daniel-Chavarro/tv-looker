package org.tvl.tvlooker.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReviewRequest {
    @Min(0)
    @Max(5)
    private Integer score;

    @Size(max = 200, message = "Review text cannot exceed 200 characters")
    private String reviewText;
}
