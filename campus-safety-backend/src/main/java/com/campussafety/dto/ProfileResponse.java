package com.campussafety.dto;

import com.campussafety.entity.User;

import java.time.LocalDateTime;

public record ProfileResponse(Long id, String firstName, String lastName, String email, String role,
                              String phone, String department, LocalDateTime createdAt) {
    public static ProfileResponse from(User user) {
        return new ProfileResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getRole().name(), user.getPhone(), user.getDepartment(), user.getCreatedAt());
    }
}
