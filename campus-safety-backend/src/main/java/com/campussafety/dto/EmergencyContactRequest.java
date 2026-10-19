package com.campussafety.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmergencyContactRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String role,
        @NotBlank @Size(max = 30) String phone,
        @Email @Size(max = 254) String email,
        @NotNull @Min(0) Integer priority
) {
}
