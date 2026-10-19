package com.campussafety.dto;

import com.campussafety.entity.AlertType;
import com.campussafety.entity.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AlertRequest(
        @NotBlank String title,
        @NotBlank String message,
        @NotNull AlertType alertType,
        @NotNull IncidentSeverity severity,
        @NotBlank String affectedArea,
        @NotNull LocalDateTime expiresAt
) {
}
