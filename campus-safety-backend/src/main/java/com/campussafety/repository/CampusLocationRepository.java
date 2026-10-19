package com.campussafety.repository;

import com.campussafety.entity.CampusLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampusLocationRepository extends JpaRepository<CampusLocation, Long> {
}
