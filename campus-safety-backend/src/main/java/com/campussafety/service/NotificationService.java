package com.campussafety.service;

import com.campussafety.dto.NotificationDto;
import com.campussafety.entity.User;

import java.util.List;

public interface NotificationService {
    List<NotificationDto> getNotificationsForUser(User user);
    NotificationDto markAsRead(User user, Long notificationId);
}
