package com.campussafety.dto;

import com.campussafety.entity.IncidentStatus;
import jakarta.validation.constraints.NotNull;

public record IncidentStatusUpdateRequest(
        @NotNull IncidentStatus status,
        String responseNotes,
        String resolutionDetails
) {
}
