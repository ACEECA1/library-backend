package org.personal.library.service.audit;

import lombok.RequiredArgsConstructor;
import org.personal.library.util.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditProducer auditProducer;

    public void logAction(String action, String details) {
        String username = SecurityUtils.getCurrentUsername();
        if (username != null && !username.equals("anonymousUser")) {
            auditProducer.logAction(action, details, username);
        } else {
            auditProducer.logAction(action, details, "anonymousUser");
        }
    }
}
