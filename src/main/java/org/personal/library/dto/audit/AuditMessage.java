package org.personal.library.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditMessage implements Serializable {
    private org.personal.library.model.AuditLogAction action;
    private String details;
    private String username;
}
