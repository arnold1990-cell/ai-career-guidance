package com.edurite.auth.config;

import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

// @Configuration marks a class that defines Spring beans and setup.
@Configuration
/**
 * This class named AuthDataSeeder is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AuthDataSeeder {

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean
    ApplicationRunner authSeedRunner(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${edurite.auth.seed.admin.email:admin@edurite.local}") String adminEmail,
            @Value("${edurite.auth.seed.admin.password:Admin@12345}") String adminPassword,
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
        List<String> roles = List.of("ROLE_STUDENT", "ROLE_COMPANY", "ROLE_ADMIN");
        for (String roleName : roles) {
            roleRepository.findByName(roleName).orElseGet(() -> {
                Role role = new Role();
                role.setName(roleName);
                return roleRepository.save(role);
            });
        }

        userRepository.findByEmail(adminEmail.toLowerCase()).orElseGet(() -> {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            User user = new User();
            user.setEmail(adminEmail.toLowerCase());
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPasswordHash(passwordEncoder.encode(adminPassword));
            user.setStatus(UserStatus.ACTIVE);
            user.getRoles().add(adminRole);
            return userRepository.save(user);
        });
    }
}
