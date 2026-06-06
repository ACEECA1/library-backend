package org.personal.library.dto.notification;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class NotificationResponseDTO {
    Long id;
    String message;
    boolean isRead;
    org.personal.library.model.NotificationType type;
    Long targetId;
    LocalDateTime createdAt;
}
