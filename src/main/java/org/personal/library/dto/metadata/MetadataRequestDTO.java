package org.personal.library.dto.metadata;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MetadataRequestDTO {
    @NotBlank(message = "Name cannot be empty")
    private String name;
    
    private String description; // Used for Author biography or Series description
}
