package com.campussafety.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 30) String phone,
        @Size(max = 120) String department
) {
}
