package com.campussafety.controller;

import com.campussafety.dto.UserSummaryResponse;
import com.campussafety.dto.AuditLogResponse;
import com.campussafety.entity.User;
import com.campussafety.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> analytics() {
        return ResponseEntity.ok(adminService.getAnalytics());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryResponse>> users() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> auditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserSummaryResponse> updateRole(@PathVariable Long id,
                                                         @AuthenticationPrincipal User currentUser,
                                                         @RequestParam String role) {
        return ResponseEntity.ok(adminService.updateRole(id, currentUser, role));
    }
}
