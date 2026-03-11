package com.edurite.user.repository;

import com.edurite.user.entity.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This interface named RoleRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(String name);
}
