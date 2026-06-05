package org.personal.library.service.audit;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.AuditLogRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.model.AuditLog;
import org.personal.library.model.User;
import org.personal.library.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String details) {
        String username = SecurityUtils.getCurrentUsername();
        User user = null;
        if (username != null && !username.equals("anonymousUser")) {
            user = userRepository.findByUsername(username).orElse(null);
        }

        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setDetails(details);
        log.setUser(user);
        auditLogRepository.save(log);
    }
}
