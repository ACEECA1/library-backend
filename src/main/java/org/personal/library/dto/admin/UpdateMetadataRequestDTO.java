package org.personal.library.dto.admin;

import lombok.Data;

@Data
public class UpdateMetadataRequestDTO {
    private String name;
    private String description; // Only used for Series
}
