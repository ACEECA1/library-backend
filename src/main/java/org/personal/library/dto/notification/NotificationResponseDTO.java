package org.personal.library.dto.notification;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import org.personal.library.model.NotificationType;

@Value
@Builder
public class NotificationResponseDTO {
    Long id;
    String message;
    boolean isRead;
    NotificationType type;
    Long targetId;
    LocalDateTime createdAt;
}
