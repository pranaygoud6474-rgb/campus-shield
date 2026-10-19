package com.campussafety.controller;

import com.campussafety.dto.AlertRequest;
import com.campussafety.entity.Alert;
import com.campussafety.entity.User;
import com.campussafety.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<Alert>> getAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    @PostMapping("/alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Alert> createAlert(@AuthenticationPrincipal User user, @Valid @RequestBody AlertRequest request) {
        return ResponseEntity.ok(alertService.createAlert(user, request));
    }

    @PatchMapping("/alerts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Alert> updateAlert(@PathVariable Long id, @AuthenticationPrincipal User user,
                                             @Valid @RequestBody AlertRequest request) {
        return ResponseEntity.ok(alertService.updateAlert(id, user, request));
    }

    @DeleteMapping("/alerts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id, @AuthenticationPrincipal User user) {
        alertService.deleteAlert(id, user);
        return ResponseEntity.noContent().build();
    }
}
