package org.personal.library.dto.report;

import lombok.Builder;
import lombok.Data;
import org.personal.library.model.Report;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportResponseDTO {
    private Long id;
    private Report.TargetType targetType;
    private Long targetId;
    private String reason;
    private boolean isResolved;
    private String reporterUsername;
    private LocalDateTime createdAt;
}
