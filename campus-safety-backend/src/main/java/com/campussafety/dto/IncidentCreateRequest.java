package com.campussafety.dto;

import com.campussafety.entity.IncidentCategory;
import com.campussafety.entity.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record IncidentCreateRequest(
        @NotNull IncidentCategory category,
        @NotBlank @Size(max = 5000) String description,
        @NotBlank @Size(max = 200) String locationName,
        String coordinates,
        @NotNull IncidentSeverity severity,
        @Min(0) @Max(100) Integer priorityScore
) {
}
