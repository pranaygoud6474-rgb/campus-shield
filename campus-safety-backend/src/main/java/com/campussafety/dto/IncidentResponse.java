package com.campussafety.dto;

import com.campussafety.entity.Incident;

import java.time.LocalDateTime;

public record IncidentResponse(
        Long id,
        UserSummaryResponse reporter,
        UserSummaryResponse assignedOfficer,
        String category,
        String description,
        String locationName,
        String coordinates,
        String severity,
        Integer priorityScore,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime acknowledgedAt,
        LocalDateTime resolvedAt,
        String responseNotes,
        String resolutionDetails
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                UserSummaryResponse.from(incident.getReporter()),
                incident.getAssignedOfficer() == null ? null : UserSummaryResponse.from(incident.getAssignedOfficer()),
                incident.getCategory().name(),
                incident.getDescription(),
                incident.getLocationName(),
                incident.getCoordinates(),
                incident.getSeverity().name(),
                incident.getPriorityScore(),
                incident.getStatus().name(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getAcknowledgedAt(),
                incident.getResolvedAt(),
                incident.getResponseNotes(),
                incident.getResolutionDetails()
        );
    }
}
