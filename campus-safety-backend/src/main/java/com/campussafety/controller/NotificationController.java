package com.campussafety.controller;

import com.campussafety.dto.NotificationDto;
import com.campussafety.entity.User;
import com.campussafety.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('STUDENT','SECURITY_OFFICER','ADMIN')")
    public ResponseEntity<List<NotificationDto>> getNotifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user));
    }

    @PatchMapping("/notifications/{id}/read")
    @PreAuthorize("hasAnyRole('STUDENT','SECURITY_OFFICER','ADMIN')")
    public ResponseEntity<NotificationDto> markRead(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(user, id));
    }
}
