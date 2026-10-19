package com.campussafety.dto;

import jakarta.validation.constraints.NotBlank;

public record IncidentNoteRequest(@NotBlank String note) {
}
