package com.campussafety.dto;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String message,
        String type,
        boolean readStatus,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
