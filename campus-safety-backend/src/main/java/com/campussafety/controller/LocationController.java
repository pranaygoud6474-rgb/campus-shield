package com.campussafety.controller;

import com.campussafety.entity.CampusLocation;
import com.campussafety.entity.User;
import com.campussafety.dto.EmergencyContactRequest;
import com.campussafety.entity.EmergencyContact;
import com.campussafety.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/locations")
    public ResponseEntity<List<CampusLocation>> listLocations() {
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    @PostMapping("/locations")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_OFFICER')")
    public ResponseEntity<CampusLocation> createLocation(@AuthenticationPrincipal User user,
                                                          @Valid @RequestBody CampusLocation location) {
        return ResponseEntity.ok(locationService.createLocation(location, user));
    }

    @GetMapping("/emergency-contacts")
    public ResponseEntity<List<?>> getContacts() {
        return ResponseEntity.ok(locationService.getEmergencyContacts());
    }

    @PostMapping("/emergency-contacts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmergencyContact> createContact(@AuthenticationPrincipal User user,
                                                           @Valid @RequestBody EmergencyContactRequest request) {
        return ResponseEntity.ok(locationService.createEmergencyContact(request, user));
    }
}
