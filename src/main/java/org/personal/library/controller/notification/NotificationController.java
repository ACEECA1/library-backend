package org.personal.library.controller.notification;

import lombok.RequiredArgsConstructor;
import org.personal.library.dto.common.ApiResponse;
import org.personal.library.dto.notification.NotificationResponseDTO;
import org.personal.library.service.notification.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getNotifications() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getCurrentUserNotifications()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponseDTO>> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAsRead(id)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }
}
