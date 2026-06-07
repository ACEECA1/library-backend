package org.personal.library.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BanRequestDTO {
    @NotBlank(message = "Reason is required")
    private String reason;
}
