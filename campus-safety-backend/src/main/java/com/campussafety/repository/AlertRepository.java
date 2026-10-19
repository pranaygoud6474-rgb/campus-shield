package com.campussafety.repository;

import com.campussafety.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(LocalDateTime now);
    List<Alert> findAllByOrderByCreatedAtDesc();
}
