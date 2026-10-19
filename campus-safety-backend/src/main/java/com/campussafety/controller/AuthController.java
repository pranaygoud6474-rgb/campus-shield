package com.campussafety.controller;

import com.campussafety.dto.AuthRequest;
import com.campussafety.dto.AuthResponse;
import com.campussafety.dto.LoginRequest;
import com.campussafety.dto.ProfileResponse;
import com.campussafety.dto.ProfileUpdateRequest;
import jakarta.validation.Valid;
import com.campussafety.dto.UserSummaryResponse;
import com.campussafety.entity.User;
import com.campussafety.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.getProfile(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal User user,
                                                          @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(authService.updateProfile(user, request));
    }
}
