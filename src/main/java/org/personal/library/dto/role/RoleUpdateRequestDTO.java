package org.personal.library.dto.role;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class RoleUpdateRequestDTO {

    @NotEmpty(message = "Permissions set cannot be empty")
    private Set<String> permissions;

}
