package com.edurite.auth.config;

import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

// @Configuration marks a class that defines Spring beans and setup.
@Configuration
/**
 * This class named AuthDataSeeder is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AuthDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(AuthDataSeeder.class);

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean
    @Order(0)
    ApplicationRunner authSeedRunner(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${edurite.auth.seed.admin.email:admin@edurite.com}") String adminEmail,
            @Value("${edurite.auth.seed.admin.password:Admin@123}") String adminPassword,
            @Value("${edurite.auth.seed.admin.first-name:System}") String firstName,
            @Value("${edurite.auth.seed.admin.last-name:Admin}") String lastName
    ) {
        return args -> seed(roleRepository, userRepository, passwordEncoder, adminEmail, adminPassword, firstName, lastName);
    }

    @Transactional
    void seed(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String adminEmail,
            String adminPassword,
            String firstName,
            String lastName
    ) {
        String normalizedAdminEmail = adminEmail.toLowerCase(Locale.ROOT);
        List<String> roles = List.of("ROLE_STUDENT", "ROLE_COMPANY", "ROLE_ADMIN");
        for (String roleName : roles) {
            roleRepository.findByName(roleName).orElseGet(() -> {
                Role role = new Role();
                role.setName(roleName);
                log.info("[auth-seed] creating missing role={}", roleName);
                return roleRepository.save(role);
            });
        }

        userRepository.findByEmail(normalizedAdminEmail).ifPresentOrElse(existingUser -> {
            Set<String> existingRoles = existingUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
            log.info(
                    "[auth-seed] admin user already exists, skipping creation email={} rawPassword={} encodedPassword={} roles={} status={}",
                    existingUser.getEmail(),
                    adminPassword,
                    existingUser.getPasswordHash(),
                    existingRoles,
                    existingUser.getStatus()
            );
        }, () -> {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            String encodedPassword = passwordEncoder.encode(adminPassword);
            User user = new User();
            user.setEmail(normalizedAdminEmail);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPasswordHash(encodedPassword);
            user.setStatus(UserStatus.ACTIVE);
            user.getRoles().add(adminRole);
            User savedUser = userRepository.save(user);
            log.info(
                    "[auth-seed] created admin user email={} rawPassword={} encodedPassword={} roles={} status={}",
                    savedUser.getEmail(),
                    adminPassword,
                    encodedPassword,
                    savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                    savedUser.getStatus()
            );
        });
    }
}
