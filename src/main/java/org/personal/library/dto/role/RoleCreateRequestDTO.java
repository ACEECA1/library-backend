package org.personal.library.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class RoleCreateRequestDTO {
    @NotBlank(message = "Role name is required")
    private String name;

    @NotEmpty(message = "At least one permission is required")
    private Set<String> permissions;
}
