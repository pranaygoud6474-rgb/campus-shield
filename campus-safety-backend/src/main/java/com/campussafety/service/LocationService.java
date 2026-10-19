package com.campussafety.service;

import com.campussafety.entity.CampusLocation;
import com.campussafety.entity.EmergencyContact;
import com.campussafety.dto.EmergencyContactRequest;
import com.campussafety.entity.User;

import java.util.List;

public interface LocationService {
    List<CampusLocation> getAllLocations();
    CampusLocation createLocation(CampusLocation location, User actor);
    List<EmergencyContact> getEmergencyContacts();
    EmergencyContact createEmergencyContact(EmergencyContactRequest request, User actor);
}
