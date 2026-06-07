package org.personal.library.dto.role;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class RoleCreateRequestDTO {
    @NotBlank(message = "Role name is required")
    private String name;

    private Set<String> permissions;
}
