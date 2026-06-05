package org.personal.library.service.notification;

import lombok.RequiredArgsConstructor;
import org.personal.library.dao.NotificationRepository;
import org.personal.library.dao.UserRepository;
import org.personal.library.dto.notification.NotificationResponseDTO;
import org.personal.library.model.Notification;
import org.personal.library.model.User;
import org.personal.library.util.AppException;
import org.personal.library.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public NotificationResponseDTO createForUser(User user, String message) {
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setUser(user);
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);
        NotificationResponseDTO response = mapToDTO(saved);
        messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications", response);
        return response;
    }

    @Transactional
    public void notifyAdmins(String message) {
        userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getName())))
                .forEach(admin -> createForUser(admin, message));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getCurrentUserNotifications() {
        User user = getCurrentUser();
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId) {
        User user = getCurrentUser();
        Notification notification = notificationRepository.findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new AppException("Notification not found", HttpStatus.NOT_FOUND));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
        return mapToDTO(notification);
    }

    @Transactional
    public void markAllAsRead() {
        User user = getCurrentUser();
        notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(notification -> !notification.isRead())
                .forEach(notification -> notification.setRead(true));
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new AppException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    private NotificationResponseDTO mapToDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
