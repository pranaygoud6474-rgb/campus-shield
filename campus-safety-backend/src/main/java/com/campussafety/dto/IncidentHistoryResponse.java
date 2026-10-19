package com.campussafety.dto;

import com.campussafety.entity.IncidentHistory;

import java.time.LocalDateTime;

public record IncidentHistoryResponse(Long id, String action, String details, LocalDateTime createdAt) {
    public static IncidentHistoryResponse from(IncidentHistory history) {
        return new IncidentHistoryResponse(history.getId(), history.getAction(), history.getDetails(),
                history.getCreatedAt());
    }
}
