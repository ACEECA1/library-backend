package org.personal.library.service.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.library.config.RabbitMQConfig;
import org.personal.library.dto.audit.AuditMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditProducer {

    private final RabbitTemplate rabbitTemplate;

    public void logAction(String action, String details, String username) {
        try {
            AuditMessage message = new AuditMessage(action, details, username);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.AUDIT_ROUTING_KEY, message);
        } catch (Exception e) {
            log.error("Failed to send audit message", e);
        }
    }
}
