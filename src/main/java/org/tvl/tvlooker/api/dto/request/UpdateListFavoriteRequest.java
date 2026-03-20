package org.tvl.tvlooker.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for updating a favorite list request.
 */
@Getter
@Setter
public class UpdateListFavoriteRequest {
    @Size(max = 100)
    private String name;

    @Size(max = 200)
    private String description;
}
