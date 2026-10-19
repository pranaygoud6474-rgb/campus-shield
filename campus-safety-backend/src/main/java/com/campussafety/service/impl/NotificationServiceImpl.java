package com.campussafety.service.impl;

import com.campussafety.dto.NotificationDto;
import com.campussafety.entity.Notification;
import com.campussafety.entity.User;
import com.campussafety.exception.ForbiddenException;
import com.campussafety.exception.ResourceNotFoundException;
import com.campussafety.repository.NotificationRepository;
import com.campussafety.service.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<NotificationDto> getNotificationsForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public NotificationDto markAsRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Not authorized to update this notification");
        }
        notification.setReadStatus(true);
        notification.setReadAt(LocalDateTime.now());
        return toDto(notificationRepository.save(notification));
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.isReadStatus(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
