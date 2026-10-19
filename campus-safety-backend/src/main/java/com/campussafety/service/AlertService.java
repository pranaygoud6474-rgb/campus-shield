package com.campussafety.service;

import com.campussafety.dto.AlertRequest;
import com.campussafety.entity.Alert;
import com.campussafety.entity.User;

import java.util.List;

public interface AlertService {
    List<Alert> getActiveAlerts();
    Alert createAlert(User user, AlertRequest request);
    Alert updateAlert(Long id, User actor, AlertRequest request);
    void deleteAlert(Long id, User actor);
}
