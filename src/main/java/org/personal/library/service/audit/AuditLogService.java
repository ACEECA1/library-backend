package org.personal.library.service.audit;

import lombok.RequiredArgsConstructor;
import org.personal.library.util.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditProducer auditProducer;

    /**
     * Records an audit log entry for a specific action performed by a user.
     * If the current user is authenticated, their username is attached to the log.
     * Otherwise, the action is logged under 'anonymousUser'.
     *
     * @param action a brief string representing the action type (e.g., "UPLOAD_BOOK", "APPROVE_USER")
     * @param details a detailed description of the action and its context
     */
    public void logAction(org.personal.library.model.AuditLogAction action, String details) {
        String username = SecurityUtils.getCurrentUsername();
        if (username != null && !username.equals("anonymousUser")) {
            auditProducer.logAction(action, details, username);
        } else {
            auditProducer.logAction(action, details, "anonymousUser");
        }
    }
}
