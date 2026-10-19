package com.campussafety.service.impl;

import com.campussafety.dto.AlertRequest;
import com.campussafety.entity.Alert;
import com.campussafety.entity.AuditLog;
import com.campussafety.entity.Notification;
import com.campussafety.entity.User;
import com.campussafety.exception.ResourceNotFoundException;
import com.campussafety.repository.AuditLogRepository;
import com.campussafety.repository.AlertRepository;
import com.campussafety.repository.NotificationRepository;
import com.campussafety.repository.UserRepository;
import com.campussafety.service.AlertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;

    public AlertServiceImpl(AlertRepository alertRepository, UserRepository userRepository,
                            NotificationRepository notificationRepository, AuditLogRepository auditLogRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public List<Alert> getActiveAlerts() {
        return alertRepository.findByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(LocalDateTime.now());
    }

    @Override
    @Transactional
    public Alert createAlert(User user, AlertRequest request) {
        Alert alert = new Alert();
        alert.setTitle(request.title());
        alert.setMessage(request.message());
        alert.setAlertType(request.alertType());
        alert.setSeverity(request.severity());
        alert.setAffectedArea(request.affectedArea());
        alert.setExpiresAt(request.expiresAt());
        alert.setActive(true);
        Alert saved = alertRepository.save(alert);
        audit(user, "ALERT_CREATED", saved);
        notificationRepository.saveAll(userRepository.findAll().stream()
                .filter(User::isEnabledStatus)
                .map(recipient -> notification(recipient, "Campus alert: " + saved.getTitle()))
                .toList());
        return saved;
    }

    @Override
    @Transactional
    public Alert updateAlert(Long id, User actor, AlertRequest request) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        alert.setTitle(request.title());
        alert.setMessage(request.message());
        alert.setAlertType(request.alertType());
        alert.setSeverity(request.severity());
        alert.setAffectedArea(request.affectedArea());
        alert.setExpiresAt(request.expiresAt());
        Alert saved = alertRepository.save(alert);
        audit(actor, "ALERT_UPDATED", saved);
        return saved;
    }

    @Override
    @Transactional
    public void deleteAlert(Long id, User actor) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        alert.setActive(false);
        alertRepository.save(alert);
        audit(actor, "ALERT_DEACTIVATED", alert);
    }

    private void audit(User actor, String action, Alert alert) {
        AuditLog audit = new AuditLog();
        audit.setActor(actor);
        audit.setAction(action);
        audit.setEntityType("Alert");
        audit.setEntityId(String.valueOf(alert.getId()));
        audit.setDetails("Alert management action");
        auditLogRepository.save(audit);
    }

    private Notification notification(User recipient, String message) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setMessage(message);
        notification.setType("ALERT");
        return notification;
    }
}
