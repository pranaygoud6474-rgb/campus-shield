package com.campussafety.service.impl;

import com.campussafety.dto.UserSummaryResponse;
import com.campussafety.dto.AuditLogResponse;
import com.campussafety.entity.AuditLog;
import com.campussafety.entity.Incident;
import com.campussafety.entity.IncidentSeverity;
import com.campussafety.entity.IncidentStatus;
import com.campussafety.entity.Role;
import com.campussafety.entity.User;
import com.campussafety.exception.BadRequestException;
import com.campussafety.exception.ForbiddenException;
import com.campussafety.exception.ResourceNotFoundException;
import com.campussafety.repository.AuditLogRepository;
import com.campussafety.repository.IncidentRepository;
import com.campussafety.repository.UserRepository;
import com.campussafety.service.AdminService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.campussafety.entity.IncidentCategory;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminServiceImpl(UserRepository userRepository, IncidentRepository incidentRepository,
                            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public Map<String, Object> getDashboard() {
        Map<String, Object> map = new HashMap<>();
        map.put("totalIncidents", incidentRepository.count());
        map.put("activeEmergencies", List.of(IncidentStatus.NEW, IncidentStatus.ACKNOWLEDGED,
                        IncidentStatus.RESPONDING, IncidentStatus.ON_SCENE).stream()
                .mapToLong(incidentRepository::countByStatus).sum());
        map.put("resolvedIncidents", incidentRepository.countByStatus(IncidentStatus.RESOLVED)
                + incidentRepository.countByStatus(IncidentStatus.CLOSED));
        List<Long> responseTimes = incidentRepository.findAll().stream()
                .filter(incident -> incident.getAcknowledgedAt() != null)
                .map(incident -> Duration.between(incident.getCreatedAt(), incident.getAcknowledgedAt()).toMinutes())
                .filter(minutes -> minutes >= 0)
                .toList();
        map.put("averageResponseTimeMinutes", responseTimes.isEmpty() ? null
                : responseTimes.stream().mapToLong(Long::longValue).average().orElseThrow());
        map.put("responseTimeSampleSize", responseTimes.size());
        return map;
    }

    @Override
    public Map<String, Object> getAnalytics() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Long> categories = incidentRepository.findAll().stream()
                .collect(Collectors.groupingBy(incident -> incident.getCategory().name(), Collectors.counting()));
        LocalDateTime now = LocalDateTime.now();
        long recent = incidentRepository.findByCreatedAtBetween(now.minusDays(30), now).size();
        long previous = incidentRepository.findByCreatedAtBetween(now.minusDays(60), now.minusDays(30)).size();
        map.put("categories", categories);
        map.put("periodDays", 30);
        map.put("recentIncidentCount", recent);
        map.put("previousPeriodIncidentCount", previous);
        map.put("trend", recent > previous ? "increase" : recent < previous ? "decrease" : "unchanged");
        map.put("label", "Database-derived counts; descriptive comparison, not an AI prediction");
        return map;
    }

    @Override
    public List<UserSummaryResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserSummaryResponse(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getDepartment(),
                        user.getCreatedAt()))
                .toList();
    }

    @Override
    public UserSummaryResponse updateRole(Long userId, User currentUser, String roleName) {
        if (currentUser == null || !Role.ADMIN.equals(currentUser.getRole())) {
            throw new ForbiddenException("Only admins may update roles");
        }
        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException("Admins cannot change their own role");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Role role;
        try {
            role = Role.valueOf(roleName.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Role must be STUDENT, SECURITY_OFFICER, or ADMIN");
        }
        user.setRole(role);
        User saved = userRepository.save(user);
        AuditLog auditLog = new AuditLog();
        auditLog.setActor(currentUser);
        auditLog.setAction("USER_ROLE_UPDATED");
        auditLog.setEntityType("User");
        auditLog.setEntityId(String.valueOf(saved.getId()));
        auditLog.setDetails("Role changed to " + saved.getRole().name());
        auditLogRepository.save(auditLog);
        return new UserSummaryResponse(saved.getId(), saved.getFirstName(), saved.getLastName(),
                saved.getEmail(), saved.getRole().name(), saved.getDepartment(), saved.getCreatedAt());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AuditLogResponse::from).toList();
    }
}
