package org.personal.library.service.badge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.library.config.RabbitMQConfig;
import org.personal.library.dto.badge.BadgeMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a badge evaluation event to the message broker.
     * This event triggers background processing to check if the user has earned a new badge.
     *
     * @param actionType the type of action performed by the user (e.g., "UPLOAD", "REVIEW", "UPVOTE")
     * @param userId the unique identifier of the user who performed the action
     */
    public void publishEvent(String actionType, Long userId) {
        try {
            BadgeMessage message = new BadgeMessage(actionType, userId);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.BADGE_ROUTING_KEY, message);
        } catch (Exception e) {
            log.error("Failed to send badge message", e);
        }
    }
}
