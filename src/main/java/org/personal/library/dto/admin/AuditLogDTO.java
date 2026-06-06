package org.personal.library.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogDTO {
    private Long id;
    private org.personal.library.model.AuditLogAction action;
    private String details;
    private String username;
    private LocalDateTime createdAt;
}
