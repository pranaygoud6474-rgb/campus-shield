package com.campussafety.dto;

import jakarta.validation.constraints.NotNull;

public record IncidentAssignmentRequest(@NotNull Long officerId) {
}
