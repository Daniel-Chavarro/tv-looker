package org.tvl.tvlooker.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateListFavoriteRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "List name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 200)
    private String description;
}