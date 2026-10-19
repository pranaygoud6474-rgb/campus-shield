package com.campussafety.service.impl;

import com.campussafety.entity.CampusLocation;
import com.campussafety.entity.EmergencyContact;
import com.campussafety.entity.AuditLog;
import com.campussafety.entity.User;
import com.campussafety.dto.EmergencyContactRequest;
import com.campussafety.repository.AuditLogRepository;
import com.campussafety.repository.CampusLocationRepository;
import com.campussafety.repository.EmergencyContactRepository;
import com.campussafety.service.LocationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationServiceImpl implements LocationService {

    private final CampusLocationRepository campusLocationRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final AuditLogRepository auditLogRepository;

    public LocationServiceImpl(CampusLocationRepository campusLocationRepository,
                              EmergencyContactRepository emergencyContactRepository,
                              AuditLogRepository auditLogRepository) {
        this.campusLocationRepository = campusLocationRepository;
        this.emergencyContactRepository = emergencyContactRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public List<CampusLocation> getAllLocations() {
        return campusLocationRepository.findAll();
    }

    @Override
    public CampusLocation createLocation(CampusLocation location, User actor) {
        CampusLocation saved = campusLocationRepository.save(location);
        audit(actor, "LOCATION_CREATED", "CampusLocation", saved.getId(), "Campus location created");
        return saved;
    }

    @Override
    public List<EmergencyContact> getEmergencyContacts() {
        return emergencyContactRepository.findAllByOrderByPriorityAsc();
    }

    @Override
    public EmergencyContact createEmergencyContact(EmergencyContactRequest request, User actor) {
        EmergencyContact contact = new EmergencyContact();
        contact.setName(request.name().trim());
        contact.setRole(request.role().trim());
        contact.setPhone(request.phone().trim());
        contact.setEmail(request.email() == null ? null : request.email().trim());
        contact.setPriority(request.priority());
        EmergencyContact saved = emergencyContactRepository.save(contact);
        audit(actor, "EMERGENCY_CONTACT_CREATED", "EmergencyContact", saved.getId(),
                "Emergency contact created");
        return saved;
    }

    private void audit(User actor, String action, String entityType, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(String.valueOf(entityId));
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }
}
