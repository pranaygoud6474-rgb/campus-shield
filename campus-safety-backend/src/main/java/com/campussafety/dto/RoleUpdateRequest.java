package com.campussafety.dto;

import com.campussafety.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull Role role) {
}
