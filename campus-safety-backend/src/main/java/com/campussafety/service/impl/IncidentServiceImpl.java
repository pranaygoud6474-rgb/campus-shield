package com.campussafety.service.impl;

import com.campussafety.dto.*;
import com.campussafety.entity.*;
import com.campussafety.exception.BadRequestException;
import com.campussafety.exception.ForbiddenException;
import com.campussafety.exception.ResourceNotFoundException;
import com.campussafety.repository.*;
import com.campussafety.service.IncidentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campussafety.dto.IncidentResponse;
import com.campussafety.dto.IncidentHistoryResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final IncidentHistoryRepository incidentHistoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;

    public IncidentServiceImpl(IncidentRepository incidentRepository, UserRepository userRepository,
                              IncidentHistoryRepository incidentHistoryRepository, AuditLogRepository auditLogRepository,
                              NotificationRepository notificationRepository) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.incidentHistoryRepository = incidentHistoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public IncidentResponse createIncident(User reporter, IncidentCreateRequest request) {
        if (request.description() == null || request.description().isBlank()) {
            throw new BadRequestException("Description is required");
        }

        Incident incident = new Incident();
        incident.setReporter(reporter);
        incident.setCategory(request.category());
        incident.setDescription(request.description());
        incident.setLocationName(request.locationName());
        incident.setCoordinates(request.coordinates());
        incident.setSeverity(request.severity());
        incident.setPriorityScore(priorityFor(request.severity()));
        incident.setStatus(IncidentStatus.NEW);

        Incident saved = incidentRepository.save(incident);
        recordHistory(saved, reporter, "CREATED", "Incident reported");
        List<User> responders = new ArrayList<>(userRepository.findByRoleAndEnabledTrue(Role.SECURITY_OFFICER));
        responders.addAll(userRepository.findByRoleAndEnabledTrue(Role.ADMIN));
        notificationRepository.saveAll(responders.stream()
                .map(responder -> notification(responder, "A new incident was reported: " + saved.getCategory().name()))
                .toList());
        return IncidentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> listIncidents(User currentUser, int page, int size) {
        return listIncidents(currentUser, page, size, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> listIncidents(User currentUser, int page, int size,
                                        IncidentStatus status, IncidentSeverity severity,
                                        com.campussafety.entity.IncidentCategory category) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page must be non-negative and size must be between 1 and 100");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Incident> specification = (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (currentUser.getRole() == Role.STUDENT) {
                predicates.add(builder.equal(root.get("reporter"), currentUser));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (severity != null) {
                predicates.add(builder.equal(root.get("severity"), severity));
            }
            if (category != null) {
                predicates.add(builder.equal(root.get("category"), category));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return incidentRepository.findAll(specification, pageable).map(IncidentResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> getMyIncidents(User currentUser, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page must be non-negative and size must be between 1 and 100");
        }
        Pageable pageable = PageRequest.of(page, size);
        return incidentRepository.findByReporter(currentUser, pageable).map(IncidentResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getIncident(Long id, User currentUser) {
        return IncidentResponse.from(findIncident(id, currentUser));
    }

    private Incident findIncident(Long id, User currentUser) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
        if (!incident.getReporter().getId().equals(currentUser.getId())
                && !currentUser.getRole().equals(Role.ADMIN)
                && !currentUser.getRole().equals(Role.SECURITY_OFFICER)) {
            throw new ForbiddenException("Not authorized to view this incident");
        }
        return incident;
    }

    @Override
    @Transactional
    public IncidentResponse updateStatus(Long id, User currentUser, IncidentStatusUpdateRequest request) {
        Incident incident = findIncident(id, currentUser);

        IncidentStatus previousStatus = incident.getStatus();
        if (!isForwardStatusTransition(previousStatus, request.status())) {
            throw new BadRequestException("Incident status cannot move backward or remain unchanged");
        }
        incident.setStatus(request.status());

        if (request.status() == IncidentStatus.ACKNOWLEDGED) {
            incident.setAcknowledgedAt(LocalDateTime.now());
        }
        if (request.status() == IncidentStatus.RESOLVED || request.status() == IncidentStatus.CLOSED) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        if (request.responseNotes() != null && !request.responseNotes().isBlank()) {
            incident.setResponseNotes(request.responseNotes());
        }
        if (request.resolutionDetails() != null && !request.resolutionDetails().isBlank()) {
            incident.setResolutionDetails(request.resolutionDetails());
        }

        Incident saved = incidentRepository.save(incident);
        recordHistory(saved, currentUser, "STATUS_UPDATED", "Status: " + previousStatus + " -> " + request.status());
        notificationRepository.save(notification(incident.getReporter(),
                "Your incident #" + incident.getId() + " status changed to " + request.status()));
        return IncidentResponse.from(saved);
    }

    private boolean isForwardStatusTransition(IncidentStatus current, IncidentStatus requested) {
        return switch (current) {
            case NEW -> requested != IncidentStatus.NEW;
            case ACKNOWLEDGED -> requested == IncidentStatus.RESPONDING
                    || requested == IncidentStatus.ON_SCENE
                    || requested == IncidentStatus.RESOLVED
                    || requested == IncidentStatus.CLOSED;
            case RESPONDING -> requested == IncidentStatus.ON_SCENE
                    || requested == IncidentStatus.RESOLVED
                    || requested == IncidentStatus.CLOSED;
            case ON_SCENE -> requested == IncidentStatus.RESOLVED || requested == IncidentStatus.CLOSED;
            case RESOLVED -> requested == IncidentStatus.CLOSED;
            case CLOSED -> false;
        };
    }

    @Override
    @Transactional
    public IncidentResponse assignIncident(Long id, User currentUser, IncidentAssignmentRequest request) {
        Incident incident = findIncident(id, currentUser);
        User officer = userRepository.findById(request.officerId())
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found"));
        if (!officer.getRole().equals(Role.SECURITY_OFFICER) && !officer.getRole().equals(Role.ADMIN)) {
            throw new BadRequestException("Target user is not an authorized officer");
        }

        incident.setAssignedOfficer(officer);
        Incident saved = incidentRepository.save(incident);
        recordHistory(saved, currentUser, "ASSIGNED", "Assigned to " + officer.getEmail());
        notificationRepository.save(notification(officer, "Incident #" + incident.getId() + " was assigned to you"));
        return IncidentResponse.from(saved);
    }

    @Override
    @Transactional
    public IncidentResponse addNote(Long id, User currentUser, IncidentNoteRequest request) {
        Incident incident = findIncident(id, currentUser);
        String notes = incident.getResponseNotes() == null ? request.note() : incident.getResponseNotes() + "\n" + request.note();
        incident.setResponseNotes(notes);
        Incident saved = incidentRepository.save(incident);
        recordHistory(saved, currentUser, "NOTE_ADDED", "Response note added");
        return IncidentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentHistoryResponse> getHistory(Long id, User currentUser) {
        Incident incident = findIncident(id, currentUser);
        return incidentHistoryRepository.findByIncidentOrderByCreatedAtAsc(incident).stream()
                .map(IncidentHistoryResponse::from).toList();
    }

    private void recordHistory(Incident incident, User actor, String action, String details) {
        incidentHistoryRepository.save(new IncidentHistory(incident, action, details));

        AuditLog auditLog = new AuditLog();
        auditLog.setActor(actor);
        auditLog.setAction(action);
        auditLog.setEntityType("Incident");
        auditLog.setEntityId(String.valueOf(incident.getId()));
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }

    private Notification notification(User recipient, String message) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setMessage(message);
        notification.setType("INCIDENT");
        return notification;
    }

    private int priorityFor(IncidentSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 95;
            case HIGH -> 80;
            case MEDIUM -> 55;
            case LOW -> 25;
        };
    }
}
