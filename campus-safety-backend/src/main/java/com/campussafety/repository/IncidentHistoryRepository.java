package com.campussafety.repository;

import com.campussafety.entity.Incident;
import com.campussafety.entity.IncidentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {
    List<IncidentHistory> findByIncidentOrderByCreatedAtAsc(Incident incident);
}
