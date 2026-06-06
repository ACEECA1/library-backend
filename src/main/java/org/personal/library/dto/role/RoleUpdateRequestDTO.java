package org.personal.library.dto.role;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class RoleUpdateRequestDTO {

    private Set<String> permissions;

}
