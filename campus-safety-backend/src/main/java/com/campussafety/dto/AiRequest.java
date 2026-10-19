package com.campussafety.dto;

import jakarta.validation.constraints.NotBlank;

public record AiRequest(@NotBlank String description) {
}
