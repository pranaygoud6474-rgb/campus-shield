package com.campussafety.dto;

import com.campussafety.entity.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id, UserSummaryResponse actor, String action, String entityType,
                               String entityId, String details, LocalDateTime createdAt) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(),
                log.getActor() == null ? null : UserSummaryResponse.from(log.getActor()),
                log.getAction(), log.getEntityType(), log.getEntityId(), log.getDetails(), log.getCreatedAt());
    }
}
