package com.campussafety.dto;

import java.time.LocalDateTime;

public record AuthResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        String token,
        LocalDateTime createdAt
) {
}
