package com.campussafety.service;

import com.campussafety.dto.AuthRequest;
import com.campussafety.dto.AuthResponse;
import com.campussafety.dto.LoginRequest;
import com.campussafety.dto.ProfileResponse;
import com.campussafety.dto.ProfileUpdateRequest;
import com.campussafety.entity.Role;
import com.campussafety.entity.User;
import com.campussafety.exception.BadRequestException;
import com.campussafety.repository.UserRepository;
import com.campussafety.repository.AuditLogRepository;
import com.campussafety.entity.AuditLog;
import com.campussafety.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogRepository auditLogRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email already exists");
        }
        if (request.role() != null && request.role() != Role.STUDENT) {
            throw new BadRequestException("Public registration is restricted to student/staff accounts");
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);
        return toAuthResponse(saved, token);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(java.util.Locale.ROOT),
                        request.password())
        );

        User user = (User) authentication.getPrincipal();
        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token);
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    public ProfileResponse getProfile(User user) {
        return ProfileResponse.from(user);
    }

    @Transactional
    public ProfileResponse updateProfile(User currentUser, ProfileUpdateRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setPhone(normalizeOptional(request.phone()));
        user.setDepartment(normalizeOptional(request.department()));
        User saved = userRepository.save(user);
        AuditLog audit = new AuditLog();
        audit.setActor(saved);
        audit.setAction("PROFILE_UPDATED");
        audit.setEntityType("User");
        audit.setEntityId(String.valueOf(saved.getId()));
        audit.setDetails("Profile contact details updated");
        auditLogRepository.save(audit);
        return ProfileResponse.from(saved);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                token,
                user.getCreatedAt()
        );
    }
}
