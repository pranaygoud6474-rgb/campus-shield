package com.campussafety.dto;

import java.time.LocalDateTime;

public record UserSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        String department,
        LocalDateTime createdAt
) {
    public static UserSummaryResponse from(com.campussafety.entity.User user) {
        return new UserSummaryResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole().name(), user.getDepartment(), user.getCreatedAt());
    }
}
