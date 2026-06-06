package org.personal.library.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import org.personal.library.model.AuditLogAction;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditMessage implements Serializable {
    private AuditLogAction action;
    private String details;
    private String username;
}
