package org.tvl.tvlooker.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for updating a user request.
 */
@Getter
@Setter
public class UpdateUserRequest {
    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String password;
}