package com.campussafety.dto;

import com.campussafety.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        Role role
) {
}
