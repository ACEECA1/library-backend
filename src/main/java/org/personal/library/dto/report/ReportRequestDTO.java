package org.personal.library.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.personal.library.model.Report;

@Data
public class ReportRequestDTO {

    @NotNull(message = "Target type is required")
    private Report.TargetType targetType;

    @NotNull(message = "Target ID is required")
    private Long targetId;

    @NotBlank(message = "Reason is required")
    private String reason;
}
