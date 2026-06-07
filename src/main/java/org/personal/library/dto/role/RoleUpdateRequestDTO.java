package org.personal.library.dto.role;

import lombok.Data;

import java.util.Set;

@Data
public class RoleUpdateRequestDTO {

    private Set<String> permissions;

}
