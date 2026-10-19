package com.campussafety.controller;

import com.campussafety.dto.*;
import com.campussafety.dto.IncidentResponse;
import com.campussafety.entity.IncidentCategory;
import com.campussafety.entity.IncidentSeverity;
import com.campussafety.entity.IncidentStatus;
import com.campussafety.entity.User;
import com.campussafety.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping("/incidents")
    @PreAuthorize("hasAnyRole('STUDENT','SECURITY_OFFICER','ADMIN')")
    public ResponseEntity<IncidentResponse> createIncident(@AuthenticationPrincipal User user,
                                                  @Valid @RequestBody IncidentCreateRequest request) {
        return ResponseEntity.ok(incidentService.createIncident(user, request));
    }

    @GetMapping("/incidents")
    @PreAuthorize("hasAnyRole('STUDENT','SECURITY_OFFICER','ADMIN')")
    public ResponseEntity<Page<IncidentResponse>> listIncidents(@AuthenticationPrincipal User user,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) IncidentStatus status,
                                                       @RequestParam(required = false) IncidentSeverity severity,
                                                       @RequestParam(required = false) IncidentCategory category) {
        return ResponseEntity.ok(incidentService.listIncidents(user, page, size, status, severity, category));
    }

    @GetMapping("/incidents/my")
    @PreAuthorize("hasAnyRole('STUDENT','SECURITY_OFFICER','ADMIN')")
    public ResponseEntity<Page<IncidentResponse>> myIncidents(@AuthenticationPrincipal User user,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(incidentService.getMyIncidents(user, page, size));
    }

    @GetMapping("/incidents/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','SECURITY_OFFICER','ADMIN')")
    public ResponseEntity<IncidentResponse> getIncident(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(incidentService.getIncident(id, user));
    }

    @PatchMapping("/incidents/{id}/status")
    @PreAuthorize("hasAnyRole('SECURITY_OFFICER','ADMIN')")
    public ResponseEntity<IncidentResponse> updateStatus(@PathVariable Long id,
                                                @AuthenticationPrincipal User user,
                                                @Valid @RequestBody IncidentStatusUpdateRequest request) {
        return ResponseEntity.ok(incidentService.updateStatus(id, user, request));
    }

    @PatchMapping("/incidents/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_OFFICER')")
    public ResponseEntity<IncidentResponse> assignIncident(@PathVariable Long id,
                                                  @AuthenticationPrincipal User user,
                                                  @Valid @RequestBody IncidentAssignmentRequest request) {
        return ResponseEntity.ok(incidentService.assignIncident(id, user, request));
    }

    @PostMapping("/incidents/{id}/notes")
    @PreAuthorize("hasAnyRole('SECURITY_OFFICER','ADMIN')")
    public ResponseEntity<IncidentResponse> addNote(@PathVariable Long id,
                                           @AuthenticationPrincipal User user,
                                           @Valid @RequestBody IncidentNoteRequest request) {
        return ResponseEntity.ok(incidentService.addNote(id, user, request));
    }

    @GetMapping("/incidents/{id}/history")
    @PreAuthorize("hasAnyRole('SECURITY_OFFICER','ADMIN','STUDENT')")
    public ResponseEntity<?> history(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(incidentService.getHistory(id, user));
    }
}
