package com.campussafety.repository;

import com.campussafety.entity.Incident;
import com.campussafety.entity.IncidentSeverity;
import com.campussafety.entity.IncidentStatus;
import com.campussafety.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {
    Page<Incident> findByReporter(User reporter, Pageable pageable);
    Page<Incident> findByAssignedOfficer(User officer, Pageable pageable);
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findBySeverity(IncidentSeverity severity);
    List<Incident> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByStatus(IncidentStatus status);
}
