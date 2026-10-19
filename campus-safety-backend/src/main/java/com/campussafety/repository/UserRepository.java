package com.campussafety.repository;

import com.campussafety.entity.User;
import com.campussafety.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRole(Role role);
    List<User> findByRoleAndEnabledTrue(Role role);
}
