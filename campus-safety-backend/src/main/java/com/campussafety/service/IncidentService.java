package com.campussafety.service;

import com.campussafety.dto.*;
import com.campussafety.entity.IncidentCategory;
import com.campussafety.entity.IncidentSeverity;
import com.campussafety.entity.IncidentStatus;
import com.campussafety.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import com.campussafety.dto.IncidentResponse;
import com.campussafety.dto.IncidentHistoryResponse;

public interface IncidentService {
    IncidentResponse createIncident(User reporter, IncidentCreateRequest request);
    Page<IncidentResponse> listIncidents(User currentUser, int page, int size);
    Page<IncidentResponse> listIncidents(User currentUser, int page, int size,
                                          IncidentStatus status, IncidentSeverity severity, IncidentCategory category);
    Page<IncidentResponse> getMyIncidents(User currentUser, int page, int size);
    IncidentResponse getIncident(Long id, User currentUser);
    IncidentResponse updateStatus(Long id, User currentUser, IncidentStatusUpdateRequest request);
    IncidentResponse assignIncident(Long id, User currentUser, IncidentAssignmentRequest request);
    IncidentResponse addNote(Long id, User currentUser, IncidentNoteRequest request);
    List<IncidentHistoryResponse> getHistory(Long id, User currentUser);
}
