package com.onboardhub.auth.repository;

import com.onboardhub.auth.entity.User;
import com.onboardhub.auth.enums.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
    List<User> findAllByOrderByCreatedAtDesc();
    List<User> findByRoleOrderByCreatedAtDesc(Role role);
}
