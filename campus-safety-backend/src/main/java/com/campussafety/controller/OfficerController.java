package com.campussafety.controller;

import com.campussafety.dto.UserSummaryResponse;
import com.campussafety.entity.Role;
import com.campussafety.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/officers")
@PreAuthorize("hasAnyRole('SECURITY_OFFICER','ADMIN')")
public class OfficerController {

    private final UserRepository userRepository;

    public OfficerController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> listOfficers() {
        return ResponseEntity.ok(userRepository.findByRoleAndEnabledTrue(Role.SECURITY_OFFICER).stream()
                .map(user -> new UserSummaryResponse(
                        user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                        user.getRole().name(), user.getDepartment(), user.getCreatedAt()))
                .toList());
    }
}
