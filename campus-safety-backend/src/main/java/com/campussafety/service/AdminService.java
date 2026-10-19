package com.campussafety.service;

import com.campussafety.dto.UserSummaryResponse;
import com.campussafety.dto.AuditLogResponse;
import com.campussafety.entity.User;

import java.util.List;
import java.util.Map;

public interface AdminService {
    Map<String, Object> getDashboard();
    Map<String, Object> getAnalytics();
    List<UserSummaryResponse> getUsers();
    UserSummaryResponse updateRole(Long userId, User currentUser, String roleName);
    List<AuditLogResponse> getAuditLogs();
}
